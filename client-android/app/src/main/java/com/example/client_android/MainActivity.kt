package com.example.client_android

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.client_android.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AirPodsControllerScreen(viewModel)
            }
        }
    }
}

@Composable
fun AirPodsControllerScreen(viewModel: MainViewModel) {
    val context = LocalContext.current

    // 권한 요청 런처 (지옥의 권한 체크를 한 방에 해결)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.values.all { it }
        if (isGranted) {
            viewModel.startBleScan() // 권한 받으면 바로 스캔 시작
        } else {
            Toast.makeText(context, "권한이 없으면 작동 안 함!", Toast.LENGTH_SHORT).show()
        }
    }

    // 앱 켜지자마자 권한 체크 및 요청
    LaunchedEffect(Unit) {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
        permissionLauncher.launch(permissionsToRequest)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "SnatchPods (BLE)",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 상태 메시지 표시
        Text(
            text = viewModel.statusMessage,
            color = if (viewModel.isMacBusy) Color.Red else Color.Green,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 가져오기 버튼 (상태에 따라 잠김/풀림)
        Button(
            onClick = { viewModel.connectAirPods() },
            enabled = !viewModel.isMacBusy, // BUSY면 클릭 불가!
            modifier = Modifier.fillMaxWidth(0.6f).height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (viewModel.isMacBusy) Color.Gray else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(if (viewModel.isMacBusy) "사용 불가 (동생 시청중)" else "Mac에서 가져오기")
        }
    }
}