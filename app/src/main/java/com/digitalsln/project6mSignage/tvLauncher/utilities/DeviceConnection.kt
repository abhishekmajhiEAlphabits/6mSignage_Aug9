package com.digitalsln.project6mSignage.tvLauncher.utilities

import android.util.Log
import com.cgutman.adblib.AdbConnection

import com.cgutman.adblib.AdbCrypto

import com.cgutman.adblib.AdbStream
import java.io.Closeable
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue

class DeviceConnection(
    private val listener: DeviceConnectionListener?,
    private val host: String?,
    private val port: Int,
    private var foreground: Boolean = true
) : Closeable {

    private val CONN_TIMEOUT = 5000

    private var connection: AdbConnection? = null
    private var shellStream: AdbStream? = null

    private var closed = false

    private val commandQueue: LinkedBlockingQueue<ByteArray> = LinkedBlockingQueue<ByteArray>()

    fun getHost(): String? {
        return host
    }

    fun getPort(): Int {
        return port
    }

    fun queueCommand(command: String): Boolean {
        return try {
            /* Queue it up for sending to the device */
            commandQueue.add(command.toByteArray(charset("UTF-8")))
            true
        } catch (e: UnsupportedEncodingException) {
            false
        }
    }

    fun queueBytes(buffer: ByteArray?): Boolean {
        /* Queue it up for sending to the device */
        commandQueue.add(buffer)
        return true
    }

    fun startConnect() {
        Thread(Runnable {
            var connected = false
            val socket = Socket()

            /* Load the crypto config */
            val crypto: AdbCrypto? =
                listener?.loadAdbCrypto(this@DeviceConnection)

            if(crypto==null) startConnect()

            try {
                /* Establish a connect to the remote host */
                socket.connect(InetSocketAddress(host, port), CONN_TIMEOUT)
            } catch (e: IOException) {
                Log.v("ADB SHELL====", "Socket failed")

                listener?.notifyConnectionFailed(this@DeviceConnection, e)
                return@Runnable
            }
            try {
                /* Establish the application layer connection */
                connection = AdbConnection.create(socket, crypto)
                Log.v("ADB SHELL====", "connecting")

                connection?.connect()
                Log.v("ADB SHELL====", "connected")

                /* Open the shell stream */shellStream = connection?.open("shell:")
                connected = true
                Log.v("ADB SHELL====", "connection established")
            } catch (e: IOException) {
                listener?.notifyConnectionFailed(this@DeviceConnection, e)
            } catch (e: InterruptedException) {
                listener?.notifyConnectionFailed(this@DeviceConnection, e)
            } finally {
                /* Cleanup if the connection failed */
                if (!connected) {
                    safeClose(shellStream)

                    /* The AdbConnection object will close the underlying socket
						 * but we need to close it ourselves if the AdbConnection object
						 * wasn't successfully constructed.
						 */if (!safeClose(connection)) {
                        try {
                            socket.close()
                        } catch (_: IOException) {
                        }
                    }
                }
            }
            Log.v("ADB SHELL====", "Update Listener for establish connection")

            /* Notify the listener that the connection is complete */listener?.notifyConnectionEstablished(
            this@DeviceConnection
        )
            /* Start the receive thread */startReceiveThread()
            /* Enter the blocking send loop */sendLoop()
        }).start()
    }

    private fun safeClose(c: Closeable?): Boolean {
        if (c == null) return false
        try {
            c.close()
        } catch (e: IOException) {
            return false
        }
        return true
    }

    fun sendLoop() {
        /* We become the send thread */
        try {
            while (true) {

            /* Get the next command */
            val command: ByteArray = commandQueue.take()

            /* This may be a close indication */if (shellStream?.isClosed == true) {
                listener!!.notifyStreamClosed(this@DeviceConnection)
                return
            }


            /* Issue it to the device */shellStream!!.write(command)
            }
        } catch (e: IOException) {
            listener!!.notifyStreamFailed(this@DeviceConnection, e)
        } catch (_: InterruptedException) {
        } finally {
            safeClose(this@DeviceConnection)
        }
    }

    private fun startReceiveThread() {
        if(shellStream==null ) return
        Thread {
            try {
                while (!shellStream!!.isClosed) {
                    val data = shellStream!!.read()
                    listener!!.receivedData(this@DeviceConnection, data, 0, data.size)
                }
                listener!!.notifyStreamClosed(this@DeviceConnection)
            } catch (e: IOException) {
                listener!!.notifyStreamFailed(this@DeviceConnection, e)
            } catch (_: InterruptedException) {
            } finally {
                safeClose(this@DeviceConnection)
            }
        }.start()
    }

    fun isClosed(): Boolean {
        return closed
    }

    @Throws(IOException::class)
    override fun close() {
        closed = if (isClosed()) {
            return
        } else {
            true
        }

        /* Close the stream first */safeClose(shellStream)

        /* Now the connection (and underlying socket) */safeClose(connection)

        /* Finally signal the command queue to allow the send thread to terminate */commandQueue.add(
            ByteArray(0)
        )
    }

    fun isForeground(): Boolean {
        return foreground
    }

    fun setForeground(foreground: Boolean) {
        this.foreground = foreground
    }
}