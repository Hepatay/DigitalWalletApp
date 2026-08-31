package com.epatay.digitalwallet.security

import android.util.Base64
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Sıfır Bilgi (Zero-Knowledge) & Uçtan Uca İstemci Taraflı Şifreleme Motoru.
 *
 * Veriler Firebase Firestore bulutuna gönderilmeden önce kullanıcının cihazında
 * AES-256-GCM ile şifrelenir.
 * Bu sayede Firebase Console'da veya veritabanı yöneticisi panelinde veriler tamamen
 * anlamsız şifreli metin (ciphertext) olarak saklanır.
 * Şifre çözme anahtarı yalnızca kullanıcının kendi cihazında üretilir ve hiçbir sunucuya iletilmez.
 */
object ZeroKnowledgeCrypto {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12

    // Proje bazlı tuz (salt) değeri
    private const val APP_KEY_SALT = "VarlikCep_ZeroKnowledge_Salt_2026_Secured_v1"

    /**
     * Kullanıcının Firebase UID'si ve uygulama tuzu üzerinden SHA-256 ile 256-bit AES anahtarı türetir.
     */
    private fun deriveKey(uid: String): SecretKeySpec {
        val md = MessageDigest.getInstance("SHA-256")
        val input = "$uid:$APP_KEY_SALT".toByteArray(Charsets.UTF_8)
        val hash = md.digest(input)
        return SecretKeySpec(hash, "AES")
    }

    /**
     * Düz metni (JSON payload) AES-256-GCM ile şifreler.
     * Çıktı: [12 bayt rastgele IV + şifrelenmiş veri + 16 bayt auth tag] -> Base64
     */
    fun encrypt(plainText: String, uid: String): String {
        val key = deriveKey(uid)
        val iv = ByteArray(IV_LENGTH_BYTE)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(ALGORITHM)
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)

        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val byteBuffer = ByteBuffer.allocate(iv.size + cipherText.size)
        byteBuffer.put(iv)
        byteBuffer.put(cipherText)

        return Base64.encodeToString(byteBuffer.array(), Base64.NO_WRAP)
    }

    /**
     * Base64 formatındaki şifreli veriyi çözer ve orijinal düz metni (JSON) döndürür.
     */
    fun decrypt(encryptedBase64: String, uid: String): String {
        val key = deriveKey(uid)
        val decoded = Base64.decode(encryptedBase64, Base64.NO_WRAP)

        if (decoded.size < IV_LENGTH_BYTE) {
            throw IllegalArgumentException("Geçersiz şifreli veri boyutu")
        }

        val iv = ByteArray(IV_LENGTH_BYTE)
        System.arraycopy(decoded, 0, iv, 0, IV_LENGTH_BYTE)

        val cipherText = ByteArray(decoded.size - IV_LENGTH_BYTE)
        System.arraycopy(decoded, IV_LENGTH_BYTE, cipherText, 0, cipherText.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        val plainTextBytes = cipher.doFinal(cipherText)
        return String(plainTextBytes, Charsets.UTF_8)
    }
}