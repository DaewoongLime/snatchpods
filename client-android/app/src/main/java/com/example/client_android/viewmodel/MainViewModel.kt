package com.example.client_android.viewmodel

import com.example.client_android.data.BleRepository

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

// Context를 써야 해서 AndroidViewModel로 상속 변경
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val bleRepository = BleRepository(application)

    // UI 상태 변수들
    var statusMessage by mutableStateOf("블루투스 스캔 대기 중...")
        private set

    var isMacBusy by mutableStateOf(false) // 버튼 잠금용 (True면 잠김)
        private set

    init {
        // BLE 상태 변경 콜백 연결
        bleRepository.onStatusChange = { statusText ->
            viewModelScope.launch {
                statusMessage = statusText
                // "BUSY"라는 단어가 포함되어 있으면 버튼 잠그기
                isMacBusy = statusText.contains("BUSY")
            }
        }
    }

    fun startBleScan() {
        statusMessage = "Mac 찾는 중..."
        bleRepository.startScanning()
    }

    fun connectAirPods() {
        bleRepository.sendConnectCommand()
        statusMessage = "명령 전송 완료!"
    }
}