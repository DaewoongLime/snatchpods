package com.example.client_android.data

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import java.util.UUID

// Mac Server.js에 설정한 UUID들
val SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab")
val CHAR_STATUS_UUID = UUID.fromString("0000cccc-0000-1000-8000-00805f9b34fb") // 읽기/알림
val CHAR_COMMAND_UUID = UUID.fromString("0000bbbb-0000-1000-8000-00805f9b34fb") // 쓰기

class BleRepository(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private var bluetoothGatt: BluetoothGatt? = null

    // UI에 상태를 알려주기 위한 콜백 함수 (람다)
    var onStatusChange: ((String) -> Unit)? = null

    // 1. 스캔 및 연결 시작
    @SuppressLint("MissingPermission")
    fun startScanning() {
        Log.d("BLE", "스캔 시작...")
        val scanner = bluetoothAdapter?.bluetoothLeScanner

        // 특정 UUID(우리 서비스)만 필터링해서 스캔
        val filter = android.bluetooth.le.ScanFilter.Builder()
            .setServiceUuid(android.os.ParcelUuid(SERVICE_UUID))
            .build()

        val settings = android.bluetooth.le.ScanSettings.Builder()
            .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner?.startScan(listOf(filter), settings, object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                Log.d("BLE", "Mac 발견! 연결 시도: ${result.device.address}")
                scanner.stopScan(this) // 찾았으면 스캔 중지 (배터리 절약)
                connectToDevice(result.device)
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        // GATT 서버(Mac)에 연결
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    // 2. GATT 이벤트 처리 (핵심 로직)
    private val gattCallback = object : BluetoothGattCallback() {

        // 연결 상태 변경 시 (Connected / Disconnected)
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BLE", "GATT 연결 성공. 서비스 탐색 시작.")
                gatt.discoverServices() // 서비스 목록 가져오기
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BLE", "연결 끊김")
                onStatusChange?.invoke("Disconnected")
            }
        }

        // 서비스 발견 시
        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                val statusChar = service?.getCharacteristic(CHAR_STATUS_UUID)

                // 알림(Notification) 구독 설정
                if (statusChar != null) {
                    gatt.setCharacteristicNotification(statusChar, true)

                    // CCCD Descriptor 설정 (안드로이드 필수 절차)
                    val descriptor = statusChar.getDescriptor(
                        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                    )
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                    Log.d("BLE", "상태 알림 구독 완료")
                }
            }
        }

        // 데이터 수신 (Mac -> Android) : 여기가 "상태 변화" 받는 곳
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == CHAR_STATUS_UUID) {
                // Node.js가 보낸 0 또는 1 값을 읽음
                val value = characteristic.value[0].toInt()
                val statusText = if (value == 1) "BUSY (스피커 사용중)" else "FREE (가져오기 가능)"

                Log.d("BLE", "상태 변경 수신: $statusText")
                // 메인 스레드로 전달해야 UI 갱신 가능 (여기선 그냥 텍스트만 보냄)
                onStatusChange?.invoke(statusText)
            }
        }
    }

    // 3. 명령 전송 (Android -> Mac)
    @SuppressLint("MissingPermission")
    fun sendConnectCommand() {
        val service = bluetoothGatt?.getService(SERVICE_UUID)
        val commandChar = service?.getCharacteristic(CHAR_COMMAND_UUID)

        if (commandChar != null) {
            commandChar.value = byteArrayOf(0x01) // 0x01 명령 전송
            bluetoothGatt?.writeCharacteristic(commandChar)
            Log.d("BLE", "연결 명령 전송함!")
        }
    }
}