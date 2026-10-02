package br.com.jylabtech.cielo_payment_tech.licence;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * Cache da licença em EncryptedSharedPreferences (mesmo arquivo e chave das versões anteriores).
 */
public class EncryptedLicenceStorage implements LicenceStorage {

    private static final String PREFS_NAME = "cielo_licence_prefs_encrypted";
    private static final String KEY_JSON = "licence_entity";

    private final Context context;

    public EncryptedLicenceStorage(Context context) {
        this.context = context;
    }

    private SharedPreferences prefs() throws Exception {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    @Override
    public CheckLicenceEntity read() {
        try {
            String savedJson = prefs().getString(KEY_JSON, null);
            if (savedJson != null) {
                return CheckLicenceEntity.fromJson(savedJson);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public void save(CheckLicenceEntity entity) {
        try {
            String json = entity.toJson();
            if (json != null) {
                prefs().edit().putString(KEY_JSON, json).apply();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void delete() {
        try {
            prefs().edit().remove(KEY_JSON).apply();
        } catch (Exception ignored) {
        }
    }
}
