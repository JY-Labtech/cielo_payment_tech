package br.com.jylabtech.cielo_payment_tech.licence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class LicenceConfigTest {

    private static final String CORE = "https://portal-api.jylabtech.com.br/pos-payments/licence/check";
    private static final String CORE_BASE64 =
            "aHR0cHM6Ly9wb3J0YWwtYXBpLmp5bGFidGVjaC5jb20uYnIvcG9zLXBheW1lbnRzL2xpY2VuY2UvY2hlY2s=";
    private static final String LEGACY =
            "https://pos-payments-api-577464235408.southamerica-east1.run.app/pos-payments/licence/check";

    private static String b64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void defaultUrlIsTheCore() {
        assertEquals(CORE, LicenceConfig.LICENCE_CHECK_URL);
    }

    @Test
    public void coreUrlInPlainTextIsAccepted() {
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl(CORE));
    }

    @Test
    public void coreUrlInBase64IsAccepted() {
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl(CORE_BASE64));
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl(b64(CORE)));
    }

    @Test
    public void invoiceAndSubscriptionVariantsAreAccepted() {
        assertEquals(CORE + "/invoice", LicenceConfig.resolveLicenceUrl(CORE + "/invoice"));
        assertEquals(CORE + "/subscription", LicenceConfig.resolveLicenceUrl(b64(CORE + "/subscription")));
        assertEquals(CORE + "/invoice", LicenceConfig.resolveLicenceUrl(
                "aHR0cHM6Ly9wb3J0YWwtYXBpLmp5bGFidGVjaC5jb20uYnIvcG9zLXBheW1lbnRzL2xpY2VuY2UvY2hlY2svaW52b2ljZQ=="));
    }

    @Test
    public void legacyCloudRunUrlFallsBackToCore() {
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl(LEGACY));
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl(b64(LEGACY)));
    }

    @Test
    public void otherHostsFallBackToCore() {
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl("https://evil.com/pos-payments/licence/check"));
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl(b64("https://evil.com/pos-payments/licence/check")));
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl(
                "https://portal-api.jylabtech.com.br.evil.com/pos-payments/licence/check"));
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl(
                "https://evil.com/portal-api.jylabtech.com.br/pos-payments/licence/check"));
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl(
                "https://xportal-api.jylabtech.com.br/pos-payments/licence/check"));
    }

    @Test
    public void userInfoFallsBackToCore() {
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl(
                "https://portal-api.jylabtech.com.br@evil.com/pos-payments/licence/check"));
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl(
                "https://user:pass@portal-api.jylabtech.com.br/pos-payments/licence/check"));
    }

    @Test
    public void plainHttpAndOtherPortsFallBackToCore() {
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl("http://portal-api.jylabtech.com.br/pos-payments/licence/check"));
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl(
                "https://portal-api.jylabtech.com.br:8443/pos-payments/licence/check"));
    }

    @Test
    public void otherPathsFallBackToCore() {
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl("https://portal-api.jylabtech.com.br/other"));
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl("https://portal-api.jylabtech.com.br/pos-payments/licence/checkX"));
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl(
                "https://portal-api.jylabtech.com.br/pos-payments/licence/check/../../admin"));
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl(CORE + "?redirect=https://evil.com"));
    }

    @Test
    public void emptyNullAndGarbageFallBackToCore() {
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl(null));
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl(""));
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl("   "));
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl("%%%not-base64%%%"));
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl("abcde"));
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl("SUA_LICENCE_INTERNAL_KEY"));
    }

    @Test
    public void isOfficialLicenceUrl() {
        assertTrue(LicenceConfig.isOfficialLicenceUrl(CORE));
        assertFalse(LicenceConfig.isOfficialLicenceUrl(LEGACY));
        assertFalse(LicenceConfig.isOfficialLicenceUrl(null));
    }

    @Test
    public void officialLicenceInternalKeyAcceptsOnlyTheCore() {
        assertTrue(LicenceConfig.isOfficialLicenceInternalKey(CORE));
        assertTrue(LicenceConfig.isOfficialLicenceInternalKey(CORE_BASE64));
        assertTrue(LicenceConfig.isOfficialLicenceInternalKey(b64(CORE + "/invoice")));
        assertFalse(LicenceConfig.isOfficialLicenceInternalKey(LEGACY));
        assertFalse(LicenceConfig.isOfficialLicenceInternalKey(b64(LEGACY)));
        assertFalse(LicenceConfig.isOfficialLicenceInternalKey(b64("https://evil.com/pos-payments/licence/check")));
        assertFalse(LicenceConfig.isOfficialLicenceInternalKey(null));
        assertFalse(LicenceConfig.isOfficialLicenceInternalKey(""));
    }

    @Test
    public void base64DecoderMatchesJdk() {
        String[] samples = {"a", "ab", "abc", "abcd", CORE, LEGACY, "ção/ü"};
        for (String sample : samples) {
            assertEquals(sample, LicenceConfig.decodeBase64(b64(sample)));
            assertEquals(sample, LicenceConfig.decodeBase64(b64(sample).replace("=", "")));
            assertEquals(sample, LicenceConfig.decodeBase64(
                    Base64.getUrlEncoder().encodeToString(sample.getBytes(StandardCharsets.UTF_8))));
        }
    }
}
