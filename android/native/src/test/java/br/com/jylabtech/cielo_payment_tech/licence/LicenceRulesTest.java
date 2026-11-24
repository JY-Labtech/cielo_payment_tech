package br.com.jylabtech.cielo_payment_tech.licence;

import static br.com.jylabtech.cielo_payment_tech.licence.TestData.CORE;
import static br.com.jylabtech.cielo_payment_tech.licence.TestData.LICENCE_KEY;
import static br.com.jylabtech.cielo_payment_tech.licence.TestData.cache;
import static br.com.jylabtech.cielo_payment_tech.licence.TestData.coreLicenceJson;
import static br.com.jylabtech.cielo_payment_tech.licence.TestData.date;
import static br.com.jylabtech.cielo_payment_tech.licence.TestData.entity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class LicenceRulesTest {

    private final Date now = date("2026-10-01T12:00:00");

    // ---- isLicenceUsable ----

    @Test
    public void validLicenceIsUsable() {
        assertTrue(LicenceRules.isLicenceUsable(entity("2026-12-31T23:59:59"), now));
    }

    @Test
    public void masterKey2099IsUsable() {
        assertTrue(LicenceRules.isLicenceUsable(entity("2099-12-31T23:59:59"), now));
    }

    @Test
    public void licenceExpiredByDateIsNotUsable() {
        assertFalse(LicenceRules.isLicenceUsable(entity("2026-09-30T23:59:59"), now));
        assertFalse(LicenceRules.isLicenceUsable(entity("2026-10-01T12:00:00"), now));
    }

    @Test
    public void isExpiredFlagBlocks() {
        CheckLicenceEntity e = entity("2026-12-31T23:59:59");
        e.setExpired(true);
        assertFalse(LicenceRules.isLicenceUsable(e, now));
    }

    @Test
    public void isInactiveFlagBlocks() {
        CheckLicenceEntity e = entity("2026-12-31T23:59:59");
        e.setInactive(true);
        assertFalse(LicenceRules.isLicenceUsable(e, now));
    }

    @Test
    public void isCanceledFlagBlocks() {
        CheckLicenceEntity e = entity("2026-12-31T23:59:59");
        e.setCanceled(true);
        assertFalse(LicenceRules.isLicenceUsable(e, now));
    }

    @Test
    public void nullExpiredAtOrNullEntityIsNotUsable() {
        CheckLicenceEntity e = entity("2026-12-31T23:59:59");
        e.setExpiredAt(null);
        assertFalse(LicenceRules.isLicenceUsable(e, now));
        assertFalse(LicenceRules.isLicenceUsable(null, now));
    }

    // ---- parse da resposta real do core ----

    @Test
    public void parsesCoreLegacyLicenceJson() {
        CheckLicenceEntity e = CheckLicenceEntity.fromJson(coreLicenceJson("2026-12-31T23:59:59", false, false, false));
        assertNotNull(e);
        assertEquals(date("2026-12-31T23:59:59"), e.getExpiredAt());
        assertEquals(LICENCE_KEY, e.getLicenceEncrypt());
        assertEquals(LICENCE_KEY, e.getLicenceKey());
        assertFalse(e.isExpired());
        assertFalse(e.isInactive());
        assertFalse(e.isCanceled());
        assertEquals(date("2026-01-10T08:30:00"), e.getCreatedAt());
        assertTrue(LicenceRules.isLicenceUsable(e, now));
    }

    @Test
    public void parsesCanceledFlag() {
        CheckLicenceEntity e = CheckLicenceEntity.fromJson(coreLicenceJson("2026-12-31T23:59:59", false, false, true));
        assertNotNull(e);
        assertTrue(e.isCanceled());
    }

    @Test
    public void expiredAtIsBrasiliaTime() {
        CheckLicenceEntity e = CheckLicenceEntity.fromJson(coreLicenceJson("2026-12-31T23:59:59", false, false, false));
        assertNotNull(e);
        // 23:59:59 em Brasília (UTC-3) = 02:59:59 UTC do dia seguinte.
        assertEquals(1798772399000L, e.getExpiredAt().getTime());
    }

    @Test
    public void toJsonRoundTripKeepsFields() {
        CheckLicenceEntity e = CheckLicenceEntity.fromJson(coreLicenceJson("2026-12-31T23:59:59", false, true, true));
        assertNotNull(e);
        e.setSavedLocalDatabaseAt(now);
        e.setLicenceInternalKey(CORE);
        CheckLicenceEntity back = CheckLicenceEntity.fromJson(e.toJson());
        assertNotNull(back);
        assertEquals(e.getExpiredAt(), back.getExpiredAt());
        assertEquals(now, back.getSavedLocalDatabaseAt());
        assertEquals(CORE, back.getLicenceInternalKey());
        assertTrue(back.isInactive());
        assertTrue(back.isCanceled());
    }

    @Test
    public void invalidJsonReturnsNull() {
        assertNull(CheckLicenceEntity.fromJson("not json"));
        assertNull(CheckLicenceEntity.fromJson(""));
        assertNull(CheckLicenceEntity.fromJson(null));
    }

    // ---- decisão do resultado HTTP ----

    @Test
    public void http200ValidIsSuccess() {
        LicenceResult r = LicenceRules.fromHttpResponse(200, coreLicenceJson("2026-12-31T23:59:59", false, false, false), now);
        assertTrue(r.isSuccess());
        assertEquals(LicenceRules.RESULT_SUCCESS, r.getResult());
        assertNotNull(r.getLicenceEntity());
    }

    @Test
    public void http200MasterKeyIsSuccess() {
        assertTrue(LicenceRules.fromHttpResponse(200, coreLicenceJson("2099-12-31T23:59:59", false, false, false), now).isSuccess());
    }

    @Test
    public void http200ExpiredIsError() {
        LicenceResult byDate = LicenceRules.fromHttpResponse(200, coreLicenceJson("2026-09-01T00:00:00", false, false, false), now);
        assertFalse(byDate.isSuccess());
        assertEquals(LicenceRules.RESULT_ERROR, byDate.getResult());
        assertEquals("Licença vencida.", byDate.getErrorMessage());

        LicenceResult byFlag = LicenceRules.fromHttpResponse(200, coreLicenceJson("2026-12-31T23:59:59", true, false, false), now);
        assertFalse(byFlag.isSuccess());
    }

    @Test
    public void http200InactiveIsError() {
        LicenceResult r = LicenceRules.fromHttpResponse(200, coreLicenceJson("2026-12-31T23:59:59", false, true, false), now);
        assertFalse(r.isSuccess());
        assertEquals("Licença inativa ou suspensa.", r.getErrorMessage());
    }

    @Test
    public void http200CanceledIsError() {
        LicenceResult r = LicenceRules.fromHttpResponse(200, coreLicenceJson("2026-12-31T23:59:59", false, false, true), now);
        assertFalse(r.isSuccess());
        assertEquals("Licença cancelada.", r.getErrorMessage());
    }

    @Test
    public void http200InvalidJsonIsError() {
        LicenceResult r = LicenceRules.fromHttpResponse(200, "<html>ok</html>", now);
        assertFalse(r.isSuccess());
        assertNotNull(r.getErrorMessage());
        assertFalse(LicenceRules.fromHttpResponse(200, "", now).isSuccess());
        assertFalse(LicenceRules.fromHttpResponse(200, null, now).isSuccess());
    }

    @Test
    public void http200WithoutExpiredAtIsError() {
        LicenceResult r = LicenceRules.fromHttpResponse(200, "{\"licenceEncrypt\":\"" + LICENCE_KEY + "\"}", now);
        assertFalse(r.isSuccess());
        assertNull(r.getLicenceEntity());
        assertEquals("Resposta do servidor de licença sem data de validade.", r.getErrorMessage());
    }

    @Test
    public void httpErrorsUseServerMessage() {
        int[] codes = {404, 412, 428, 403, 400, 429, 500};
        for (int code : codes) {
            LicenceResult r = LicenceRules.fromHttpResponse(code, "{\"message\":\"Mensagem " + code + "\"}", now);
            assertFalse(r.isSuccess());
            assertEquals(LicenceRules.RESULT_ERROR, r.getResult());
            assertEquals("Mensagem " + code, r.getErrorMessage());
        }
    }

    @Test
    public void httpErrorWithoutJsonBodyUsesFallbackMessage() {
        assertEquals("Erro ao validar licença (HTTP 502)", LicenceRules.fromHttpResponse(502, null, now).getErrorMessage());
        assertEquals("Erro ao validar licença (HTTP 500)", LicenceRules.fromHttpResponse(500, "", now).getErrorMessage());
        assertEquals("Erro ao validar licença (HTTP 503)", LicenceRules.fromHttpResponse(503, "<html>down</html>", now).getErrorMessage());
        assertEquals("Erro ao validar licença (HTTP 404)", LicenceRules.fromHttpResponse(404, "{\"message\":null}", now).getErrorMessage());
        assertEquals("Erro ao validar licença (HTTP 404)", LicenceRules.fromHttpResponse(404, "{}", now).getErrorMessage());
    }

    @Test
    public void http2xxOtherThan200IsError() {
        assertFalse(LicenceRules.fromHttpResponse(204, null, now).isSuccess());
    }

    @Test
    public void refusalsAreDefinitiveAndOutagesAreNot() {
        for (int code : new int[]{404, 412, 428, 403, 400}) {
            assertTrue("HTTP " + code, LicenceRules.fromHttpResponse(code, "{\"message\":\"x\"}", now).isRejectedByServer());
        }
        for (int code : new int[]{408, 429, 500, 502, 503, 204}) {
            assertFalse("HTTP " + code, LicenceRules.fromHttpResponse(code, "{\"message\":\"x\"}", now).isRejectedByServer());
        }
        assertTrue(LicenceRules.fromHttpResponse(200, coreLicenceJson("2026-12-31T23:59:59", false, false, true), now).isRejectedByServer());
        assertTrue(LicenceRules.fromHttpResponse(200, coreLicenceJson("2026-09-01T00:00:00", false, false, false), now).isRejectedByServer());
        assertTrue(LicenceRules.fromHttpResponse(200, coreLicenceJson("2026-12-31T23:59:59", false, true, false), now).isRejectedByServer());
        assertTrue(LicenceRules.fromHttpResponse(200, "{\"licenceEncrypt\":\"" + LICENCE_KEY + "\"}", now).isRejectedByServer());
        assertFalse(LicenceRules.fromHttpResponse(200, coreLicenceJson("2026-12-31T23:59:59", false, false, false), now).isRejectedByServer());
        // corpo ilegível (proxy/captive portal) não é recusa da licença
        assertFalse(LicenceRules.fromHttpResponse(200, "<html>oops</html>", now).isRejectedByServer());
        assertFalse(LicenceRules.fromHttpResponse(200, null, now).isRejectedByServer());
    }

    @Test
    public void customerBlockedReasonHasClearMessage() {
        LicenceResult r = LicenceRules.fromHttpResponse(428,
                "{\"message\":\"A data de expiração da licença é inválida. [-]\",\"reason\":\"customer_blocked\"}", now);
        assertTrue(r.isRejectedByServer());
        assertEquals("Cliente bloqueado. Entre em contato com a JY Labtech.", r.getErrorMessage());
    }

    @Test
    public void definitiveRejectionCodes() {
        for (int code : new int[]{400, 401, 402, 403, 404, 409, 410, 412, 423, 428}) {
            assertTrue("HTTP " + code, LicenceRules.isDefinitiveRejection(code));
        }
        for (int code : new int[]{200, 204, 301, 408, 429, 500, 502, 503, 504}) {
            assertFalse("HTTP " + code, LicenceRules.isDefinitiveRejection(code));
        }
    }

    @Test
    public void newLicenceResultStartsAsError() {
        LicenceResult r = new LicenceResult();
        assertFalse(r.isSuccess());
        assertEquals(LicenceRules.RESULT_ERROR, r.getResult());
    }

    // ---- regras do cache ----

    @Test
    public void validCacheIsUsable() {
        Date savedAt = new Date(now.getTime() - TimeUnit.HOURS.toMillis(48));
        assertTrue(LicenceRules.isCacheUsable(cache("2026-12-31T23:59:59", savedAt), now, LICENCE_KEY, CORE));
    }

    @Test
    public void cacheExactly72HoursOldIsUsable() {
        Date savedAt = new Date(now.getTime() - TimeUnit.HOURS.toMillis(72));
        assertTrue(LicenceRules.isCacheUsable(cache("2026-12-31T23:59:59", savedAt), now, LICENCE_KEY, CORE));
    }

    @Test
    public void cacheOlderThan72HoursIsNotUsable() {
        Date savedAt = new Date(now.getTime() - TimeUnit.HOURS.toMillis(72) - 1000);
        assertFalse(LicenceRules.isCacheUsable(cache("2026-12-31T23:59:59", savedAt), now, LICENCE_KEY, CORE));
    }

    @Test
    public void cacheOfFourDaysIsNoLongerUsable() {
        Date savedAt = new Date(now.getTime() - TimeUnit.DAYS.toMillis(4));
        assertFalse(LicenceRules.isCacheUsable(cache("2026-12-31T23:59:59", savedAt), now, LICENCE_KEY, CORE));
    }

    @Test
    public void shouldRevalidateOnFirstCheckOfProcessAndAfterInterval() {
        CheckLicenceEntity c = cache("2026-12-31T23:59:59", new Date(now.getTime() - TimeUnit.MINUTES.toMillis(10)));
        assertTrue(LicenceRules.shouldRevalidate(c, false, now));
        assertFalse(LicenceRules.shouldRevalidate(c, true, now));
        assertTrue(LicenceRules.shouldRevalidate(null, true, now));
        c.setSavedLocalDatabaseAt(new Date(now.getTime() - LicenceRules.REVALIDATE_INTERVAL_MILLIS));
        assertTrue(LicenceRules.shouldRevalidate(c, true, now));
        c.setSavedLocalDatabaseAt(null);
        assertTrue(LicenceRules.shouldRevalidate(c, true, now));
    }

    @Test
    public void cacheSavedInTheFutureIsNotUsable() {
        Date future = new Date(now.getTime() + TimeUnit.MINUTES.toMillis(10));
        assertFalse(LicenceRules.isCacheUsable(cache("2026-12-31T23:59:59", future), now, LICENCE_KEY, CORE));
        Date withinTolerance = new Date(now.getTime() + TimeUnit.MINUTES.toMillis(4));
        assertTrue(LicenceRules.isCacheUsable(cache("2026-12-31T23:59:59", withinTolerance), now, LICENCE_KEY, CORE));
    }

    @Test
    public void cacheWithoutSavedAtIsNotUsable() {
        assertFalse(LicenceRules.isCacheUsable(cache("2026-12-31T23:59:59", null), now, LICENCE_KEY, CORE));
    }

    @Test
    public void cacheOfAnotherKeyIsNotUsable() {
        assertFalse(LicenceRules.isCacheUsable(cache("2026-12-31T23:59:59", now), now, "OTHER-KEY", CORE));
        assertFalse(LicenceRules.isCacheUsable(cache("2026-12-31T23:59:59", now), now, null, CORE));
    }

    @Test
    public void cacheOfAnotherUrlIsNotUsable() {
        assertFalse(LicenceRules.isCacheUsable(cache("2026-12-31T23:59:59", now), now, LICENCE_KEY, CORE + "/invoice"));
        CheckLicenceEntity fromEvilServer = cache("2026-12-31T23:59:59", now);
        fromEvilServer.setLicenceInternalKey("https://evil.com/pos-payments/licence/check");
        assertFalse(LicenceRules.isCacheUsable(fromEvilServer, now, LICENCE_KEY, CORE));
        CheckLicenceEntity withoutUrl = cache("2026-12-31T23:59:59", now);
        withoutUrl.setLicenceInternalKey(null);
        assertFalse(LicenceRules.isCacheUsable(withoutUrl, now, LICENCE_KEY, CORE));
    }

    @Test
    public void cacheSavedWithLegacyCloudRunKeyIsNotUsable() {
        String legacy = "https://pos-payments-api-577464235408.southamerica-east1.run.app/pos-payments/licence/check";
        CheckLicenceEntity plain = cache("2026-12-31T23:59:59", now);
        plain.setLicenceInternalKey(legacy);
        assertFalse(LicenceRules.isCacheUsable(plain, now, LICENCE_KEY, CORE));

        CheckLicenceEntity base64 = cache("2026-12-31T23:59:59", now);
        base64.setLicenceInternalKey(java.util.Base64.getEncoder().encodeToString(
                legacy.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertFalse(LicenceRules.isCacheUsable(base64, now, LICENCE_KEY, CORE));
    }

    @Test
    public void cacheSavedWithCoreBase64KeyIsUsable() {
        CheckLicenceEntity old = cache("2026-12-31T23:59:59", now);
        // Versões anteriores salvavam o valor enviado pelo app (base64 do core).
        old.setLicenceInternalKey("aHR0cHM6Ly9wb3J0YWwtYXBpLmp5bGFidGVjaC5jb20uYnIvcG9zLXBheW1lbnRzL2xpY2VuY2UvY2hlY2s=");
        assertTrue(LicenceRules.isCacheUsable(old, now, LICENCE_KEY, CORE));
    }

    @Test
    public void expiredLicenceInCacheIsNotUsable() {
        assertFalse(LicenceRules.isCacheUsable(cache("2026-09-30T00:00:00", now), now, LICENCE_KEY, CORE));
        CheckLicenceEntity canceled = cache("2026-12-31T23:59:59", now);
        canceled.setCanceled(true);
        assertFalse(LicenceRules.isCacheUsable(canceled, now, LICENCE_KEY, CORE));
    }
}
