package br.com.jylabtech.cielo_payment_tech.licence;

import static br.com.jylabtech.cielo_payment_tech.licence.TestData.CORE;
import static br.com.jylabtech.cielo_payment_tech.licence.TestData.LICENCE_KEY;
import static br.com.jylabtech.cielo_payment_tech.licence.TestData.cache;
import static br.com.jylabtech.cielo_payment_tech.licence.TestData.coreLicenceJson;
import static br.com.jylabtech.cielo_payment_tech.licence.TestData.date;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class CheckLicenceTechTest {

    private final class FakeNetwork implements LicenceNetwork {
        int calls;
        String lastUrl;
        String lastKey;
        int status = 200;
        String body;
        RuntimeException failure;

        @Override
        public LicenceResult checkLicence(String url, String licenceKey) {
            calls++;
            lastUrl = url;
            lastKey = licenceKey;
            if (failure != null) {
                throw failure;
            }
            return LicenceRules.fromHttpResponse(status, body, now);
        }
    }

    private static final class FakeStorage implements LicenceStorage {
        CheckLicenceEntity saved;
        int deletes;

        @Override
        public CheckLicenceEntity read() {
            return saved;
        }

        @Override
        public void save(CheckLicenceEntity entity) {
            saved = entity;
        }

        @Override
        public void delete() {
            deletes++;
            saved = null;
        }
    }

    private static final Date NOW = date("2026-10-01T12:00:00");
    private static final String VALID_JSON = coreLicenceJson("2026-12-31T23:59:59", false, false, false);

    private Date now = NOW;

    private FakeNetwork network;
    private FakeStorage storage;
    private CheckLicenceTech tech;

    @Before
    public void setUp() {
        now = NOW;
        network = new FakeNetwork();
        storage = new FakeStorage();
        tech = new CheckLicenceTech(network, storage);
    }

    private void advance(long millis) {
        now = new Date(now.getTime() + millis);
    }

    private LicenceResult validate() {
        return tech.validateLicence(LICENCE_KEY, CORE, now);
    }

    @Test
    public void validOnlineLicenceIsSavedWithNow() {
        network.body = coreLicenceJson("2026-12-31T23:59:59", false, false, false);

        LicenceResult r = tech.validateLicence(LICENCE_KEY, null, NOW);

        assertTrue(r.isSuccess());
        assertEquals(1, network.calls);
        assertEquals(CORE, network.lastUrl);
        assertEquals(LICENCE_KEY, network.lastKey);
        assertNotNull(storage.saved);
        assertEquals(NOW, storage.saved.getSavedLocalDatabaseAt());
        assertEquals(CORE, storage.saved.getLicenceInternalKey());
    }

    @Test
    public void maliciousInternalKeyIsIgnoredAndCoreIsCalled() {
        network.body = coreLicenceJson("2026-12-31T23:59:59", false, false, false);

        tech.validateLicence(LICENCE_KEY, "https://evil.com/pos-payments/licence/check", NOW);

        assertEquals(CORE, network.lastUrl);
    }

    @Test
    public void firstCheckOfProcessGoesOnlineEvenWithValidCache() {
        storage.saved = cache("2026-12-31T23:59:59", new Date(NOW.getTime() - TimeUnit.HOURS.toMillis(1)));
        network.body = VALID_JSON;

        LicenceResult r = validate();

        assertTrue(r.isSuccess());
        assertEquals(1, network.calls);
        assertEquals(NOW, storage.saved.getSavedLocalDatabaseAt());
    }

    @Test
    public void validCacheSkipsNetworkAfterOnlineCheckUntilRevalidationInterval() {
        network.body = VALID_JSON;
        validate();
        assertEquals(1, network.calls);
        CheckLicenceEntity saved = storage.saved;

        advance(TimeUnit.HOURS.toMillis(1));
        LicenceResult r = validate();
        assertTrue(r.isSuccess());
        assertEquals(1, network.calls);
        assertSame(saved, r.getLicenceEntity());

        advance(LicenceRules.REVALIDATE_INTERVAL_MILLIS);
        assertTrue(validate().isSuccess());
        assertEquals(2, network.calls);
    }

    @Test
    public void customerBlockedClearsCacheAndBlocksImmediately() {
        network.body = VALID_JSON;
        validate();
        advance(LicenceRules.REVALIDATE_INTERVAL_MILLIS);
        network.status = 428;
        network.body = "{\"message\":\"A data de expiração da licença é inválida. [-]\",\"reason\":\"customer_blocked\"}";

        LicenceResult r = validate();
        assertFalse(r.isSuccess());
        assertTrue(r.isRejectedByServer());
        assertEquals(LicenceRules.MESSAGE_CUSTOMER_BLOCKED, r.getErrorMessage());
        assertNull(storage.saved);
        // sem cache: a próxima operação também consulta o servidor e continua bloqueada
        assertFalse(validate().isSuccess());
        assertEquals(3, network.calls);
    }

    @Test
    public void everyDefinitiveRefusalClearsValidCache() {
        for (int code : new int[]{404, 412, 428, 403, 400}) {
            setUp();
            storage.saved = cache("2026-12-31T23:59:59", NOW);
            network.status = code;
            network.body = "{\"message\":\"recusada\"}";
            LicenceResult r = validate();
            assertFalse("HTTP " + code, r.isSuccess());
            assertEquals("recusada", r.getErrorMessage());
            assertNull("HTTP " + code, storage.saved);
        }
    }

    @Test
    public void online200CanceledOrExpiredClearsValidCache() {
        storage.saved = cache("2026-12-31T23:59:59", NOW);
        network.body = coreLicenceJson("2026-12-31T23:59:59", false, false, true);
        assertFalse(validate().isSuccess());
        assertNull(storage.saved);

        setUp();
        storage.saved = cache("2026-12-31T23:59:59", NOW);
        network.body = coreLicenceJson("2026-09-01T00:00:00", false, false, false);
        assertFalse(validate().isSuccess());
        assertNull(storage.saved);
    }

    @Test
    public void outageUsesCacheUpTo72Hours() {
        network.body = VALID_JSON;
        validate();
        network.failure = new RuntimeException("sem internet");

        advance(TimeUnit.HOURS.toMillis(71));
        assertTrue(validate().isSuccess());
        assertNotNull(storage.saved);

        advance(TimeUnit.HOURS.toMillis(1) + 1000);
        LicenceResult r = validate();
        assertFalse(r.isSuccess());
        assertTrue(r.getErrorMessage().contains("sem internet"));
        assertNull(storage.saved);
    }

    @Test
    public void serverErrorsUseCache() {
        for (int code : new int[]{500, 502, 503, 408, 429}) {
            setUp();
            storage.saved = cache("2026-12-31T23:59:59", NOW);
            network.status = code;
            network.body = "{\"message\":\"fora do ar\"}";
            assertTrue("HTTP " + code, validate().isSuccess());
            assertNotNull("HTTP " + code, storage.saved);
        }
    }

    @Test
    public void timeoutResultFromProviderUsesCache() {
        // LicenceProvider devolve erro (sem exceção) em timeout: não é recusa do servidor.
        storage.saved = cache("2026-12-31T23:59:59", NOW);
        LicenceNetwork timeout = (url, key) -> LicenceRules.error("Tempo esgotado ao validar a licença.");
        tech = new CheckLicenceTech(timeout, storage);
        assertTrue(validate().isSuccess());
        assertNotNull(storage.saved);
    }

    @Test
    public void unreadable200UsesCache() {
        storage.saved = cache("2026-12-31T23:59:59", NOW);
        network.body = "<html>captive portal</html>";
        assertTrue(validate().isSuccess());
        assertNotNull(storage.saved);
    }

    @Test
    public void afterOutageWaitsBeforeRetryingServer() {
        storage.saved = cache("2026-12-31T23:59:59", NOW);
        network.failure = new RuntimeException("sem internet");
        validate();
        assertEquals(1, network.calls);

        advance(TimeUnit.MINUTES.toMillis(1));
        assertTrue(validate().isSuccess());
        assertEquals(1, network.calls);

        advance(LicenceRules.RETRY_AFTER_FAILURE_MILLIS);
        network.failure = null;
        network.status = 428;
        network.body = "{\"message\":\"Licença vencida\"}";
        assertFalse(validate().isSuccess());
        assertEquals(2, network.calls);
        assertNull(storage.saved);
    }

    @Test
    public void outageWithoutCacheBlocksAndKeepsTrying() {
        network.failure = new RuntimeException("sem internet");
        assertFalse(validate().isSuccess());
        assertFalse(validate().isSuccess());
        assertEquals(2, network.calls);
    }

    @Test
    public void staleCacheIsDeletedAndRevalidatedOnline() {
        storage.saved = cache("2026-12-31T23:59:59", new Date(NOW.getTime() - TimeUnit.HOURS.toMillis(73)));
        network.body = coreLicenceJson("2027-01-31T23:59:59", false, false, false);

        LicenceResult r = tech.validateLicence(LICENCE_KEY, CORE, NOW);

        assertTrue(r.isSuccess());
        assertEquals(1, storage.deletes);
        assertEquals(1, network.calls);
        assertEquals(NOW, storage.saved.getSavedLocalDatabaseAt());
        assertEquals(date("2027-01-31T23:59:59"), storage.saved.getExpiredAt());
    }

    @Test
    public void legacyCloudRunCacheIsDeletedAndRevalidatedOnCore() {
        String legacy = "https://licence-server.invalid/pos-payments/licence/check";
        String[] savedValues = {
                legacy,
                java.util.Base64.getEncoder().encodeToString(legacy.getBytes(java.nio.charset.StandardCharsets.UTF_8))
        };
        for (String saved : savedValues) {
            setUp();
            CheckLicenceEntity cached = cache("2026-12-31T23:59:59", NOW);
            cached.setLicenceInternalKey(saved);
            storage.saved = cached;
            network.body = coreLicenceJson("2026-12-31T23:59:59", false, false, false);

            LicenceResult r = tech.validateLicence(LICENCE_KEY, saved, NOW);

            assertTrue(r.isSuccess());
            assertEquals(1, storage.deletes);
            assertEquals(1, network.calls);
            assertEquals(CORE, network.lastUrl);
            // O cache novo guarda a URL oficial efetiva, não o valor enviado pelo app.
            assertEquals(CORE, storage.saved.getLicenceInternalKey());
        }
    }

    @Test
    public void coreBase64CacheIsValidOffline() {
        String coreBase64 = "***REMOVIDO***";
        CheckLicenceEntity cached = cache("2026-12-31T23:59:59", NOW);
        cached.setLicenceInternalKey(coreBase64);
        storage.saved = cached;
        network.failure = new RuntimeException("sem internet");

        LicenceResult r = tech.validateLicence(LICENCE_KEY, coreBase64, NOW);

        assertTrue(r.isSuccess());
        assertEquals(1, network.calls);
        assertEquals(0, storage.deletes);
    }

    @Test
    public void cacheOfAnotherKeyIsNotUsed() {
        storage.saved = cache("2026-12-31T23:59:59", NOW);
        network.status = 404;
        network.body = "{\"message\":\"Licença não encontrada\"}";

        LicenceResult r = tech.validateLicence("OTHER-KEY", CORE, NOW);

        assertFalse(r.isSuccess());
        assertEquals("Licença não encontrada", r.getErrorMessage());
        assertNull(storage.saved);
    }

    @Test
    public void expiredOnlineLicenceIsErrorAndNotSaved() {
        network.body = coreLicenceJson("2026-09-01T00:00:00", false, false, false);

        LicenceResult r = tech.validateLicence(LICENCE_KEY, CORE, NOW);

        assertFalse(r.isSuccess());
        assertNull(storage.saved);
    }

    @Test
    public void canceledOnlineLicenceIsErrorAndNotSaved() {
        network.body = coreLicenceJson("2026-12-31T23:59:59", false, false, true);

        LicenceResult r = tech.validateLicence(LICENCE_KEY, CORE, NOW);

        assertFalse(r.isSuccess());
        assertNull(storage.saved);
    }

    @Test
    public void serverErrorsAreReturnedWithMessage() {
        network.status = 428;
        network.body = "{\"message\":\"Licença vencida\"}";

        LicenceResult r = tech.validateLicence(LICENCE_KEY, CORE, NOW);

        assertFalse(r.isSuccess());
        assertTrue(r.isRejectedByServer());
        assertEquals("Licença vencida", r.getErrorMessage());
        assertNull(storage.saved);
    }

    @Test
    public void networkExceptionIsError() {
        network.failure = new RuntimeException("timeout");

        LicenceResult r = tech.validateLicence(LICENCE_KEY, CORE, NOW);

        assertFalse(r.isSuccess());
        assertEquals("Erro ao validar licença: timeout", r.getErrorMessage());
    }

    @Test
    public void missingLicenceKeyIsErrorWithoutNetwork() {
        assertFalse(tech.validateLicence(null, CORE, NOW).isSuccess());
        assertFalse(tech.validateLicence("  ", CORE, NOW).isSuccess());
        assertEquals(0, network.calls);
    }
}
