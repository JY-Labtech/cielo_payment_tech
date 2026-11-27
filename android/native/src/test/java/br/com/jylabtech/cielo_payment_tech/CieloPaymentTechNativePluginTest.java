package br.com.jylabtech.cielo_payment_tech;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import br.com.jylabtech.cielo_payment_tech.payments.CieloPaymentPresenter;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

public class CieloPaymentTechNativePluginTest {

    private CieloPaymentTechNativePlugin plugin;
    private CieloPaymentPresenter presenter;
    private MethodChannel.Result result;

    @Before
    public void setUp() {
        plugin = new CieloPaymentTechNativePlugin();
        presenter = mock(CieloPaymentPresenter.class);
        plugin.setPaymentPresenter(presenter);
        result = mock(MethodChannel.Result.class);
    }

    private static Map<String, Object> args(String key, String value, String urlCallback) {
        Map<String, Object> map = new HashMap<>();
        if (value != null) map.put(key, value);
        if (urlCallback != null) map.put("urlCallback", urlCallback);
        return map;
    }

    @Test
    public void paymentCheckoutCallsPresenterDirectly() {
        plugin.onMethodCall(new MethodCall("paymentCheckout", args("paymentRequest", "REQ", "order://payment")), result);

        verify(presenter).checkout("REQ", "order://payment", result);
    }

    @Test
    public void paymentCancelCallsPresenterDirectly() {
        plugin.onMethodCall(new MethodCall("paymentCancel", args("cancelRequest", "REQ", "order://response")), result);

        verify(presenter).cancel("REQ", "order://response", result);
    }

    @Test
    public void printCallsPresenterDirectly() {
        plugin.onMethodCall(new MethodCall("print", args("printRequest", "REQ", "order://response")), result);

        verify(presenter).print("REQ", "order://response", result);
    }

    @Test
    public void checkoutWithoutArgumentsIsInvalid() {
        plugin.onMethodCall(new MethodCall("paymentCheckout", args("paymentRequest", null, "order://payment")), result);

        verify(result).error(eq("INVALID_ARGUMENT"), anyString(), isNull());
        verifyNoInteractions(presenter);
    }

    @Test
    public void cancelWithoutCallbackIsInvalid() {
        plugin.onMethodCall(new MethodCall("paymentCancel", args("cancelRequest", "REQ", null)), result);

        verify(result).error(eq("INVALID_ARGUMENT"), anyString(), isNull());
        verifyNoInteractions(presenter);
    }

    @Test
    public void printWithoutRequestIsInvalid() {
        plugin.onMethodCall(new MethodCall("print", args("printRequest", null, null)), result);

        verify(result).error(eq("INVALID_ARGUMENT"), anyString(), isNull());
        verifyNoInteractions(presenter);
    }

    @Test
    public void removedInitPaymentIsNotImplemented() {
        plugin.onMethodCall(new MethodCall("initPayment", new HashMap<>()), result);

        verify(result).notImplemented();
        verifyNoMoreInteractions(result);
    }

    @Test
    public void creditAndDebitAreNotImplemented() {
        plugin.onMethodCall(new MethodCall("paymentCredit", new HashMap<>()), result);
        plugin.onMethodCall(new MethodCall("paymentDebit", new HashMap<>()), result);

        verify(result, org.mockito.Mockito.times(2)).notImplemented();
        verifyNoInteractions(presenter);
    }

    @Test
    public void unknownMethodIsNotImplemented() {
        plugin.onMethodCall(new MethodCall("foo", null), result);

        verify(result).notImplemented();
    }

    @Test
    public void getPlatformVersionReturnsAndroidPrefix() {
        plugin.onMethodCall(new MethodCall("getPlatformVersion", null), result);

        verify(result).success(any(String.class));
    }
}
