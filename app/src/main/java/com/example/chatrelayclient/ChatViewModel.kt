package com.example.chatrelayclient

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class ChatViewModel : ViewModel() {

    // KtorのWebSocketクライアント
    private val client = HttpClient(OkHttp) {
        // JSONプラグインのインストール
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true // JSONに未知のキーがあっても無視
            })
        }
        // WebSocketプラグインのインストール
        install(WebSockets) {
            // WebSocket で JSON を使うためのコンバーターを明示的に指定
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
    }

    // WebSocketセッションを保持する変数
    private var webSocketSession: DefaultClientWebSocketSession? = null

    // 接続状態を管理する (UIに "接続中..." などを表示するため)
    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus = _connectionStatus.asStateFlow()

    // ユーザー名を管理する
    private val _userName = MutableStateFlow("")
    val userName = _userName.asStateFlow()

    // チャットメッセージのリストを管理する
    // ★UIにはこのリストを表示します
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    /**
     * サーバーへの接続を開始する
     * @param ip 接続先のIPアドレス
     * @param name ユーザー名
     */
    fun connect(ip: String, name: String) {
        // 既に接続中なら何もしない
        if (webSocketSession != null && webSocketSession!!.isActive) return

        // ユーザー名を保存
        _userName.value = name

        // viewModelScopeで通信処理を開始 (Activityが閉じたら自動でキャンセルされる)
        viewModelScope.launch {
            try {
                // ▼▼▼ ログ追加 ① (接続試行) ▼▼▼
                Log.d("ChatViewModel", "Attempting to connect to ws://$ip:8080/ ...")
                // ▲▲▲ ここまで ▲▲▲
                _connectionStatus.value = "Connecting..."

                // サーバーに接続
                webSocketSession = client.webSocketSession {
                    url("ws://$ip:8080/") // サーバーのURL
                }

                // 接続成功
                _connectionStatus.value = "Connected"

                // ▼▼▼ ログ追加 ② (接続成功) ▼▼▼
                Log.d("ChatViewModel", "Success Connection!")
                // ▲▲▲ ここまで ▲▲▲

                // ★メッセージ受信ループ (接続が切れるまでここで待ち続ける)
                listenForMessages()

            } catch (e: Exception) {
                // 接続失敗
                _connectionStatus.value = "Error: ${e.message}"

                // ▼▼▼ ログ追加 ② (失敗原因) ▼▼▼
                Log.e("ChatViewModel", "Connection failed!", e)
                // ▲▲▲ ここまで ▲▲▲

                webSocketSession = null // セッションをクリア
            }
        }
    }


    /**
     * メッセージ受信ループ
     */
    private suspend fun listenForMessages() {
        try {
            webSocketSession?.incoming?.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val receivedText = frame.readText()

                    // ★デバッグログ（前回仕込んだもの）
                    Log.d("ChatViewModel", "Received raw: $receivedText")

                    // 受け取ったJSON文字列を ChatMessage オブジェクトに変換
                    val chatMessage = Json.decodeFromString<ChatMessage>(receivedText)

                    // ★デバッグログ（前回仕込んだもの）
                    Log.d("ChatViewModel", "Decoded: $chatMessage")

                    // メッセージリストの「末尾」に新しいメッセージを追加
                    // (UIが更新される)
                    _messages.value = _messages.value + chatMessage
                }
            }
        } catch (e: Exception) {
            // ▼▼▼ ここにログを追加 ▼▼▼
            Log.e("ChatViewModel", "Error in listenForMessages!", e)
            // ▲▲▲ ここまで ▲▲▲

            // 受信中にエラー (切断など)
            _connectionStatus.value = "Disconnected (Error)"
            webSocketSession = null
        }
    }


    /**
     * サーバーにメッセージを送信する
     * @param messageText 送信するメッセージ本文
     */
    fun sendMessage(messageText: String) {
        // 未接続、またはメッセージが空なら何もしない
        if (webSocketSession == null || !webSocketSession!!.isActive || messageText.isBlank()) return

        // 自分が送信するメッセージも、自分の画面に表示する
        val myMessage = ChatMessage(
            user = _userName.value, // 自分の名前
            message = messageText
        )
        // ローカル分の表示
        // _messages.value = _messages.value + myMessage

        // ★デバッグログ（前回仕込んだもの）
        Log.d("ChatViewModel", "Sending: $myMessage")

        // メッセージをサーバーに送信
        viewModelScope.launch {
            try {
                // ChatMessageオブジェクトをJSON文字列に自動変換して送信
                webSocketSession?.sendSerialized(myMessage)
            } catch (e: Exception) {
                // ▼▼▼ ここにログを追加 ▼▼▼
                Log.e("ChatViewModel", "Error in sendMessage!", e)
                // ▲▲▲ ここまで ▲▲▲

                // 送信失敗
                _connectionStatus.value = "Send Error: ${e.message}"
            }
        }
    }


    /**
     * サーバーから切断する
     */
    fun disconnect() {
        viewModelScope.launch {
            try {
                webSocketSession?.close(CloseReason(CloseReason.Codes.NORMAL, "User disconnected"))
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error during disconnect", e)
            } finally {
                webSocketSession = null
                _connectionStatus.value = "Disconnected"
                _messages.value = emptyList() // チャット履歴をクリア
            }
        }
    }


    /**
     * ViewModelが破棄されるときに呼ばれる (Activity終了時など)
     */
    override fun onCleared() {
        super.onCleared()
        disconnect() // 既存の切断処理を呼び出す
        client.close()
    }
}