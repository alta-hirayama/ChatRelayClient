package com.example.chatrelayclient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * ログイン画面 (IPと名前を入力)
 * @param viewModel 共有する ChatViewModel
 * @param onConnect 接続ボタンが押されたときのコールバック (画面遷移用)
 */
@Composable
fun LoginScreen(
    viewModel: ChatViewModel,
    onConnect: () -> Unit // "() -> Unit" は「引数なし、戻り値なしの関数」という意味
) {
    // 画面内で使用する一時的な状態変数
    var ip by remember { mutableStateOf("192.168.11.16") } // IP入力用
    var name by remember { mutableStateOf("alta") }          // 名前入力用

    // ★ViewModelの接続状態を監視
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    // ★接続状態(connectionStatus) が変化したら実行
    LaunchedEffect(connectionStatus) {
        if (connectionStatus == "Connected") {
            // 接続成功時のみ、画面遷移コールバックを呼ぶ
            onConnect()
        }
        // TODO: if (connectionStatus.startsWith("Error")) { ... }
        // (ここでエラーメッセージをトーストなどで表示すると親切)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "サーバーに接続", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        // IPアドレス入力欄
        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it },
            label = { Text("サーバーIPアドレス") }
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 名前入力欄
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("あなたの名前") }
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 接続ボタン
        Button(
            onClick = {
                // ボタンが押されたら...
                // 1. ViewModelのconnectメソッドを呼ぶ（だけにする）
                viewModel.connect(ip, name)
            },
            // ★接続中はボタンを押せなくする
            enabled = (ip.isNotBlank() && name.isNotBlank() && connectionStatus != "Connecting...")
        ) {
            // ★接続状態に応じてボタンの文字を変える
            Text(text = if (connectionStatus == "Connecting...") "接続中..." else "接続")
        }
    }
}