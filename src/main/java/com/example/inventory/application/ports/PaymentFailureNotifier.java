package com.example.inventory.application.ports;

public interface PaymentFailureNotifier {

    void notifyPaymentFailed(String orderId, String customerId, String reason);
}
