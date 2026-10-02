package br.com.jylabtech.cielo_payment_tech.licence;

/**
 * Cache local da licença validada. Implementação real: {@link EncryptedLicenceStorage}.
 */
public interface LicenceStorage {
    CheckLicenceEntity read();

    void save(CheckLicenceEntity entity);

    void delete();
}
