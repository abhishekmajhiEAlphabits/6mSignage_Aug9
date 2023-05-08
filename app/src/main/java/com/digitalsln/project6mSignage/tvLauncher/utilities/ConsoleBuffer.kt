package com.digitalsln.project6mSignage.tvLauncher.utilities

import com.digitalsln.project6mSignage.MainActivity

class ConsoleBuffer(bufferSize: Int) {
    private var buffer: CharArray
    private var amountPopulated: Int

    init {
        buffer = CharArray(bufferSize)
        amountPopulated = 0
    }

    @Synchronized
    fun append(asciiData: ByteArray, offset: Int, length: Int) {
        if (amountPopulated + length > buffer.size) {
            /* Move the old data backwards */
            System.arraycopy(
                buffer,
                length,
                buffer,
                0,
                amountPopulated - length
            )
            amountPopulated -= length
        }
        for (i in 0 until length) {
            buffer[amountPopulated++] = Char(asciiData[offset + i].toUShort())
        }
    }

    @Synchronized
    fun updateTextView(listener: MainActivity.CommandSuccess) {
        if (String(buffer).contains("Package com.google.android.tvlauncher new state: disabled-user")) {
            listener.onSuccess()
        }else if(String(buffer).contains("enable")){
            listener.onHomePressed()
        }
    }
}