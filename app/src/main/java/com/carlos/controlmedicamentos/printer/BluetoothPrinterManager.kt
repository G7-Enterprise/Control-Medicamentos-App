package com.carlos.controlmedicamentos.printer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class BluetoothPrinterManager(private val context: Context) {

    companion object {
        // UUID estándar SPP (Serial Port Profile) para impresoras térmicas
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var currentSocket: BluetoothSocket? = null

    private val bluetoothManager: BluetoothManager? by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager?.adapter
    }

    fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    @Suppress("DEPRECATION")
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasBluetoothPermission()) return emptyList()
        val adapter = bluetoothAdapter ?: return emptyList()
        return try {
            adapter.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    suspend fun connect(address: String): Result<BluetoothSocket> = withContext(Dispatchers.IO) {
        try {
            if (!hasBluetoothPermission()) {
                return@withContext Result.failure(SecurityException("Faltan permisos de Bluetooth"))
            }
            val adapter = bluetoothAdapter
                ?: return@withContext Result.failure(IllegalStateException("Bluetooth no disponible"))

            val device = adapter.getRemoteDevice(address)
                ?: return@withContext Result.failure(IllegalArgumentException("Dispositivo no encontrado"))

            disconnect()

            val socket = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
            } else {
                device.createRfcommSocketToServiceRecord(SPP_UUID)
            }

            socket.connect()
            currentSocket = socket
            Result.success(socket)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: SecurityException) {
            Result.failure(e)
        }
    }

    suspend fun print(data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val socket = currentSocket
                ?: return@withContext Result.failure(IllegalStateException("No hay conexión activa con la impresora"))
            if (!socket.isConnected) {
                return@withContext Result.failure(IOException("Socket desconectado"))
            }
            socket.outputStream?.write(data)
            socket.outputStream?.flush()
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    fun disconnect() {
        try {
            currentSocket?.outputStream?.close()
            currentSocket?.inputStream?.close()
            currentSocket?.close()
        } catch (_: IOException) {
        } finally {
            currentSocket = null
        }
    }

    suspend fun printAndDisconnect(data: ByteArray, address: String): Result<Unit> = withContext(Dispatchers.IO) {
        val connectResult = connect(address)
        if (connectResult.isFailure) {
            return@withContext Result.failure(connectResult.exceptionOrNull() ?: Exception("Error de conexión"))
        }
        val printResult = print(data)
        // Dar tiempo a la impresora antes de cerrar
        try { Thread.sleep(500) } catch (_: InterruptedException) { }
        disconnect()
        return@withContext printResult
    }
}
