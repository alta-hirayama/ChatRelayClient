// JSONの識別子typeからサブクラスに型変換するサンプル

package com.example.chatrelayclient

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// --- データクラス定義 ---

// 1. 同ファイルにあるクラスだけが継承できる基底となる sealed class を定義します。
//    - @Serializable アノテーションを付けます。
//    - JSON の "type" プロパティは、ライブラリが「どのサブクラスか」を判断するための
//      特別な「識別子 (discriminator)」として使うので、
//      通常、Kotlin のクラス定義には含めません。
@Serializable
sealed class ServerEvent {
    // 共通のプロパティがあればここに書く
}

// 2. 各 JSON の形に対応する data class を定義します。
//    - 基底クラス (ServerEvent) を継承します。
//    - @Serializable アノテーションを付けます。
//    - @SerialName アノテーションに、JSON の "type" プロパティの値を指定します。
//      これが、JSON の "type" と Kotlin のクラスを結びつける目印になります。

@Serializable
@SerialName("chat") // JSON の "type" が "chat" なら、このクラスに変換
data class ChatMessageEvent(
    @SerialName("user") val user: String,    // このイベントに必要なデータだけ定義
    // ちなみにクラスの中に@SerialName("")を使うと、
    // そのキーはクラスのそのプロパティと対応させますという意味になる
    // JSONのuser_idをdataクラスではuseIdに変えるみたいな
    val message: String
) : ServerEvent() // ServerEvent を継承


@Serializable
@SerialName("user_join") // JSON の "type" が "user_join" なら、このクラスに変換
data class UserJoinEvent(
    val user: String     // このイベントに必要なデータだけ定義
) : ServerEvent() // ServerEvent を継承

// --- デコードと利用例 ---

fun main() {
    // サンプルのJSON文字列
    val chatJson = """{"type":"chat", "user":"Alice", "message":"Hello!"}"""
    val joinJson = """{"type":"user_join", "user":"Bob"}"""

    // 3. JSON 文字列をデコードする際は「基底クラス」の型を指定します。
    //    - ライブラリが JSON 内の "type" の値を読み取り、
    //      @SerialName に基づいて適切なサブクラスのオブジェクトを生成してくれます。
    val event1: ServerEvent = Json.decodeFromString(chatJson)
    val event2: ServerEvent = Json.decodeFromString(joinJson)

    // event1 変数の「宣言された型」は ServerEvent ですが、
    // 「実際の中身」は ChatMessageEvent オブジェクトになっています。
    // 同様に event2 の中身は UserJoinEvent オブジェクトです。

    println("--- event1 の処理 ---")
    // 4. `when` 式と `is` (型チェック) を使って処理を分岐させます。
    //    - `is` で型をチェックすると、そのブロック内では Kotlin が自動的に
    //      その型に変換 (スマートキャスト) してくれるので、安全にプロパティにアクセスできます。
    when (event1) {
        is ChatMessageEvent -> {
            // このブロック内では event1 は ChatMessageEvent 型として扱われます。
            println("チャットメッセージを受信:")
            println("  ユーザー: ${event1.user}") // キャストなしで .user にアクセス
            println("  メッセージ: ${event1.message}") // キャストなしで .message にアクセス
        }
        is UserJoinEvent -> {
            // このブロック内では event1 は UserJoinEvent 型として扱われます。
            println("ユーザー参加を受信:")
            println("  ユーザー: ${event1.user}") // キャストなしで .user にアクセス
        }
        // もし ServerEvent を継承する他のクラスがあれば、ここに追加します。
        // sealed class なので、when 式は全てのサブクラスを網羅するように要求されます。
    }

    println("\n--- event2 の処理 ---")
    when (event2) {
        is ChatMessageEvent -> {
            println("チャットメッセージを受信:")
            println("  ユーザー: ${event2.user}")
            println("  メッセージ: ${event2.message}")
        }
        is UserJoinEvent -> {
            println("ユーザー参加を受信:")
            println("  ユーザー: ${event2.user}") // 中身は UserJoinEvent なのでこちらが実行される
        }
    }
}