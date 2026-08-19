package com.orderflow.payment;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.orderflow.common.BizException;
import com.orderflow.config.MockPaymentProperties;
import com.orderflow.domain.entity.OrderStatusHistory;
import com.orderflow.domain.entity.Orders;
import com.orderflow.domain.entity.PaymentTransaction;
import com.orderflow.domain.mapper.OrderStatusHistoryMapper;
import com.orderflow.domain.mapper.OrdersMapper;
import com.orderflow.domain.mapper.PaymentTransactionMapper;
import com.orderflow.order.OrderStatus;
import com.orderflow.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 项目内模拟扫码付款：不请求支付宝或任何真实支付渠道。
 * 电脑端只展示二维码；扫码后的手机页面持有一次性付款码，再由服务端条件更新订单状态。
 */
@Service
public class MockPaymentService implements PaymentService {

    private static final String PROVIDER = "MOCK";
    private static final int EXPIRE_MINUTES = 30;

    private final MockPaymentProperties properties;
    private final OrdersMapper ordersMapper;
    private final PaymentTransactionMapper paymentMapper;
    private final OrderStatusHistoryMapper historyMapper;

    public MockPaymentService(MockPaymentProperties properties, OrdersMapper ordersMapper,
                              PaymentTransactionMapper paymentMapper, OrderStatusHistoryMapper historyMapper) {
        this.properties = properties;
        this.ordersMapper = ordersMapper;
        this.paymentMapper = paymentMapper;
        this.historyMapper = historyMapper;
    }

    @Override
    @Transactional
    public MockCheckoutDTO createMockCheckout(Long orderId, Long customerId) {
        boolean previousIgnore = TenantContext.isIgnoreTenant();
        try {
            TenantContext.setIgnoreTenant(true);
            Orders order = loadPayableOrder(orderId, customerId);
            // 已下线的历史渠道不再允许继续付款，防止同一订单出现两个有效付款码。
            paymentMapper.closeLegacyPendingByOrderId(orderId);
            PaymentTransaction transaction = paymentMapper.findMockByOrderId(orderId);
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(EXPIRE_MINUTES);
            String paymentToken = newPaymentToken();
            String checkoutUrl = checkoutUrl(paymentToken);
            if (transaction == null) {
                transaction = new PaymentTransaction();
                transaction.setTenantId(order.getTenantId());
                transaction.setOrderId(orderId);
                transaction.setOutTradeNo(newPaymentNo(order.getOrderNo()));
                transaction.setProvider(PROVIDER);
                transaction.setAmountCent(order.getTotalAmountCent());
                transaction.setStatus("PENDING");
                transaction.setPaymentToken(paymentToken);
                transaction.setExpiresAt(expiresAt);
                transaction.setQrCode(checkoutUrl);
                paymentMapper.insert(transaction);
            } else {
                if (!"PENDING".equals(transaction.getStatus())) {
                    throw new BizException(40912, "该订单付款码已关闭或已完成，请刷新订单状态");
                }
                paymentMapper.refreshMockCheckout(transaction.getId(), paymentToken, expiresAt, checkoutUrl);
                transaction.setPaymentToken(paymentToken);
                transaction.setExpiresAt(expiresAt);
                transaction.setQrCode(checkoutUrl);
            }
            MockCheckoutDTO result = new MockCheckoutDTO();
            result.setPaymentNo(transaction.getOutTradeNo());
            result.setAmountCent(transaction.getAmountCent());
            result.setQrCodeImage(toQrCodeImage(checkoutUrl));
            return result;
        } finally {
            TenantContext.setIgnoreTenant(previousIgnore);
        }
    }

    @Override
    public PaymentStatusDTO getPaymentStatus(Long orderId, Long customerId) {
        boolean previousIgnore = TenantContext.isIgnoreTenant();
        try {
            TenantContext.setIgnoreTenant(true);
            Orders order = ordersMapper.selectById(orderId);
            if (order == null) throw new BizException(40403, "订单不存在");
            if (!Objects.equals(customerId, order.getCustomerId())) throw new BizException(40303, "无权查看该订单支付状态");
            PaymentStatusDTO result = new PaymentStatusDTO();
            result.setOrderStatus(order.getStatus());
            result.setPaid(OrderStatus.PAID.name().equals(order.getStatus()));
            return result;
        } finally {
            TenantContext.setIgnoreTenant(previousIgnore);
        }
    }

    @Override
    public MockPaymentPageDTO getMockCheckoutPage(String paymentToken) {
        boolean previousIgnore = TenantContext.isIgnoreTenant();
        try {
            TenantContext.setIgnoreTenant(true);
            return toPageDto(requireTransaction(paymentToken));
        } finally {
            TenantContext.setIgnoreTenant(previousIgnore);
        }
    }

    @Override
    @Transactional
    public MockPaymentPageDTO confirmMockPayment(String paymentToken) {
        boolean previousIgnore = TenantContext.isIgnoreTenant();
        try {
            // 手机模拟收银台不带登录态；付款码已作为能力凭证，内部读取必须显式跨租户。
            TenantContext.setIgnoreTenant(true);
            PaymentTransaction transaction = requireTransaction(paymentToken);
            if ("SUCCESS".equals(transaction.getStatus())) return toPageDto(transaction);
            if (!"PENDING".equals(transaction.getStatus())) throw new BizException(40913, "该付款码已失效，请返回商城重新发起付款");
            if (transaction.getExpiresAt() == null || !transaction.getExpiresAt().isAfter(LocalDateTime.now())) {
                paymentMapper.markClosed(transaction.getId());
                throw new BizException(40913, "付款码已过期，请返回商城重新发起付款");
            }

            Orders order = ordersMapper.selectById(transaction.getOrderId());
            if (order == null || !OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
                throw new BizException(40911, "当前订单不处于待付款状态");
            }
            if (ordersMapper.markPaid(order.getId(), order.getCustomerId()) != 1) {
                Orders latest = ordersMapper.selectById(order.getId());
                if (latest == null || !OrderStatus.PAID.name().equals(latest.getStatus())) {
                    throw new BizException(40911, "订单状态已变化，请返回商城刷新");
                }
            } else {
                OrderStatusHistory history = new OrderStatusHistory();
                history.setTenantId(order.getTenantId());
                history.setOrderId(order.getId());
                history.setFromStatus(OrderStatus.PENDING_PAYMENT.name());
                history.setToStatus(OrderStatus.PAID.name());
                history.setRemark("模拟扫码付款成功，流水号=" + transaction.getOutTradeNo());
                historyMapper.insert(history);
            }
            paymentMapper.markSuccess(transaction.getId(), "MOCK-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
                    "{\"channel\":\"mock_qr\",\"confirmedAt\":\"" + LocalDateTime.now() + "\"}");
            return toPageDto(requireTransaction(paymentToken));
        } finally {
            TenantContext.setIgnoreTenant(previousIgnore);
        }
    }

    @Override
    @Transactional
    @InterceptorIgnore(tenantLine = "true")
    public void closePendingPayments(Long orderId) {
        paymentMapper.closePendingByOrderId(orderId);
    }

    private PaymentTransaction requireTransaction(String paymentToken) {
        if (paymentToken == null || paymentToken.isBlank()) throw new BizException(40001, "付款码不能为空");
        PaymentTransaction transaction = paymentMapper.findMockByToken(paymentToken);
        if (transaction == null) throw new BizException(40404, "付款码不存在或已更新，请返回商城重新扫码");
        return transaction;
    }

    private Orders loadPayableOrder(Long orderId, Long customerId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) throw new BizException(40403, "订单不存在");
        if (!Objects.equals(customerId, order.getCustomerId())) throw new BizException(40303, "无权支付该订单");
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            throw new BizException(40911, "当前订单不处于待付款状态");
        }
        return order;
    }

    private MockPaymentPageDTO toPageDto(PaymentTransaction transaction) {
        Orders order = ordersMapper.selectById(transaction.getOrderId());
        if (order == null) throw new BizException(40403, "订单不存在");
        MockPaymentPageDTO result = new MockPaymentPageDTO();
        result.setPaymentNo(transaction.getOutTradeNo());
        result.setOrderNo(order.getOrderNo());
        result.setAmountCent(transaction.getAmountCent());
        result.setStatus(transaction.getStatus());
        result.setPaid("SUCCESS".equals(transaction.getStatus()) || OrderStatus.PAID.name().equals(order.getStatus()));
        result.setExpiresAt(transaction.getExpiresAt());
        return result;
    }

    private String checkoutUrl(String paymentToken) {
        String baseUrl = properties.getPublicBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) throw new BizException(50301, "模拟付款公网地址尚未配置");
        return baseUrl.replaceAll("/+$", "") + "/mock-pay?token="
                + URLEncoder.encode(paymentToken, StandardCharsets.UTF_8);
    }

    private String newPaymentToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private String newPaymentNo(String orderNo) {
        return "MOCK" + orderNo.substring(Math.max(0, orderNo.length() - 16))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String toQrCodeImage(String content) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Map<EncodeHintType, Object> hints = Map.of(EncodeHintType.CHARACTER_SET, "UTF-8", EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 320, 320, hints);
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception ex) {
            throw new BizException(50001, "生成模拟付款二维码失败");
        }
    }
}
