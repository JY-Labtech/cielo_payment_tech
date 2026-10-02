package br.com.jylabtech.cielo_payment_tech.licence;

import java.text.SimpleDateFormat;
import java.util.Date;

final class TestData {

    static final String LICENCE_KEY = "TEST-LICENCE-KEY-0000";
    static final String CORE = LicenceConfig.LICENCE_CHECK_URL;

    private TestData() {
    }

    static Date date(String value) {
        try {
            SimpleDateFormat sdf = CheckLicenceEntity.newDateFormat();
            return sdf.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** JSON no formato do LegacyLicence do jylabtech_core. */
    static String coreLicenceJson(String expiredAt, boolean isExpired, boolean isInactive, boolean isCanceled) {
        return "{"
                + "\"id\":\"64f1c0ffee0000000000abcd\","
                + "\"uid\":\"64f1c0ffee0000000000abcd\","
                + "\"deviceUid\":\"\","
                + "\"deviceSerialNumber\":\"\","
                + "\"userUid\":\"64f1c0ffee0000000000beef\","
                + "\"productId\":\"64f1c0ffee0000000000cafe\","
                + "\"productName\":\"Licença plugin de pagamento\","
                + "\"productDescription\":\"\","
                + "\"integrationLibId\":\"\","
                + "\"integrationLibName\":\"pos_payments\","
                + "\"checkoutSessionId\":\"\","
                + "\"licenceKey\":\"" + LICENCE_KEY + "\","
                + "\"licenceEncrypt\":\"" + LICENCE_KEY + "\","
                + "\"expiredAt\":\"" + expiredAt + "\","
                + "\"isExpired\":" + isExpired + ","
                + "\"isInactive\":" + isInactive + ","
                + "\"isTrial\":false,"
                + "\"isDebug\":true,"
                + "\"isCanceled\":" + isCanceled + ","
                + "\"canceledAt\":null,"
                + "\"licencesQuantity\":-1,"
                + "\"subscriptionId\":\"\","
                + "\"subscription\":null,"
                + "\"checkoutType\":\"one_time\","
                + "\"createdAt\":\"2026-01-10T08:30:00\","
                + "\"updatedAt\":\"2026-09-30T17:45:12\","
                + "\"coreToken\":\"header.payload.signature\""
                + "}";
    }

    static CheckLicenceEntity entity(String expiredAt) {
        CheckLicenceEntity entity = new CheckLicenceEntity();
        entity.setLicenceKey(LICENCE_KEY);
        entity.setLicenceEncrypt(LICENCE_KEY);
        entity.setExpiredAt(date(expiredAt));
        return entity;
    }

    static CheckLicenceEntity cache(String expiredAt, Date savedAt) {
        CheckLicenceEntity entity = entity(expiredAt);
        entity.setSavedLocalDatabaseAt(savedAt);
        entity.setLicenceInternalKey(CORE);
        return entity;
    }
}
