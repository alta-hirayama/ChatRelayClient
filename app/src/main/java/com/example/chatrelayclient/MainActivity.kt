package com.example.chatrelayclient // パッケージ名はご自身のものに合わせてください

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel // ★ViewModelをComposeで使うために import
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chatrelayclient.ui.theme.ChatRelayClientTheme // テーマ名はご自身のものに

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatRelayClientTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // ★アプリ本体の画面(NavHost)を呼び出す
                    ChatAppNavigation()
                }
            }
        }
    }
}

/**
 * アプリの画面遷移（ナビゲーション）を管理する Composable
 */
@Composable
fun ChatAppNavigation() {
    // 1. ナビゲーションの状態を管理する「コントローラー」を作成
    val navController = rememberNavController()

    // 2. ★ViewModelのインスタンスを作成★
    // viewModel() 関数が、Activityが生きている間ずっと
    // 同一の ChatViewModel インスタンスを保持してくれます。
    val chatViewModel: ChatViewModel = viewModel()

    // 3. 画面遷移のホスト (NavHost) を設定
    NavHost(
        navController = navController,
        startDestination = "login" // 最初に表示する画面
    ) {

        // 4. 各画面のルートを定義 (中身を本物に入れ替え)

        // 画面1: ログイン画面
        composable(route = "login") {
            LoginScreen(
                viewModel = chatViewModel, // ★ViewModelを渡す
                onConnect = {
                    // 接続ボタンが押されたら、チャット画面に遷移
                    navController.navigate("chat")
                }
            )
        }

        // 画面2: チャット画面
        composable(route = "chat") {
            ChatScreen(
                viewModel = chatViewModel, // ★ログイン画面と【同じ】ViewModelを渡す
                // ★「切断」時にログイン画面に戻るコールバックを渡す
                onDisconnect = {
                    // "login" 画面に戻る (スタックをクリア)
                    navController.popBackStack(route = "login", inclusive = false)
                }
            )
        }
    }
}

// --- 仮置きだったプレビュー関数は削除 ---