package com.example.client_android.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.client_android.data.NetworkModule
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    // UI 상태 (성공/실패 메시지 보여주기용)
    var statusMessage by mutableStateOf("대기 중...")
        private set

    fun connectAirPods() {
        performRequest("연결 시도 중...") {
            val response = NetworkModule.api.connect()
            "성공: $response"
        }
    }

    fun disconnectAirPods() {
        performRequest("해제 시도 중...") {
            val response = NetworkModule.api.disconnect()
            "성공: $response"
        }
    }

    private fun performRequest(loadingMsg: String, request: suspend () -> String) {
        viewModelScope.launch {
            statusMessage = loadingMsg
            try {
                statusMessage = request()
            } catch (e: Exception) {
                statusMessage = "에러: ${e.message}"
                e.printStackTrace()
            }
        }
    }
}