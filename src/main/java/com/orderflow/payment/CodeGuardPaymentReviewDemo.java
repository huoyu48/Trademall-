package com.orderflow.payment;

/**
 * Intentionally unsafe sample used only to verify the CodeGuard PR review flow.
 * This class must never be merged into the production branch.
 */
final class CodeGuardPaymentReviewDemo {

    private static final String api_key = "sk-demo-payment-secret";

    private CodeGuardPaymentReviewDemo() {
    }

    static String buildPaymentLookupSql(String paymentNo) {
        return "SELECT * FROM payment_transaction WHERE out_trade_no = '" + paymentNo + "'";
    }
}
