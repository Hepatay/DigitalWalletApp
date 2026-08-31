package com.epatay.digitalwallet.sync

import androidx.annotation.Keep

/**
 * Firestore bulut veritabanında saklanan Uçtan Uca Şifreli (Zero-Knowledge) Doküman Modeli.
 *
 * uuid, user_id, updated_at ve is_deleted alanları doküman eşleme, sorgulama ve
 * çakışma çözümü (conflict resolution) için kullanılır.
 * Geriye kalan TÜM hassas veriler (tutar, not, başlık, miktar, fiyat vb.)
 * `payload` alanında AES-256-GCM ile şifrelenmiş olarak tutulur.
 */
@Keep
data class EncryptedCloudRecord(
    val uuid: String = "",
    val user_id: String = "",
    val updated_at: Long = 0L,
    val is_deleted: Boolean = false,
    val payload: String = "",
    val encrypted: Boolean = true,
    val version: Int = 1
)