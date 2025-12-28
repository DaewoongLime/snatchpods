package com.example.client_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.client_android.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AirPodsControllerScreen()
            }
        }
    }
}

@Composable
fun AirPodsControllerScreen(viewModel: MainViewModel = viewModel()) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "AirPods Switcher",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 상태 메시지 표시
        Text(text = viewModel.statusMessage)

        Spacer(modifier = Modifier.height(32.dp))

        // 연결 버튼
        Button(
            onClick = { viewModel.connectAirPods() },
            modifier = Modifier.fillMaxWidth(0.6f).height(50.dp)
        ) {
            Text("Mac으로 가져오기 (Connect)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 해제 버튼
        Button(
            onClick = { viewModel.disconnectAirPods() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth(0.6f).height(50.dp)
        ) {
            Text("연결 끊기 (Disconnect)")
        }
    }
}