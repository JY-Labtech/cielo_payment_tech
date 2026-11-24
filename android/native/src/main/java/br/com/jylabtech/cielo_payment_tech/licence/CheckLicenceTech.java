package br.com.jylabtech.cielo_payment_tech.licence;

import android.content.Context;

import java.util.Date;

/**
 * Fluxo de validação conforme o contrato de licenças do jylabtech_core:
 *
 * <ul>
 *   <li>A primeira validação de cada processo (abertura do app) sempre consulta o servidor oficial
 *       da JY Labtech, mesmo com cache válido.</li>
 *   <li>Depois disso, com cache válido, usa o cache sem rede e consulta o servidor de novo a cada
 *       {@link LicenceRules#REVALIDATE_INTERVAL_MILLIS} (4h).</li>
 *   <li>Recusa definitiva do servidor (cliente bloqueado, licença vencida/cancelada/suspensa, não
 *       encontrada, aparelho desativado...) apaga o cache e bloqueia na hora.</li>
 *   <li>Falha de rede, timeout, 408, 429 ou 5xx usa o cache válido (salvo há no máximo 72h); sem
 *       cache, bloqueia. Após uma falha, espera {@link LicenceRules#RETRY_AFTER_FAILURE_MILLIS}
 *       para tentar de novo, para não travar cada operação offline no timeout.</li>
 * </ul>
 *
 * Só devolve sucesso para licença válida e ativa. {@link #validateLicence} faz rede: chame fora
 * da main thread.
 */
public class CheckLicenceTech {

    private final LicenceNetwork network;
    private final LicenceStorage storage;

    // Estado desta instância/processo (não persistido): reiniciar o app força nova consulta ao servidor.
    // volatile: lido fora do lock caso um atalho só-cache venha a ser usado (não existe hoje neste
    // plugin; todo gate passa por validateLicence numa thread de background).
    private volatile boolean checkedOnlineInThisProcess;
    private volatile Date lastOnlineFailureAt;

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

    public synchronized LicenceResult validateLicence(String licenceKey, String licenceInternalKey, Date now) {
        if (licenceKey == null || licenceKey.trim().isEmpty()) {
            return LicenceRules.error(LicenceRules.MESSAGE_NOT_CONFIGURED);
        }
        String key = licenceKey.trim();
        String apiUrl = LicenceConfig.resolveLicenceUrl(licenceInternalKey);

        CheckLicenceEntity cached = readCache();
        if (cached != null && !LicenceRules.isCacheUsable(cached, now, key, apiUrl)) {
            deleteCache();
            cached = null;
        }
        if (cached != null && !LicenceRules.shouldRevalidate(cached, checkedOnlineInThisProcess, now)) {
            return success(cached);
        }
        if (cached != null && recentlyFailedOnline(now)) {
            return success(cached);
        }

        LicenceResult online;
        try {
            online = network.checkLicence(apiUrl, key);
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return onlineFailure(cached, now, LicenceRules.error("Erro ao validar licença: " + message));
        }

        if (online == null) {
            return onlineFailure(cached, now, LicenceRules.error("Erro ao validar licença."));
        }

        if (online.isSuccess()) {
            CheckLicenceEntity entity = online.getLicenceEntity();
            if (!LicenceRules.isLicenceUsable(entity, now)) {
                // 200 com licença inutilizável: recusa definitiva.
                return rejected(LicenceRules.error(LicenceRules.unusableMessage(entity, now)));
            }
            // Salva a URL efetiva (sempre oficial), não o valor recebido do app.
            entity.setLicenceInternalKey(apiUrl);
            entity.setSavedLocalDatabaseAt(now);
            try {
                storage.save(entity);
            } catch (Exception ignored) {
            }
            checkedOnlineInThisProcess = true;
            lastOnlineFailureAt = null;
            return online;
        }

        LicenceResult error = online;
        if (error.getErrorMessage() == null || error.getErrorMessage().trim().isEmpty()) {
            error = LicenceRules.error(null);
            error.setRejectedByServer(online.isRejectedByServer());
        }
        if (error.isRejectedByServer()) {
            return rejected(error);
        }
        return onlineFailure(cached, now, error);
    }

    /** Recusa definitiva do servidor: o cache não pode mais liberar o uso. */
    private LicenceResult rejected(LicenceResult error) {
        deleteCache();
        checkedOnlineInThisProcess = true;
        lastOnlineFailureAt = null;
        error.setRejectedByServer(true);
        return error;
    }

    /** Servidor inacessível: usa o cache válido (até 72h) ou bloqueia se não houver. */
    private LicenceResult onlineFailure(CheckLicenceEntity cached, Date now, LicenceResult error) {
        lastOnlineFailureAt = now;
        if (cached != null) {
            return success(cached);
        }
        return error;
    }

    private boolean recentlyFailedOnline(Date now) {
        Date failedAt = lastOnlineFailureAt;
        if (failedAt == null || now == null) {
            return false;
        }
        long elapsed = now.getTime() - failedAt.getTime();
        return elapsed >= 0 && elapsed < LicenceRules.RETRY_AFTER_FAILURE_MILLIS;
    }

    private static LicenceResult success(CheckLicenceEntity entity) {
        LicenceResult result = new LicenceResult();
        result.setMethod("licence");
        result.setResult(LicenceRules.RESULT_SUCCESS);
        result.setLicenceEntity(entity);
        return result;
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
