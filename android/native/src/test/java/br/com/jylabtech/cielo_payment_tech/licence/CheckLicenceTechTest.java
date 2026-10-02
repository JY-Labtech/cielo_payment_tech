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

    private static final class FakeNetwork implements LicenceNetwork {
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
            return LicenceRules.fromHttpResponse(status, body, NOW);
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

    private FakeNetwork network;
    private FakeStorage storage;
    private CheckLicenceTech tech;

    @Before
    public void setUp() {
        network = new FakeNetwork();
        storage = new FakeStorage();
        tech = new CheckLicenceTech(network, storage);
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
    public void validCacheSkipsNetwork() {
        CheckLicenceEntity cached = cache("2026-12-31T23:59:59", new Date(NOW.getTime() - TimeUnit.DAYS.toMillis(1)));
        storage.saved = cached;

        LicenceResult r = tech.validateLicence(LICENCE_KEY, CORE, NOW);

        assertTrue(r.isSuccess());
        assertEquals(0, network.calls);
        assertSame(cached, r.getLicenceEntity());
    }

    @Test
    public void staleCacheIsDeletedAndRevalidatedOnline() {
        storage.saved = cache("2026-12-31T23:59:59", new Date(NOW.getTime() - TimeUnit.DAYS.toMillis(8)));
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
        String legacy = "https://pos-payments-api-577464235408.southamerica-east1.run.app/pos-payments/licence/check";
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
        String coreBase64 = "aHR0cHM6Ly9wb3J0YWwtYXBpLmp5bGFidGVjaC5jb20uYnIvcG9zLXBheW1lbnRzL2xpY2VuY2UvY2hlY2s=";
        CheckLicenceEntity cached = cache("2026-12-31T23:59:59", NOW);
        cached.setLicenceInternalKey(coreBase64);
        storage.saved = cached;

        LicenceResult r = tech.validateLicence(LICENCE_KEY, coreBase64, NOW);

        assertTrue(r.isSuccess());
        assertEquals(0, network.calls);
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
