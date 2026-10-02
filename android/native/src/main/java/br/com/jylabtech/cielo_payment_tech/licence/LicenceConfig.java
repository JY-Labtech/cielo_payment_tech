package br.com.jylabtech.cielo_payment_tech.licence;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Configuração da validação de licença.
 *
 * A URL de validação é travada no servidor oficial da JY Labtech (jylabtech_core).
 * O {@code licenceInternalKey} informado pelo app só é aceito se apontar para esse
 * servidor; qualquer outro valor é ignorado e a URL padrão é usada. Assim nenhum app
 * consegue apontar a validação para um servidor próprio.
 *
 * Classe pura (sem dependências do Android) para poder ser testada na JVM.
 */
public final class LicenceConfig {

    public static final String LICENCE_HOST = "licence-server.invalid";
    public static final String LICENCE_PATH_PREFIX = "/pos-payments/licence/check";
    public static final String LICENCE_CHECK_URL = "https://" + LICENCE_HOST + LICENCE_PATH_PREFIX;

    private LicenceConfig() {
    }

    /**
     * Devolve a URL efetiva de validação. Aceita o {@code licenceInternalKey} (URL em
     * texto ou em base64) apenas se for {@code https://licence-server.invalid/pos-payments/licence/check...};
     * em qualquer outro caso devolve {@link #LICENCE_CHECK_URL}.
     */
    public static String resolveLicenceUrl(String licenceInternalKey) {
        String candidate = decodeLicenceInternalKey(licenceInternalKey);
        if (isOfficialLicenceUrl(candidate)) {
            return candidate.trim();
        }
        return LICENCE_CHECK_URL;
    }

    /** true se a URL é https, com host exatamente o servidor oficial e path de validação. */
    public static boolean isOfficialLicenceUrl(String url) {
        return isAcceptedUrl(url, LICENCE_HOST);
    }

    /**
     * true se o {@code licenceInternalKey} (texto ou base64) aponta para o servidor oficial.
     * Usado para o cache: um cache salvo com valor fora do servidor oficial (inclusive a URL
     * antiga do Cloud Run) não vale e precisa ser revalidado online.
     */
    public static boolean isOfficialLicenceInternalKey(String licenceInternalKey) {
        return isOfficialLicenceUrl(decodeLicenceInternalKey(licenceInternalKey));
    }

    static String decodeLicenceInternalKey(String licenceInternalKey) {
        if (licenceInternalKey == null) {
            return null;
        }
        String value = licenceInternalKey.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.contains("http")) {
            return value;
        }
        return decodeBase64(value);
    }

    private static boolean isAcceptedUrl(String url, String expectedHost) {
        if (url == null) {
            return false;
        }
        try {
            URI uri = new URI(url.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return false;
            }
            if (uri.getRawUserInfo() != null) {
                return false;
            }
            String host = uri.getHost();
            if (host == null || !host.equalsIgnoreCase(expectedHost)) {
                return false;
            }
            if (uri.getPort() != -1 && uri.getPort() != 443) {
                return false;
            }
            if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
                return false;
            }
            String path = uri.getRawPath();
            if (path == null || !path.equals(uri.normalize().getRawPath()) || path.contains("%")) {
                return false;
            }
            return path.equals(LICENCE_PATH_PREFIX) || path.startsWith(LICENCE_PATH_PREFIX + "/");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Decodificador base64 (padrão e url-safe) sem dependências do Android.
     * {@code java.util.Base64} só existe a partir da API 26 e o plugin suporta a API 24.
     * Devolve null se o valor não for base64 válido.
     */
    static String decodeBase64(String input) {
        StringBuilder clean = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (!Character.isWhitespace(c)) {
                clean.append(c);
            }
        }
        String s = clean.toString();
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '=') {
            end--;
        }
        if (s.length() - end > 2) {
            return null;
        }
        if (end == 0 || end % 4 == 1) {
            return null;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0;
        int bits = 0;
        for (int i = 0; i < end; i++) {
            int value = base64Value(s.charAt(i));
            if (value < 0) {
                return null;
            }
            buffer = (buffer << 6) | value;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out.write((buffer >> bits) & 0xFF);
                buffer &= (1 << bits) - 1;
            }
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static int base64Value(char c) {
        if (c >= 'A' && c <= 'Z') return c - 'A';
        if (c >= 'a' && c <= 'z') return c - 'a' + 26;
        if (c >= '0' && c <= '9') return c - '0' + 52;
        if (c == '+' || c == '-') return 62;
        if (c == '/' || c == '_') return 63;
        return -1;
    }
}
