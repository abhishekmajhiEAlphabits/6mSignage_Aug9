package com.digitalsln.project6mSignage.tvLauncher.utilities

import android.app.Service
import android.util.Log
import com.cgutman.adblib.AdbCrypto
import com.digitalsln.project6mSignage.tvLauncher.utilities.AdbUtils
import com.digitalsln.project6mSignage.tvLauncher.utilities.ConsoleBuffer
import com.digitalsln.project6mSignage.tvLauncher.utilities.DeviceConnection
import com.digitalsln.project6mSignage.tvLauncher.utilities.DeviceConnectionListener
import java.util.*

class ShellListener(val shellService: Service): DeviceConnectionListener {

    private val TERM_LENGTH = 25000

    private val listenerMap: HashMap<DeviceConnection?, LinkedList<DeviceConnectionListener?>?> =
        HashMap()
//    private val consoleMap: ConcurrentHashMap<DeviceConnection?, ConsoleBuffer> =
//        ConcurrentHashMap<DeviceConnection?, ConsoleBuffer>()

    fun addListener(conn: DeviceConnection?, listener: DeviceConnectionListener?) {
        synchronized(listenerMap) {
            var listeners: LinkedList<DeviceConnectionListener?>? = listenerMap[conn]
            if (listeners != null) {
                listeners.add(listener)
            } else {
                listeners = LinkedList<DeviceConnectionListener?>()
                listeners.add(listener)
                listenerMap.put(conn, listeners)
            }
        }

        /* If the listener supports console input, we'll tell them about the console buffer
          * by firing them an initial console updated callback */
//        val console: ConsoleBuffer = consoleMap[conn]!!
//        if (listener?.isConsole() == true) {
//            listener.consoleUpdated(conn, console)
//        }
    }

    fun removeListener(conn: DeviceConnection?, listener: DeviceConnectionListener?) {
        synchronized(listenerMap) {
            val listeners: LinkedList<DeviceConnectionListener?>? = listenerMap[conn]
            listeners?.remove(listener)
        }
    }

    override fun notifyConnectionEstablished(devConn: DeviceConnection?) {
        synchronized(listenerMap) {
            val listeners: LinkedList<DeviceConnectionListener?>? = listenerMap[devConn]
            if (listeners != null) {
                for (listener in listeners) {
                    listener?.notifyConnectionEstablished(devConn)
                }
            }
        }
    }

    override fun notifyConnectionFailed(devConn: DeviceConnection?, e: Exception?) {
        synchronized(listenerMap) {
            val listeners: LinkedList<DeviceConnectionListener?>? = listenerMap[devConn]
            if (listeners != null) {
                for (listener in listeners) {
                    listener?.notifyConnectionFailed(devConn, e)
                }
            }
        }
    }

    override fun notifyStreamFailed(devConn: DeviceConnection?, e: Exception?) {
        /* Return if this connection has already "failed" */
//        if (consoleMap.remove(devConn) == null) {
//            return
//        }
        synchronized(listenerMap) {
            val listeners: LinkedList<DeviceConnectionListener?>? = listenerMap[devConn]
            if (listeners != null) {
                for (listener in listeners) {
                    listener?.notifyStreamFailed(devConn, e)
                }
            }
        }
    }

    override fun notifyStreamClosed(devConn: DeviceConnection?) {
        /* Return if this connection has already "failed" */
//        if (consoleMap.remove(devConn) == null) {
//            return
//        }
        synchronized(listenerMap) {
            val listeners: LinkedList<DeviceConnectionListener?>? = listenerMap[devConn]
            if (listeners != null) {
                for (listener in listeners) {
                    listener?.notifyStreamClosed(devConn)
                }
            }
        }
    }

    override fun loadAdbCrypto(devConn: DeviceConnection?): AdbCrypto? {
        Log.v("Shell Listener====", "loadAdbCrypto")

        return AdbUtils.readCryptoConfig(shellService.filesDir)
    }

    override fun receivedData(
        devConn: DeviceConnection?,
        data: ByteArray?,
        offset: Int,
        length: Int
    ) {
        /* Add data to the console for this connection */
        var length = length
//        consoleMap.clear()
        val console = ConsoleBuffer(TERM_LENGTH)
//        val console: ConsoleBuffer? = consoleMap[devConn]
        if (console != null && data!=null) {
            /* Hack to remove the bell from the end of the prompt */
            if (data[offset + length - 1].toInt() == 0x07) {
                length--
            }
            console.append(data, offset, length)

            /* Attempt to deliver a console update notification */synchronized(listenerMap) {
                val listeners: LinkedList<DeviceConnectionListener?>? = listenerMap[devConn]
                if (listeners != null) {
                    for (listener in listeners) {
                        if (listener?.isConsole() == true) {
                            listener.consoleUpdated(devConn, console)
                        }
                    }
                }
            }
        }
    }

    override fun canReceiveData(): Boolean {
        /* We can always receive data */
        return true
    }

    override fun isConsole(): Boolean {
        return false
    }

    override fun consoleUpdated(
        devConsole: DeviceConnection?,
        console: ConsoleBuffer?
    ) {
    }
}