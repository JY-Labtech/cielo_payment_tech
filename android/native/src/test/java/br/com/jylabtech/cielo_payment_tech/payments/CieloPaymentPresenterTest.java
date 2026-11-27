package br.com.jylabtech.cielo_payment_tech.payments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import br.com.jylabtech.cielo_payment_tech.core.CieloTech;
import io.flutter.plugin.common.MethodChannel;

public class CieloPaymentPresenterTest {

    private static final String REQUEST = "eyJ2YWx1ZSI6MTAwMH0=";

    private final List<String> launchedUrls = new ArrayList<>();
    private boolean lioInstalled;
    private Exception launchError;

    private MethodChannel channel;
    private CieloTech cieloTech;
    private MethodChannel.Result result;
    private CieloPaymentPresenter presenter;

    @Before
    public void setUp() {
        lioInstalled = true;
        launchError = null;
        launchedUrls.clear();
        channel = mock(MethodChannel.class);
        cieloTech = mock(CieloTech.class);
        result = mock(MethodChannel.Result.class);
        presenter = new CieloPaymentPresenter(channel, cieloTech, url -> {
            if (launchError != null) {
                throw launchError;
            }
            launchedUrls.add(url);
            return lioInstalled;
        });
    }

    @Test
    public void buildLioUrlConcatenatesRequestAndCallback() {
        assertEquals(
                "lio://payment?request=abc&urlCallback=order://payment",
                CieloPaymentPresenter.buildLioUrl("lio://payment", "abc", "order://payment"));
    }

    @Test
    public void checkoutOpensLioPaymentAndRespondsTrueOnce() {
        presenter.checkout(REQUEST, "order://payment", result);

        assertEquals(1, launchedUrls.size());
        assertEquals("lio://payment?request=" + REQUEST + "&urlCallback=order://payment", launchedUrls.get(0));
        verify(cieloTech).setUrlCallback("order://payment");
        verify(channel).invokeMethod("onMessage", "Pagamento iniciado. Aguardando resposta do LIO...");
        verify(result).success(true);
        verifyNoMoreInteractions(result);
    }

    @Test
    public void cancelOpensLioPaymentReversal() {
        presenter.cancel(REQUEST, "order://response", result);

        assertEquals("lio://payment-reversal?request=" + REQUEST + "&urlCallback=order://response", launchedUrls.get(0));
        verify(cieloTech).setUrlCallback("order://response");
        verify(channel).invokeMethod("onMessage", "Cancelamento iniciado. Aguardando resposta do LIO...");
        verify(result).success(true);
        verifyNoMoreInteractions(result);
    }

    @Test
    public void printOpensLioPrint() {
        presenter.print(REQUEST, "order://response", result);

        assertEquals("lio://print?request=" + REQUEST + "&urlCallback=order://response", launchedUrls.get(0));
        verify(channel).invokeMethod("onMessage", "Impressão iniciada. Aguardando resposta do LIO...");
        verify(result).success(true);
        verifyNoMoreInteractions(result);
    }

    @Test
    public void lioNotInstalledSendsOnErrorAndStillRespondsTrue() {
        lioInstalled = false;

        presenter.checkout(REQUEST, "order://payment", result);

        verify(channel).invokeMethod("onError", "Erro: App LIO não instalado.");
        verify(channel, never()).invokeMethod(eq("onMessage"), any());
        verify(result).success(true);
        verifyNoMoreInteractions(result);
    }

    @Test
    public void launchFailureRespondsErrorOnce() {
        launchError = new IllegalStateException("falhou");

        presenter.print(REQUEST, "order://response", result);

        verify(result).error(eq("ERROR"), eq("falhou"), isNull());
        verify(result, never()).success(any());
        verifyNoMoreInteractions(result);
    }

    @Test
    public void everyOperationResolvesWithoutNetwork() {
        presenter.checkout(REQUEST, "order://payment", result);
        presenter.cancel(REQUEST, "order://response", result);
        presenter.print(REQUEST, "order://response", result);

        assertEquals(3, launchedUrls.size());
        for (String url : launchedUrls) {
            assertTrue(url.startsWith("lio://"));
        }
        verify(result, org.mockito.Mockito.times(3)).success(true);
        verify(result, never()).error(anyString(), any(), any());
    }

    @Test
    public void deepLinkAndDisposeDelegateToCieloTech() {
        presenter.handleDeepLink(null);
        presenter.dispose();

        verify(cieloTech).handleDeepLink(null);
        verify(cieloTech).dispose();
    }
}
