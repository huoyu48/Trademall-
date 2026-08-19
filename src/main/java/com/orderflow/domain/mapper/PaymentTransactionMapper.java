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
    @Select("SELECT * FROM payment_transaction WHERE order_id = #{orderId} AND provider = 'ALIPAY' LIMIT 1")
    PaymentTransaction findAlipayByOrderId(@Param("orderId") Long orderId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM payment_transaction WHERE out_trade_no = #{outTradeNo} AND provider = 'ALIPAY' LIMIT 1")
    PaymentTransaction findAlipayByOutTradeNo(@Param("outTradeNo") String outTradeNo);

    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE payment_transaction SET status = 'SUCCESS', alipay_trade_no = #{alipayTradeNo}, " +
            "callback_payload = #{payload}, paid_at = NOW() WHERE id = #{id} AND status = 'PENDING'")
    int markSuccess(@Param("id") Long id, @Param("alipayTradeNo") String alipayTradeNo,
                    @Param("payload") String payload);

    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE payment_transaction SET qr_code = #{qrCode} WHERE id = #{id} AND status = 'PENDING'")
    int updateQrCode(@Param("id") Long id, @Param("qrCode") String qrCode);

    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE payment_transaction SET status = 'CLOSED' WHERE id = #{id} AND status = 'PENDING'")
    int markClosed(@Param("id") Long id);
}
