package com.orderflow.domain.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orderflow.domain.entity.PaymentTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PaymentTransactionMapper extends BaseMapper<PaymentTransaction> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM payment_transaction WHERE order_id = #{orderId} AND provider = 'MOCK' LIMIT 1")
    PaymentTransaction findMockByOrderId(@Param("orderId") Long orderId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM payment_transaction WHERE payment_token = #{paymentToken} AND provider = 'MOCK' LIMIT 1")
    PaymentTransaction findMockByToken(@Param("paymentToken") String paymentToken);

    /**
     * 订单详情只读取安全支付摘要。调用方已经通过订单归属校验，故跨租户读取时不依赖当前顾客租户。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM payment_transaction WHERE order_id = #{orderId} ORDER BY id DESC LIMIT 1")
    PaymentTransaction findLatestByOrderId(@Param("orderId") Long orderId);

    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE payment_transaction SET status = 'SUCCESS', provider_trade_no = #{providerTradeNo}, " +
            "callback_payload = #{payload}, paid_at = NOW() WHERE id = #{id} AND status = 'PENDING'")
    int markSuccess(@Param("id") Long id, @Param("providerTradeNo") String providerTradeNo,
                    @Param("payload") String payload);

    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE payment_transaction SET payment_token = #{paymentToken}, expires_at = #{expiresAt}, qr_code = #{qrCode} " +
            "WHERE id = #{id} AND status = 'PENDING'")
    int refreshMockCheckout(@Param("id") Long id, @Param("paymentToken") String paymentToken,
                            @Param("expiresAt") java.time.LocalDateTime expiresAt, @Param("qrCode") String qrCode);

    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE payment_transaction SET status = 'CLOSED' WHERE id = #{id} AND status = 'PENDING'")
    int markClosed(@Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE payment_transaction SET status = 'CLOSED' WHERE order_id = #{orderId} AND status = 'PENDING'")
    int closePendingByOrderId(@Param("orderId") Long orderId);

    /** 切换为模拟付款时，关闭历史渠道遗留的待付款流水，避免同一订单存在两个可用付款入口。 */
    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE payment_transaction SET status = 'CLOSED' WHERE order_id = #{orderId} AND status = 'PENDING' AND provider <> 'MOCK'")
    int closeLegacyPendingByOrderId(@Param("orderId") Long orderId);

    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE payment_transaction SET status = 'REFUNDED' WHERE order_id = #{orderId} AND status = 'SUCCESS'")
    int markRefundedByOrderId(@Param("orderId") Long orderId);
}
