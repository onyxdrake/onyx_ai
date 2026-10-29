package com.example.data.remote

import com.example.data.model.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException

data class TermuxStatus(
    val isOnline: Boolean,
    val latencyMs: Long,
    val message: String
)

class TermuxBridge {

    suspend fun checkServerStatus(settings: AppSettings): TermuxStatus = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val client = NetworkClient.createClient(settings)
        val serverUrl = settings.termuxServerUrl.trimEnd('/')

        val endpoints = listOf("$serverUrl/health", "$serverUrl/status", serverUrl)
        for (url in endpoints) {
            try {
                val request = Request.Builder().url(url).get().build()
                val response = client.newCall(request).execute()
                val latency = System.currentTimeMillis() - startTime
                if (response.isSuccessful) {
                    return@withContext TermuxStatus(
                        isOnline = true,
                        latencyMs = latency,
                        message = "Termux server active on $serverUrl (${latency}ms)"
                    )
                }
            } catch (_: IOException) {
                // Try next endpoint
            } catch (_: Exception) {
                // Ignore and proceed
            }
        }

        TermuxStatus(
            isOnline = false,
            latencyMs = -1,
            message = "Termux server unreachable at $serverUrl. Check if Python/Node server is running."
        )
    }

    fun getPythonServerScript(): String {
        return """
# Zenith AI - Termux Server Bridge (server.py)
# Run in Termux:
#   pkg update && pkg install python
#   pip install flask requests
#   python server.py

from flask import Flask, request, jsonify
import requests, os

app = Flask(__name__)

DISCORD_WEBHOOK = os.getenv("DISCORD_WEBHOOK_URL", "")
TELEGRAM_BOT_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN", "")
TELEGRAM_CHAT_ID = os.getenv("TELEGRAM_CHAT_ID", "")

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "online", "service": "Zenith AI Termux Bridge", "version": "1.0"})

@app.route('/chat', methods=['POST'])
def chat():
    data = request.json or {}
    prompt = data.get('prompt', '')
    messages = data.get('messages', [])
    
    # Process or route to local tools/scripts
    reply = f"[Termux AI Engine]: Received query '{prompt[:40]}...'. Local bridge operational."
    return jsonify({"response": reply, "status": "success"})

@app.route('/discord', methods=['POST'])
def discord_relay():
    data = request.json or {}
    msg = data.get('message', '')
    webhook = data.get('webhook', DISCORD_WEBHOOK)
    if not webhook:
        return jsonify({"error": "No webhook configured"}), 400
    res = requests.post(webhook, json={"content": msg})
    return jsonify({"status": res.status_code})

@app.route('/telegram', methods=['POST'])
def telegram_relay():
    data = request.json or {}
    text = data.get('text', '')
    token = data.get('token', TELEGRAM_BOT_TOKEN)
    chat_id = data.get('chat_id', TELEGRAM_CHAT_ID)
    if not token or not chat_id:
        return jsonify({"error": "Missing token or chat_id"}), 400
    url = f"https://api.telegram.org/bot{token}/sendMessage"
    res = requests.post(url, json={"chat_id": chat_id, "text": text})
    return jsonify({"status": res.status_code})

if __name__ == '__main__':
    print(">>> Zenith AI Termux Server running on 0.0.0.0:8080 <<<")
    app.run(host='0.0.0.0', port=8080, debug=False)
        """.trimIndent()
    }

    fun getNodeServerScript(): String {
        return """
// Zenith AI - Termux Node.js Server Bridge (server.js)
// Run in Termux:
//   pkg install nodejs
//   npm install express cors
//   node server.js

const express = require('express');
const cors = require('cors');
const app = express();
app.use(cors());
app.use(express.json());

app.get('/health', (req, res) => {
  res.json({ status: 'online', service: 'Zenith Node Bridge' });
});

app.post('/chat', (req, res) => {
  const { prompt } = req.body;
  res.json({ response: "[Node Termux]: Bridge processed prompt '" + prompt + "'." });
});

app.listen(8080, '0.0.0.0', () => {
  console.log('Zenith Node Bridge running on port 8080');
});
        """.trimIndent()
    }
}
