package com.orderflow.payment;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.orderflow.common.BizException;
import com.orderflow.config.AlipayProperties;
import com.orderflow.domain.entity.OrderStatusHistory;
import com.orderflow.domain.entity.Orders;
import com.orderflow.domain.entity.PaymentTransaction;
import com.orderflow.domain.mapper.OrderStatusHistoryMapper;
import com.orderflow.domain.mapper.OrdersMapper;
import com.orderflow.domain.mapper.PaymentTransactionMapper;
import com.orderflow.order.OrderStatus;
import com.orderflow.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 支付宝沙箱“当面付”二维码支付。
 *
 * 浏览器只展示二维码；付款结果只能由支付宝异步通知验签后写入订单，避免前端伪造“支付成功”。
 */
@Service
public class AlipayPaymentService implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(AlipayPaymentService.class);
    private static final String PROVIDER = "ALIPAY";
    private static final DateTimeFormatter ALIPAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();

    private final AlipayProperties properties;
    private final OrdersMapper ordersMapper;
    private final PaymentTransactionMapper paymentMapper;
    private final OrderStatusHistoryMapper historyMapper;
    private final ObjectMapper objectMapper;

    public AlipayPaymentService(AlipayProperties properties, OrdersMapper ordersMapper,
                                PaymentTransactionMapper paymentMapper,
                                OrderStatusHistoryMapper historyMapper, ObjectMapper objectMapper) {
        this.properties = properties;
        this.ordersMapper = ordersMapper;
        this.paymentMapper = paymentMapper;
        this.historyMapper = historyMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public AlipayCheckoutDTO createAlipayCheckout(Long orderId, Long customerId) {
        requireReady();
        boolean previousIgnore = TenantContext.isIgnoreTenant();
        try {
            TenantContext.setIgnoreTenant(true);
            Orders order = loadPayableOrder(orderId, customerId);
            PaymentTransaction transaction = paymentMapper.findAlipayByOrderId(orderId);
            if (transaction == null) {
                transaction = new PaymentTransaction();
                transaction.setTenantId(order.getTenantId());
                transaction.setOrderId(orderId);
                transaction.setOutTradeNo(newOutTradeNo(order.getOrderNo()));
                transaction.setProvider(PROVIDER);
                transaction.setAmountCent(order.getTotalAmountCent());
                transaction.setStatus("PENDING");
                paymentMapper.insert(transaction);
            }
            if (!"PENDING".equals(transaction.getStatus())) {
                throw new BizException(40912, "该订单支付流水已关闭或已完成，请刷新订单状态");
            }

            String qrCode = transaction.getQrCode();
            if (qrCode == null || qrCode.isBlank()) {
                qrCode = precreateQrCode(order, transaction);
                paymentMapper.updateQrCode(transaction.getId(), qrCode);
            }
            AlipayCheckoutDTO result = new AlipayCheckoutDTO();
            result.setPaymentNo(transaction.getOutTradeNo());
            result.setAmountCent(transaction.getAmountCent());
            result.setQrCodeImage(toQrCodeImage(qrCode));
            return result;
        } finally {
            TenantContext.setIgnoreTenant(previousIgnore);
        }
    }

    @Override
    public AlipayPaymentStatusDTO getAlipayPaymentStatus(Long orderId, Long customerId) {
        boolean previousIgnore = TenantContext.isIgnoreTenant();
        try {
            // 顾客订单可能属于任意商家租户；仅凭 customerId 做归属校验后跨租户读取状态。
            TenantContext.setIgnoreTenant(true);
            Orders order = ordersMapper.selectById(orderId);
            if (order == null) throw new BizException(40403, "订单不存在");
            if (!Objects.equals(customerId, order.getCustomerId())) throw new BizException(40303, "无权查看该订单支付状态");
            AlipayPaymentStatusDTO result = new AlipayPaymentStatusDTO();
            result.setOrderStatus(order.getStatus());
            result.setPaid(OrderStatus.PAID.name().equals(order.getStatus()));
            return result;
        } finally {
            TenantContext.setIgnoreTenant(previousIgnore);
        }
    }

    @Override
    @Transactional
    @InterceptorIgnore(tenantLine = "true")
    public boolean handleAlipayNotify(Map<String, String> parameters) {
        if (!properties.isReady() || !verifySignature(parameters)
                || !Objects.equals(properties.getAppId(), parameters.get("app_id"))) {
            log.warn("支付宝异步通知校验失败 outTradeNo={}", parameters.get("out_trade_no"));
            return false;
        }
        String tradeStatus = parameters.get("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) return true;

        PaymentTransaction transaction = paymentMapper.findAlipayByOutTradeNo(parameters.get("out_trade_no"));
        if (transaction == null || !"PENDING".equals(transaction.getStatus())) {
            return transaction != null && "SUCCESS".equals(transaction.getStatus());
        }
        if (!sameAmount(transaction.getAmountCent(), parameters.get("total_amount"))) {
            log.error("支付宝回调金额不一致 outTradeNo={}", parameters.get("out_trade_no"));
            return false;
        }
        return settleSuccessfulPayment(transaction, parameters.get("trade_no"), toJson(parameters));
    }

    @Override
    @Transactional
    public void closePendingAlipayPayment(Orders order) {
        if (!properties.isReady()) return;
        PaymentTransaction transaction = paymentMapper.findAlipayByOrderId(order.getId());
        if (transaction == null || !"PENDING".equals(transaction.getStatus())) return;
        try {
            AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
            request.setBizContent(objectMapper.writeValueAsString(Map.of("out_trade_no", transaction.getOutTradeNo())));
            AlipayTradeCloseResponse response = alipayClient().execute(request);
            if (response.isSuccess()) {
                paymentMapper.markClosed(transaction.getId());
            }
        } catch (Exception ex) {
            log.error("支付宝超时关单调用异常 orderNo={}", order.getOrderNo(), ex);
        }
    }

    @Override
    @Transactional
    public void closePendingAlipayPayment(Long orderId) {
        boolean previousIgnore = TenantContext.isIgnoreTenant();
        try {
            TenantContext.setIgnoreTenant(true);
            Orders order = ordersMapper.selectById(orderId);
            if (order != null) {
                closePendingAlipayPayment(order);
            }
        } finally {
            TenantContext.setIgnoreTenant(previousIgnore);
        }
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

    private String precreateQrCode(Orders order, PaymentTransaction transaction) {
        try {
            Map<String, Object> bizContent = new LinkedHashMap<>();
            bizContent.put("out_trade_no", transaction.getOutTradeNo());
            bizContent.put("total_amount", yuan(transaction.getAmountCent()));
            bizContent.put("subject", "OrderFlow 订单 " + order.getOrderNo());
            bizContent.put("timeout_express", "30m");
            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            request.setNotifyUrl(properties.getNotifyUrl());
            request.setBizContent(objectMapper.writeValueAsString(bizContent));
            AlipayTradePrecreateResponse response = alipayClient().execute(request);
            String qrCode = response.getQrCode();
            if (!response.isSuccess() || qrCode == null || qrCode.isBlank()) {
                String message = response.getSubMsg() == null ? response.getMsg() : response.getSubMsg();
                log.warn("支付宝沙箱预创建交易失败 orderNo={} code={} message={}", order.getOrderNo(),
                        response.getCode(), message);
                throw new BizException(50201, "支付宝沙箱生成二维码失败：" + message);
            }
            return qrCode;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("支付宝沙箱预创建交易异常 orderNo={}", order.getOrderNo(), ex);
            throw new BizException(50201, "支付宝沙箱生成二维码失败，请检查本地私钥和沙箱配置");
        }
    }

    /** SDK 会将 UTF-8 编码、sign_type 与签名规则一起处理，不能再由页面手工拼接。 */
    private AlipayClient alipayClient() {
        return new DefaultAlipayClient(properties.getGatewayUrl(), properties.getAppId(), properties.getAppPrivateKey(),
                "json", "UTF-8", properties.getAlipayPublicKey(), "RSA2");
    }

    private boolean settleSuccessfulPayment(PaymentTransaction transaction, String tradeNo, String payload) {
        Orders order = ordersMapper.selectById(transaction.getOrderId());
        if (order == null || !OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) return false;
        if (ordersMapper.markPaid(order.getId(), order.getCustomerId()) != 1) {
            Orders latest = ordersMapper.selectById(order.getId());
            return latest != null && OrderStatus.PAID.name().equals(latest.getStatus());
        }
        OrderStatusHistory history = new OrderStatusHistory();
        history.setTenantId(order.getTenantId());
        history.setOrderId(order.getId());
        history.setFromStatus(OrderStatus.PENDING_PAYMENT.name());
        history.setToStatus(OrderStatus.PAID.name());
        history.setRemark("支付宝沙箱扫码支付成功，交易号=" + tradeNo);
        historyMapper.insert(history);
        paymentMapper.markSuccess(transaction.getId(), tradeNo, payload);
        return true;
    }

    private JsonNode postGateway(String method, String bizContent) throws Exception {
        Map<String, String> parameters = signedParameters(method, bizContent);
        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (!body.isEmpty()) body.append('&');
            body.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            body.append('=').append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getGatewayUrl()))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) throw new IllegalStateException("支付宝网关响应=" + response.statusCode());
        return objectMapper.readTree(response.body());
    }

    private Map<String, String> signedParameters(String method, String bizContent) {
        Map<String, String> parameters = new TreeMap<>();
        parameters.put("app_id", properties.getAppId());
        parameters.put("method", method);
        parameters.put("format", "JSON");
        parameters.put("charset", "utf-8");
        parameters.put("sign_type", "RSA2");
        parameters.put("timestamp", LocalDateTime.now().format(ALIPAY_TIME));
        parameters.put("version", "1.0");
        parameters.put("notify_url", properties.getNotifyUrl());
        parameters.put("biz_content", bizContent);
        parameters.put("sign", sign(parameters));
        return parameters;
    }

    private String sign(Map<String, String> parameters) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey());
            signature.update(requestSignContent(parameters).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception ex) {
            throw new BizException(50302, "支付宝私钥格式无效，请使用沙箱应用私钥");
        }
    }

    private boolean verifySignature(Map<String, String> parameters) {
        try {
            String sign = parameters.get("sign");
            if (sign == null || sign.isBlank()) return false;
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey());
            verifier.update(callbackSignContent(parameters).getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(sign));
        } catch (Exception ex) {
            log.warn("支付宝异步通知验签异常", ex);
            return false;
        }
    }

    private String toQrCodeImage(String content) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Map<EncodeHintType, Object> hints = Map.of(EncodeHintType.CHARACTER_SET, "UTF-8", EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 320, 320, hints);
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception ex) {
            throw new BizException(50001, "生成付款二维码失败");
        }
    }

    private String requestSignContent(Map<String, String> parameters) {
        Map<String, String> sorted = new TreeMap<>(parameters);
        sorted.remove("sign");
        return joinSignContent(sorted);
    }

    private String callbackSignContent(Map<String, String> parameters) {
        Map<String, String> sorted = new TreeMap<>(parameters);
        sorted.remove("sign");
        sorted.remove("sign_type");
        return joinSignContent(sorted);
    }

    private String joinSignContent(Map<String, String> sorted) {
        StringBuilder content = new StringBuilder();
        sorted.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                if (!content.isEmpty()) content.append('&');
                content.append(key).append('=').append(value);
            }
        });
        return content.toString();
    }

    private PrivateKey privateKey() throws Exception {
        byte[] bytes = Base64.getDecoder().decode(normalizeKey(properties.getAppPrivateKey()));
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    private PublicKey publicKey() throws Exception {
        byte[] bytes = Base64.getDecoder().decode(normalizeKey(properties.getAlipayPublicKey()));
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
    }

    private String normalizeKey(String key) {
        return key.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
    }

    private void requireReady() {
        if (!properties.isReady()) throw new BizException(50301, "支付宝沙箱尚未配置完成，请先填写本地私钥和回调地址");
    }

    private String newOutTradeNo(String orderNo) {
        return "OF" + orderNo.substring(Math.max(0, orderNo.length() - 18))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private boolean sameAmount(long amountCent, String amountYuan) {
        try { return BigDecimal.valueOf(amountCent, 2).compareTo(new BigDecimal(amountYuan)) == 0; }
        catch (NumberFormatException ex) { return false; }
    }

    private String yuan(long amountCent) {
        return BigDecimal.valueOf(amountCent, 2).setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    private String toJson(Map<String, String> parameters) {
        try { return objectMapper.writeValueAsString(parameters); }
        catch (JsonProcessingException ex) { return "{\"serialization\":\"failed\"}"; }
    }
}
