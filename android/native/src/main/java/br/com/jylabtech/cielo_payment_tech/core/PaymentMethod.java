package br.com.jylabtech.cielo_payment_tech.core;

public enum PaymentMethod {
    PAYMENT_CREDIT("paymentCredit"),
    PAYMENT_DEBIT("paymentDebit"),
    PAYMENT_CHECKOUT("paymentCheckout"),
    PAYMENT_CANCEL("paymentCancel"),
    PRINT("print");

    private final String method;

    PaymentMethod(String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    }

    public static PaymentMethod fromString(String method) {
        for (PaymentMethod paymentMethod : PaymentMethod.values()) {
            if (paymentMethod.method.equals(method)) {
                return paymentMethod;
            }
        }
        return null;
    }
}

