package com.example.data.remote

import com.example.data.model.AppSettings
import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

object NetworkClient {
    fun createClient(settings: AppSettings): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)

        if (settings.isTorEnabled && settings.torHost.isNotBlank()) {
            try {
                // SOCKS proxy for Tor routing
                val proxyAddress = InetSocketAddress(settings.torHost, settings.torPort)
                val proxy = Proxy(Proxy.Type.SOCKS, proxyAddress)
                builder.proxy(proxy)
            } catch (_: Exception) {
                // Fallback to direct if proxy fails to initialize
            }
        }

        return builder.build()
    }
}
