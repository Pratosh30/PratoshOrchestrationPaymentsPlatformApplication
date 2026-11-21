package com.pratosh.orchestration.gateway;

import com.pratosh.orchestration.entity.PaymentFailureCategory;
import com.pratosh.orchestration.entity.PaymentProvider;
import com.pratosh.orchestration.entity.PaymentTransaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MockPaymentGatewayTest {

    private final MockPaymentGateway gateway = new MockPaymentGateway();

    private GatewayResult run(String order) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setMerchantOrderId(order);
        return gateway.process(transaction, new PaymentProvider());
    }

    @Test
    void ordinaryOrdersSucceedWithAReference() {
        GatewayResult result = run("ORDER-1");

        assertTrue(result.success());
        assertTrue(result.providerReference().startsWith("MOCK-"));
    }

    @Test
    void failureScenariosAreDeterministic() {
        assertEquals(PaymentFailureCategory.NETWORK_ERROR,
                run("ORDER-FAIL_NETWORK").failureCategory());
        assertEquals(PaymentFailureCategory.PAYMENT_DECLINED,
                run("order-fail_decline").failureCategory());
        assertEquals(PaymentFailureCategory.TIMEOUT,
                run("ORDER-FAIL_TIMEOUT").failureCategory());
    }
}
