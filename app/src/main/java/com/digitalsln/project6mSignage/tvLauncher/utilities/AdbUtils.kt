package com.digitalsln.project6mSignage.tvLauncher.utilities

import android.util.Log
import com.cgutman.adblib.AdbCrypto
import java.io.Closeable
import java.io.File
import java.io.IOException

object AdbUtils {
    val PUBLIC_KEY_NAME = "public.key"
    val PRIVATE_KEY_NAME = "private.key"

    fun readCryptoConfig(dataDir: File?): AdbCrypto? {
        val pubKey = File(dataDir, PUBLIC_KEY_NAME)
        val privKey = File(dataDir, PRIVATE_KEY_NAME)
        var crypto: AdbCrypto? = null
        if (pubKey.exists() && privKey.exists()) {
            crypto = try {
                AdbCrypto.loadAdbKeyPair(AndroidBase64(), privKey, pubKey)
            } catch (e: Exception) {
                null
            }
        }
        Log.v("readCryptoConfig====", "Loaded")

        return crypto
    }

    fun writeNewCryptoConfig(dataDir: File?): AdbCrypto? {
        val pubKey = File(dataDir, PUBLIC_KEY_NAME)
        val privKey = File(dataDir, PRIVATE_KEY_NAME)
        var crypto: AdbCrypto? = null
        try {
            crypto = AdbCrypto.generateAdbKeyPair(AndroidBase64())
            crypto.saveAdbKeyPair(privKey, pubKey)
        } catch (e: Exception) {
            crypto = null
        }
        Log.v("writeNewCrypto===", "Loaded")

        return crypto
    }

    fun safeClose(c: Closeable?): Boolean {
        if (c == null) return false
        try {
            c.close()
        } catch (e: IOException) {
            return false
        }
        return true
    }

    fun safeAsyncClose(c: Closeable?) {
        if (c == null) return
        Thread {
            try {
                c.close()
            } catch (ignored: IOException) {
            }
        }.start()
    }
}