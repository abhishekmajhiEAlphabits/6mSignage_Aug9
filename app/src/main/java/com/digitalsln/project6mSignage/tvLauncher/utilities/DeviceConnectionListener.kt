package com.digitalsln.project6mSignage.tvLauncher.utilities

import com.cgutman.adblib.AdbCrypto
import com.digitalsln.project6mSignage.tvLauncher.utilities.ConsoleBuffer
import com.digitalsln.project6mSignage.tvLauncher.utilities.DeviceConnection

interface DeviceConnectionListener {
    fun notifyConnectionEstablished(devConn: DeviceConnection?)

    fun notifyConnectionFailed(devConn: DeviceConnection?, e: Exception?)

    fun notifyStreamFailed(devConn: DeviceConnection?, e: Exception?)

    fun notifyStreamClosed(devConn: DeviceConnection?)

    fun loadAdbCrypto(devConn: DeviceConnection?): AdbCrypto?

    fun canReceiveData(): Boolean

    fun receivedData(devConn: DeviceConnection?, data: ByteArray?, offset: Int, length: Int)

    fun isConsole(): Boolean

    fun consoleUpdated(devConn: DeviceConnection?, console: ConsoleBuffer?)
}