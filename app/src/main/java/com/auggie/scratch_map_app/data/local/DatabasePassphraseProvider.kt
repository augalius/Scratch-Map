package com.auggie.scratch_map_app.data.local

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supplies the SQLCipher passphrase for the local database.
 *
 * A 256-bit key is generated once with [SecureRandom] and kept in [EncryptedSharedPreferences],
 * itself protected by an Android Keystore master key. Extracted from the Hilt module so this
 * security-critical code sits somewhere discoverable and testable rather than inside DI wiring.
 */
@Singleton
class DatabasePassphraseProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getOrCreate(): ByteArray {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPrefs = EncryptedSharedPreferences.create(
            PREFS_NAME, masterKeyAlias, context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        sharedPrefs.getString(KEY_PASSPHRASE, null)?.let { return it.toByteArray() }

        val bytes = ByteArray(KEY_SIZE_BYTES)
        SecureRandom().nextBytes(bytes)
        val passphrase = Base64.encodeToString(bytes, Base64.NO_WRAP)
        sharedPrefs.edit().putString(KEY_PASSPHRASE, passphrase).apply()
        return passphrase.toByteArray()
    }

    private companion object {
        const val PREFS_NAME = "scratchmap_secure_prefs"
        const val KEY_PASSPHRASE = "db_passphrase"
        const val KEY_SIZE_BYTES = 32
    }
}
