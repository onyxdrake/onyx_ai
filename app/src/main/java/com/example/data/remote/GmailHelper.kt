package com.example.data.remote

import com.example.data.model.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class OtpResult(
    val code: String?,
    val sender: String?,
    val subject: String?,
    val snippet: String?,
    val rawMessage: String
)

class GmailHelper {

    suspend fun fetchLatestOtp(settings: AppSettings): Result<OtpResult> = withContext(Dispatchers.IO) {
        val user = settings.gmailUser.trim()
        val pass = settings.gmailAppPassword.trim()
        val serverUrl = settings.termuxServerUrl.trimEnd('/')

        if (user.isBlank() || pass.isBlank()) {
            return@withContext Result.failure(
                Exception("Gmail user or App Password is not set. Fill in Settings -> Integrations.")
            )
        }

        try {
            val client = NetworkClient.createClient(settings)
            // Query Termux bridge endpoint for secure server-side IMAP reading
            val json = JSONObject()
                .put("user", user)
                .put("password", pass)
                .put("target", "OTP")

            val request = Request.Builder()
                .url("$serverUrl/gmail/otp")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val resStr = response.body?.string().orEmpty()

            if (response.isSuccessful && resStr.isNotBlank()) {
                val data = JSONObject(resStr)
                val code = data.optString("code")
                val sender = data.optString("sender")
                val subject = data.optString("subject")
                val snippet = data.optString("snippet")

                Result.success(
                    OtpResult(
                        code = code.ifBlank { null },
                        sender = sender.ifBlank { "Google / Verification Service" },
                        subject = subject.ifBlank { "Your verification code" },
                        snippet = snippet.ifBlank { resStr },
                        rawMessage = resStr
                    )
                )
            } else {
                // If Termux server didn't implement it yet, provide instructions and status
                Result.failure(
                    Exception("Termux /gmail/otp responded with code ${response.code}. Ensure your Termux server script has the IMAP route.")
                )
            }
        } catch (e: Exception) {
            Result.failure(
                Exception("Could not connect to Termux Gmail OTP bridge: ${e.localizedMessage}")
            )
        }
    }
}
