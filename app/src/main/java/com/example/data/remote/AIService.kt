package com.example.data.remote

import com.example.data.model.AIProvider
import com.example.data.model.AppSettings
import com.example.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class AIService {

    suspend fun sendMessage(
        history: List<ChatMessage>,
        userMessage: String,
        settings: AppSettings,
        searchContext: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val provider = settings.activeProvider
            val client = NetworkClient.createClient(settings)
            val jsonMediaType = "application/json; charset=utf-8".toMediaType()

            // Prepare prompt with optional web search augmentation
            val finalPrompt = if (!searchContext.isNullOrBlank()) {
                """
                [Real-time Web Search Results]:
                $searchContext
                
                [User Query]:
                $userMessage
                
                Use the web search results above to answer accurately and cite information if relevant.
                """.trimIndent()
            } else {
                userMessage
            }

            when (provider) {
                AIProvider.GEMINI -> {
                    val key = settings.geminiApiKey.ifBlank {
                        return@withContext Result.failure(Exception("Gemini API key is not configured. Set it in Settings or .env"))
                    }
                    val model = settings.activeModel.ifBlank { "gemini-2.0-flash" }
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"

                    val contentsArray = JSONArray()
                    // Add recent context (up to 8 messages)
                    val recent = history.takeLast(8)
                    for (msg in recent) {
                        val role = if (msg.role == "user") "user" else "model"
                        val partObj = JSONObject().put("text", msg.content)
                        val contentObj = JSONObject()
                            .put("role", role)
                            .put("parts", JSONArray().put(partObj))
                        contentsArray.put(contentObj)
                    }

                    // Add current prompt
                    val currentPart = JSONObject().put("text", finalPrompt)
                    contentsArray.put(
                        JSONObject().put("role", "user").put("parts", JSONArray().put(currentPart))
                    )

                    val bodyJson = JSONObject().put("contents", contentsArray)
                    val request = Request.Builder()
                        .url(url)
                        .post(bodyJson.toString().toRequestBody(jsonMediaType))
                        .build()

                    val response = client.newCall(request).execute()
                    val responseStr = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("Gemini Error (${response.code}): $responseStr"))
                    }

                    val json = JSONObject(responseStr)
                    val candidates = json.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        val content = candidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text")
                            return@withContext Result.success(text)
                        }
                    }
                    Result.failure(Exception("Gemini returned empty response: $responseStr"))
                }

                AIProvider.GROQ, AIProvider.CEREBRAS, AIProvider.MISTRAL, AIProvider.OPENROUTER -> {
                    val (apiKey, endpoint, defaultModel) = when (provider) {
                        AIProvider.GROQ -> Triple(
                            settings.groqApiKey,
                            "https://api.groq.com/openai/v1/chat/completions",
                            "llama-3.3-70b-versatile"
                        )
                        AIProvider.CEREBRAS -> Triple(
                            settings.cerebrasApiKey,
                            "https://api.cerebras.ai/v1/chat/completions",
                            "llama-3.3-70b"
                        )
                        AIProvider.MISTRAL -> Triple(
                            settings.mistralApiKey,
                            "https://api.mistral.ai/v1/chat/completions",
                            "mistral-large-latest"
                        )
                        AIProvider.OPENROUTER -> Triple(
                            settings.openRouterApiKey,
                            "https://openrouter.ai/api/v1/chat/completions",
                            "deepseek/deepseek-r1:free"
                        )
                        else -> Triple("", "", "")
                    }

                    if (apiKey.isBlank()) {
                        return@withContext Result.failure(
                            Exception("${provider.displayName} API Key is empty. Please enter your key in Settings.")
                        )
                    }

                    val model = settings.activeModel.ifBlank { defaultModel }
                    val messagesArray = JSONArray()

                    // System prompt
                    messagesArray.put(
                        JSONObject()
                            .put("role", "system")
                            .put("content", "You are Zenith AI, an elite, ultra-capable AI assistant with anime flair, coding brilliance, and deep technical knowledge.")
                    )

                    for (msg in history.takeLast(8)) {
                        messagesArray.put(
                            JSONObject()
                                .put("role", if (msg.role == "user") "user" else "assistant")
                                .put("content", msg.content)
                        )
                    }
                    messagesArray.put(
                        JSONObject().put("role", "user").put("content", finalPrompt)
                    )

                    val bodyJson = JSONObject()
                        .put("model", model)
                        .put("messages", messagesArray)
                        .put("temperature", 0.7)

                    val requestBuilder = Request.Builder()
                        .url(endpoint)
                        .addHeader("Authorization", "Bearer $apiKey")
                        .addHeader("Content-Type", "application/json")

                    if (provider == AIProvider.OPENROUTER) {
                        requestBuilder.addHeader("HTTP-Referer", "https://zenith.ai")
                        requestBuilder.addHeader("X-Title", "Zenith AI")
                    }

                    val request = requestBuilder
                        .post(bodyJson.toString().toRequestBody(jsonMediaType))
                        .build()

                    val response = client.newCall(request).execute()
                    val responseStr = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("${provider.displayName} Error (${response.code}): $responseStr")
                        )
                    }

                    val json = JSONObject(responseStr)
                    val choices = json.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val choice = choices.getJSONObject(0)
                        val messageObj = choice.optJSONObject("message")
                        val content = messageObj?.optString("content")
                        if (!content.isNullOrBlank()) {
                            return@withContext Result.success(content)
                        }
                    }
                    Result.failure(Exception("${provider.displayName} returned empty answer"))
                }

                AIProvider.TERMUX_SERVER -> {
                    val serverUrl = settings.termuxServerUrl.trimEnd('/')
                    val url = "$serverUrl/chat"

                    val messagesArray = JSONArray()
                    for (msg in history.takeLast(8)) {
                        messagesArray.put(
                            JSONObject()
                                .put("role", msg.role)
                                .put("content", msg.content)
                        )
                    }
                    messagesArray.put(
                        JSONObject().put("role", "user").put("content", finalPrompt)
                    )

                    val bodyJson = JSONObject()
                        .put("prompt", finalPrompt)
                        .put("messages", messagesArray)
                        .put("context", searchContext ?: "")

                    val request = Request.Builder()
                        .url(url)
                        .post(bodyJson.toString().toRequestBody(jsonMediaType))
                        .build()

                    val response = client.newCall(request).execute()
                    val responseStr = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("Termux Server Error (${response.code}): $responseStr\nMake sure your server script is running in Termux!")
                        )
                    }

                    // Parse reply (supports {"response": "..."} or {"message": "..."} or {"reply": "..."} or plain text)
                    try {
                        val json = JSONObject(responseStr)
                        val reply = json.optString("response").ifBlank {
                            json.optString("reply").ifBlank {
                                json.optString("message").ifBlank {
                                    json.optJSONObject("message")?.optString("content").orEmpty()
                                }
                            }
                        }
                        if (reply.isNotBlank()) return@withContext Result.success(reply)
                    } catch (_: Exception) {
                        // Plain text output
                    }

                    if (responseStr.isNotBlank()) {
                        Result.success(responseStr)
                    } else {
                        Result.failure(Exception("Termux server returned an empty body."))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
