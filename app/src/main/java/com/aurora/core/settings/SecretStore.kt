package com.aurora.core.settings

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Guarda segredos (ex.: chave de API) cifrados com AES-256/GCM; a chave de cifra fica no
 * Android Keystore e nunca sai do hardware/TEE. Sem dependências externas.
 * Se a chave do Keystore for perdida/invalidada, o valor é descartado (get -> null), nunca devolvido corrompido.
 */
class SecretStore(context: Context) {
    private val prefs = context.getSharedPreferences("aurora_secrets", Context.MODE_PRIVATE)

    fun put(name: String, value: String?) {
        if (value.isNullOrBlank()) {
            prefs.edit().remove(name).apply()
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val packed = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        prefs.edit().putString(name, Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
    }

    fun get(name: String): String? {
        val encoded = prefs.getString(name, null) ?: return null
        return try {
            val packed = Base64.decode(encoded, Base64.NO_WRAP)
            val iv = packed.copyOfRange(0, IV_SIZE)
            val data = packed.copyOfRange(IV_SIZE, packed.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(data), Charsets.UTF_8)
        } catch (t: Exception) {
            Log.w(TAG, "Segredo '$name' ilegível; descartado.", t)
            prefs.edit().remove(name).apply()
            null
        }
    }

    fun has(name: String): Boolean = prefs.contains(name)

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        gen.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    companion object {
        const val KEY_REMOTE_API = "remote_api_key"
        private const val TAG = "SecretStore"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "aurora_secret_key_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_BITS = 128
    }
}
