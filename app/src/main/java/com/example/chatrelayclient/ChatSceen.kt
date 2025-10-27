package com.example.chatrelayclient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons // ★追加
import androidx.compose.material.icons.filled.ExitToApp // ★追加
import androidx.compose.material3.* // ★ (Button, Text などまとめて import)
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * チャット画面
 * @param viewModel 共有する ChatViewModel
 * @param onDisconnect 切断時に呼び出されるコールバック (画面遷移用)
 */
@OptIn(ExperimentalMaterial3Api::class) // ★TopAppBar用に必要
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onDisconnect: () -> Unit // ★コールバックを受け取る
) {
    // ViewModelが保持しているメッセージリストを監視
    // (messages.value が更新されると、 'messages' も自動的に更新 = 再描画)
    val messages by viewModel.messages.collectAsState()

    // ViewModelが保持している自分の名前
    val myName by viewModel.userName.collectAsState()

    // ViewModelが保持している接続状態の監視
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    // 画面下部のメッセージ入力欄用の状態変数
    var messageText by remember { mutableStateOf("") }

    // スクロール状態を管理
    val listState = rememberLazyListState()

    // --- 要望2: 接続が切れたら自動で戻る ---
    LaunchedEffect(connectionStatus) {
        if (connectionStatus != "Connected" && connectionStatus != "Connecting...") {
            // 接続状態が "Connected" 以外 (Error, Disconnected など) になったら
            // ログイン画面に戻る
            onDisconnect()
        }
    }

    // messagesリストの中身が変わるたびに、一番下までスクロールする
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(index = messages.size - 1)
        }
    }

    // ★画面全体を Scaffold で囲む
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "チャット (${connectionStatus})") }, // 接続状態を表示
                actions = {
                    // --- 要望1: 接続画面に戻るボタン ---
                    IconButton(onClick = {
                        viewModel.disconnect() // ViewModel に切断を通知
                        // onDisconnect() // LaunchedEffect が検知するので不要
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "切断")
                    }
                }
            )
        },
        // ★メッセージ送信欄 (画面下部) を Scaffold の bottomBar に移動
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("メッセージ") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                    },
                    enabled = messageText.isNotBlank() && connectionStatus == "Connected"
                ) {
                    Text(text = "送信")
                }
            }
        }
    ) { paddingValues -> // ★Scaffold が計算した padding

        // --- メッセージリスト ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // ★Scaffold の padding を適用
                .padding(horizontal = 8.dp), // 左右の余白
        ) {
            items(messages) { msg ->
                ChatMessageItem(msg = msg, isMine = (msg.user == myName))
            }
        }
    }
}

/**
 * チャットメッセージ1行分のUI (Discord風 左寄せ)
 */
@Composable
fun ChatMessageItem(msg: ChatMessage, isMine: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        // Discord風: 常に左寄せ
        Column(
            modifier = Modifier
                .background(
                    color = if (isMine) Color(0xFFDCF8C6) else Color.White, // 自分が送ったものだけ色を変える
                    shape = MaterialTheme.shapes.medium
                )
                .padding(8.dp)
        ) {
            // ユーザー名
            Text(
                text = msg.user,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            // メッセージ本文
            Text(
                text = msg.message,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}