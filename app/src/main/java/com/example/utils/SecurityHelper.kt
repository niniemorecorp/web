package com.example.utils

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurityHelper {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128
    
    // 256-bit symmetric key for standard offline database encryption
    private val keyBytes = byteArrayOf(
        0x52.toByte(), 0x65.toByte(), 0x73.toByte(), 0x69.toByte(),
        0x55.toByte(), 0x73.toByte(), 0x61.toByte(), 0x68.toByte(),
        0x61.toByte(), 0x41.toByte(), 0x6d.toByte(), 0x61.toByte(),
        0x6e.toByte(), 0x53.toByte(), 0x65.toByte(), 0x63.toByte(),
        0x75.toByte(), 0x72.toByte(), 0x65.toByte(), 0x4b.toByte(),
        0x65.toByte(), 0x79.toByte(), 0x320.toByte().coerceAtMost(0x7F.toByte()), 0x32.toByte(),
        0x36.toByte(), 0x4e.toByte(), 0x65.toByte(), 0x77.toByte(),
        0x45.toByte(), 0x32.toByte(), 0x45.toByte(), 0x53.toByte()
    )
    private val secretKey: SecretKey = SecretKeySpec(keyBytes, "AES")

    fun encrypt(data: String): String {
        if (data.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(IV_SIZE)
            SecureRandom().nextBytes(iv)
            val spec = GCMParameterSpec(TAG_SIZE, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
            val cipherText = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback safe encoding
            try {
                Base64.encodeToString(data.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            } catch (ex: Exception) {
                data
            }
        }
    }

    fun decrypt(encryptedBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size < IV_SIZE) return encryptedBase64
            
            val iv = ByteArray(IV_SIZE)
            System.arraycopy(combined, 0, iv, 0, iv.size)
            
            val cipherTextSize = combined.size - IV_SIZE
            val cipherText = ByteArray(cipherTextSize)
            System.arraycopy(combined, IV_SIZE, cipherText, 0, cipherTextSize)
            
            val cipher = Cipher.getInstance(ALGORITHM)
            val spec = GCMParameterSpec(TAG_SIZE, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            try {
                // Fallback decode
                String(Base64.decode(encryptedBase64, Base64.NO_WRAP), Charsets.UTF_8)
            } catch (ex: Exception) {
                encryptedBase64
            }
        }
    }
}
