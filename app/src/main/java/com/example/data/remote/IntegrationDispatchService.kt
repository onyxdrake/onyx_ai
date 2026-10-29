package com.example.data.remote

import com.example.data.model.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class IntegrationDispatchService {

    suspend fun sendToDiscord(message: String, settings: AppSettings): Result<String> =
        withContext(Dispatchers.IO) {
            val webhook = settings.discordWebhookUrl.trim()
            if (webhook.isBlank()) {
                return@withContext Result.failure(Exception("Discord Webhook URL is not set in Settings."))
            }

            try {
                val client = NetworkClient.createClient(settings)
                val jsonBody = JSONObject().put("content", message).toString()
                val request = Request.Builder()
                    .url(webhook)
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Result.success("Message dispatched to Discord successfully.")
                } else {
                    Result.failure(Exception("Discord error code ${response.code}: ${response.body?.string()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun sendToTelegram(message: String, settings: AppSettings): Result<String> =
        withContext(Dispatchers.IO) {
            val token = settings.telegramBotToken.trim()
            val chatId = settings.telegramChatId.trim()
            if (token.isBlank() || chatId.isBlank()) {
                return@withContext Result.failure(
                    Exception("Telegram Bot Token or Chat ID is not configured in Settings.")
                )
            }

            try {
                val client = NetworkClient.createClient(settings)
                val url = "https://api.telegram.org/bot$token/sendMessage"
                val jsonBody = JSONObject()
                    .put("chat_id", chatId)
                    .put("text", message)
                    .toString()

                val request = Request.Builder()
                    .url(url)
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Result.success("Message dispatched to Telegram bot successfully.")
                } else {
                    Result.failure(Exception("Telegram error code ${response.code}: ${response.body?.string()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
