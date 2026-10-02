package br.com.jylabtech.cielo_payment_tech.licence;

/**
 * Consulta da licença no servidor. Implementação real: {@link LicenceProvider}.
 * Chamada síncrona: deve rodar fora da main thread.
 */
public interface LicenceNetwork {
    LicenceResult checkLicence(String url, String licenceKey) throws Exception;
}
