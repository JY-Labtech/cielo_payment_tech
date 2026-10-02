package br.com.jylabtech.cielo_payment_tech.licence;

import android.content.Context;

import java.util.Date;

/**
 * Fluxo de validação: cache local válido (sem rede) ou, se não houver, consulta online
 * ao servidor oficial da JY Labtech. Só devolve sucesso para licença válida e ativa.
 *
 * {@link #validateLicence} faz rede: chame fora da main thread.
 */
public class CheckLicenceTech {

    private final LicenceNetwork network;
    private final LicenceStorage storage;

    public CheckLicenceTech(Context context) {
        this(new LicenceProvider(context), new EncryptedLicenceStorage(context));
    }

    public CheckLicenceTech(LicenceNetwork network, LicenceStorage storage) {
        this.network = network;
        this.storage = storage;
    }

    public LicenceResult validateLicence(String licenceKey, String licenceInternalKey) {
        return validateLicence(licenceKey, licenceInternalKey, new Date());
    }

    public LicenceResult validateLicence(String licenceKey, String licenceInternalKey, Date now) {
        if (licenceKey == null || licenceKey.trim().isEmpty()) {
            return LicenceRules.error(LicenceRules.MESSAGE_NOT_CONFIGURED);
        }
        String key = licenceKey.trim();
        String apiUrl = LicenceConfig.resolveLicenceUrl(licenceInternalKey);

        CheckLicenceEntity cached = readCache();
        if (cached != null) {
            if (LicenceRules.isCacheUsable(cached, now, key, apiUrl)) {
                LicenceResult result = new LicenceResult();
                result.setMethod("licence");
                result.setResult(LicenceRules.RESULT_SUCCESS);
                result.setLicenceEntity(cached);
                return result;
            }
            deleteCache();
        }

        LicenceResult online;
        try {
            online = network.checkLicence(apiUrl, key);
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return LicenceRules.error("Erro ao validar licença: " + message);
        }

        if (online == null) {
            return LicenceRules.error("Erro ao validar licença.");
        }
        if (!online.isSuccess()) {
            if (online.getErrorMessage() == null || online.getErrorMessage().trim().isEmpty()) {
                return LicenceRules.error(null);
            }
            return online;
        }

        CheckLicenceEntity entity = online.getLicenceEntity();
        if (!LicenceRules.isLicenceUsable(entity, now)) {
            return LicenceRules.error(LicenceRules.unusableMessage(entity, now));
        }

        entity.setLicenceInternalKey(apiUrl);
        entity.setSavedLocalDatabaseAt(now);
        try {
            storage.save(entity);
        } catch (Exception ignored) {
        }
        return online;
    }

    private CheckLicenceEntity readCache() {
        try {
            return storage.read();
        } catch (Exception e) {
            return null;
        }
    }

    private void deleteCache() {
        try {
            storage.delete();
        } catch (Exception ignored) {
        }
    }
}
