package br.com.jylabtech.cielo_payment_tech.licence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class LicenceConfigTest {

    private static final String CORE = "https://licence-server.invalid/pos-payments/licence/check";
    private static final String CORE_BASE64 =
            "***REMOVIDO***";
    private static final String LEGACY =
            "https://licence-server.invalid/pos-payments/licence/check";

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
                "***REMOVIDO***"));
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
                "https://licence-server.invalid.evil.com/pos-payments/licence/check"));
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl(
                "https://evil.com/licence-server.invalid/pos-payments/licence/check"));
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl(
                "https://xlicence-server.invalid/pos-payments/licence/check"));
    }

    @Test
    public void userInfoFallsBackToCore() {
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl(
                "https://licence-server.invalid@evil.com/pos-payments/licence/check"));
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl(
                "https://user:pass@licence-server.invalid/pos-payments/licence/check"));
    }

    @Test
    public void plainHttpAndOtherPortsFallBackToCore() {
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl("http://licence-server.invalid/pos-payments/licence/check"));
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl(
                "https://licence-server.invalid:8443/pos-payments/licence/check"));
    }

    @Test
    public void otherPathsFallBackToCore() {
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl("https://licence-server.invalid/other"));
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl("https://licence-server.invalid/pos-payments/licence/checkX"));
        assertEquals(CORE, LicenceConfig.resolveLicenceUrl(
                "https://licence-server.invalid/pos-payments/licence/check/../../admin"));
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
