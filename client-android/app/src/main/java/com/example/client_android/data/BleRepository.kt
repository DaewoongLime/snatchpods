package com.example.client_android.data

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.util.Log
import java.util.UUID

// Mac Server.js와 동일한 UUID
val SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab")
val CHAR_STATUS_UUID = UUID.fromString("0000cccc-0000-1000-8000-00805f9b34fb")
val CHAR_COMMAND_UUID = UUID.fromString("0000bbbb-0000-1000-8000-00805f9b34fb")

class BleRepository(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private var bluetoothGatt: BluetoothGatt? = null
    var onStatusChange: ((String) -> Unit)? = null

    @SuppressLint("MissingPermission")
    fun startScanning() {
        Log.d("BLE", "스캔 시작...")
        val scanner = bluetoothAdapter?.bluetoothLeScanner

        // 1. 기존 필터 코드 삭제 (이게 있으면 Mac을 못 찾을 수 있음)
        /* val filter = android.bluetooth.le.ScanFilter.Builder()
            .setServiceUuid(android.os.ParcelUuid(SERVICE_UUID))
            .build()
        */

        val settings = android.bluetooth.le.ScanSettings.Builder()
            .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        // 2. 필터 자리에 'null' 입력 -> 모든 기기 스캔
        scanner?.startScan(null, settings, object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                // 3. 이름이나 주소로 우리 Mac인지 확인
                val deviceName = result.device.name
                val deviceAddress = result.device.address

                // 로그로 주변 기기들이 잡히는지 확인해보세요
                // Log.d("BLE", "발견됨: $deviceName ($deviceAddress)")
//                Log.d("BLE", "발견됨: ${result.device.name} (${result.device.address})")

                // 이름이 "AirPods-Manager"인 녀석만 골라내기
                if (deviceName == "AirPods-Manager" || deviceAddress == "YOUR MAC ADDRESS") {
                    Log.d("BLE", "🎯 Mac 발견! 연결 시도: ${result.device.address}")
                    scanner.stopScan(this)
                    connectToDevice(result.device)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e("BLE", "스캔 실패: $errorCode")
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        // autoConnect = false로 해야 더 빠르게 붙습니다.
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BLE", "연결 성공. 서비스 탐색 중...")
                onStatusChange?.invoke("Mac 연결됨! 상태 확인 중...")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BLE", "연결 끊김")
                onStatusChange?.invoke("Disconnected")
                bluetoothGatt?.close()
                bluetoothGatt = null
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                val statusChar = service?.getCharacteristic(CHAR_STATUS_UUID)

                if (statusChar != null) {
                    gatt.setCharacteristicNotification(statusChar, true)

                    val descriptor = statusChar.getDescriptor(
                        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                    )

                    // [수정 포인트 1] 안드로이드 13(API 33) 이상 대응
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    } else {
                        @Suppress("DEPRECATION")
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        @Suppress("DEPRECATION")
                        gatt.writeDescriptor(descriptor)
                    }
                    Log.d("BLE", "알림 구독 요청 보냄")
                }
            }
        }

        // [수정 포인트 2] 안드로이드 13용 콜백 추가 (이게 없으면 데이터 수신 불가)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleCharacteristicChange(characteristic, value)
        }

        // 구버전 호환용 콜백
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            handleCharacteristicChange(characteristic, characteristic.value)
        }

        private fun handleCharacteristicChange(characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            if (characteristic.uuid == CHAR_STATUS_UUID) {
                val statusValue = value[0].toInt()
                val statusText = if (statusValue == 1) "BUSY (스피커 사용중)" else "FREE (가져오기 가능)"
                Log.d("BLE", "상태 수신: $statusText")
                onStatusChange?.invoke(statusText)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun sendConnectCommand() {
        val service = bluetoothGatt?.getService(SERVICE_UUID)
        val commandChar = service?.getCharacteristic(CHAR_COMMAND_UUID)

        if (commandChar != null) {
            val command = byteArrayOf(0x01)

            // [수정 포인트 3] 쓰기 방식 버전 대응
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bluetoothGatt?.writeCharacteristic(commandChar, command, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            } else {
                @Suppress("DEPRECATION")
                commandChar.value = command
                @Suppress("DEPRECATION")
                bluetoothGatt?.writeCharacteristic(commandChar)
            }
            Log.d("BLE", "명령 전송함")
        }
    }
}