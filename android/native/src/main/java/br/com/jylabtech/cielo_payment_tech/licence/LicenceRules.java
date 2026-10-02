package br.com.jylabtech.cielo_payment_tech.licence;

import org.json.JSONObject;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Regras de validação de licença, centralizadas e sem dependências do Android
 * (testáveis na JVM).
 */
public final class LicenceRules {

    public static final int RESULT_SUCCESS = 0;
    public static final int RESULT_ERROR = 999999;

    /** Tempo máximo de uso offline do cache sem nova consulta ao servidor. */
    public static final long MAX_CACHE_AGE_MILLIS = TimeUnit.DAYS.toMillis(7);
    /** Tolerância para um cache salvo "no futuro" (relógio do aparelho adulterado). */
    public static final long FUTURE_TOLERANCE_MILLIS = TimeUnit.MINUTES.toMillis(5);

    public static final String MESSAGE_NOT_CONFIGURED =
            "Licença não configurada. Informe licenceKey ao chamar initPayment.";

    private LicenceRules() {
    }

    /** A licença só libera o uso se tiver validade futura e não estiver vencida, inativa ou cancelada. */
    public static boolean isLicenceUsable(CheckLicenceEntity entity, Date now) {
        return entity != null
                && now != null
                && entity.getExpiredAt() != null
                && entity.getExpiredAt().after(now)
                && !entity.isExpired()
                && !entity.isInactive()
                && !entity.isCanceled();
    }

    /**
     * O cache offline só vale se a licença for utilizável, tiver sido salvo há no máximo
     * 7 dias (e não no futuro), for da mesma chave e da mesma URL de validação.
     */
    public static boolean isCacheUsable(CheckLicenceEntity cache, Date now, String licenceKey, String resolvedLicenceUrl) {
        if (!isLicenceUsable(cache, now)) {
            return false;
        }
        Date savedAt = cache.getSavedLocalDatabaseAt();
        if (savedAt == null) {
            return false;
        }
        long age = now.getTime() - savedAt.getTime();
        if (age > MAX_CACHE_AGE_MILLIS) {
            return false;
        }
        if (age < -FUTURE_TOLERANCE_MILLIS) {
            return false;
        }
        if (licenceKey == null || !licenceKey.equals(cache.getLicenceEncrypt())) {
            return false;
        }
        String savedSource = cache.getLicenceInternalKey();
        // Cache salvo com valor fora do servidor oficial (ex.: URL antiga do Cloud Run) não vale:
        // precisa ser revalidado online no core.
        if (!LicenceConfig.isOfficialLicenceInternalKey(savedSource)) {
            return false;
        }
        return LicenceConfig.resolveLicenceUrl(savedSource).equals(resolvedLicenceUrl);
    }

    public static LicenceResult fromHttpResponse(int statusCode, String body) {
        return fromHttpResponse(statusCode, body, new Date());
    }

    /**
     * Converte a resposta HTTP do servidor de licença em resultado. Só um 200 com uma
     * licença utilizável vira sucesso; qualquer outro caso é erro com mensagem útil.
     */
    public static LicenceResult fromHttpResponse(int statusCode, String body, Date now) {
        if (statusCode != 200) {
            return error(httpErrorMessage(statusCode, body));
        }
        CheckLicenceEntity entity = CheckLicenceEntity.fromJson(body);
        if (entity == null) {
            return error("Resposta inválida do servidor de licença (HTTP 200).");
        }
        if (entity.getExpiredAt() == null) {
            return error("Resposta do servidor de licença sem data de validade.");
        }
        if (!isLicenceUsable(entity, now)) {
            return error(unusableMessage(entity, now));
        }
        LicenceResult result = new LicenceResult();
        result.setMethod("licence");
        result.setResult(RESULT_SUCCESS);
        result.setLicenceEntity(entity);
        result.setMessage("Licença ativada");
        return result;
    }

    public static String unusableMessage(CheckLicenceEntity entity, Date now) {
        if (entity == null || entity.getExpiredAt() == null) {
            return "Licença inválida.";
        }
        if (entity.isCanceled()) {
            return "Licença cancelada.";
        }
        if (entity.isInactive()) {
            return "Licença inativa ou suspensa.";
        }
        if (entity.isExpired() || !entity.getExpiredAt().after(now)) {
            return "Licença vencida.";
        }
        return "Licença inválida.";
    }

    static String httpErrorMessage(int statusCode, String body) {
        String fallback = "Erro ao validar licença (HTTP " + statusCode + ")";
        if (body == null || body.trim().isEmpty()) {
            return fallback;
        }
        try {
            JSONObject json = new JSONObject(body);
            if (json.isNull("message")) {
                return fallback;
            }
            String message = json.optString("message", "").trim();
            return message.isEmpty() ? fallback : message;
        } catch (Exception e) {
            return fallback;
        }
    }

    public static LicenceResult error(String message) {
        LicenceResult result = new LicenceResult();
        result.setMethod("licence");
        result.setResult(RESULT_ERROR);
        result.setErrorMessage(message == null || message.trim().isEmpty() ? "Sua licença não foi ativada." : message);
        return result;
    }
}
