package com.friday.assistant.domain.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.friday.assistant.data.preferences.PreferencesManager
import com.friday.assistant.data.remote.FridayApiClient
import com.friday.assistant.data.remote.models.ApiMessageItem
import com.friday.assistant.data.remote.models.AssistantChatRequest
import com.friday.assistant.data.remote.models.AssistantChatResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

enum class NetworkConnectivityMode {
    ONLINE,
    OFFLINE,
    SWITCHING,
    LIMITED,
    BACKEND_UNAVAILABLE
}

data class RoutedResult(
    val isCloudExecuted: Boolean,
    val modelUsed: String,
    val response: AssistantChatResponse?,
    val latencyMs: Long,
    val fallbackOccurred: Boolean = false
)

class FridayAIModelRouter(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val preferencesManager = PreferencesManager(context)
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _connectivityMode = MutableStateFlow(NetworkConnectivityMode.OFFLINE)
    val connectivityMode: StateFlow<NetworkConnectivityMode> = _connectivityMode.asStateFlow()

    init {
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        if (connectivityManager == null) {
            _connectivityMode.value = NetworkConnectivityMode.OFFLINE
            return
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            // Initial check
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            if (capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                _connectivityMode.value = NetworkConnectivityMode.ONLINE
            } else {
                _connectivityMode.value = NetworkConnectivityMode.OFFLINE
            }

            connectivityManager.registerNetworkCallback(
                request,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _connectivityMode.value = NetworkConnectivityMode.ONLINE
                    }

                    override fun onLost(network: Network) {
                        _connectivityMode.value = NetworkConnectivityMode.OFFLINE
                    }

                    override fun onUnavailable() {
                        _connectivityMode.value = NetworkConnectivityMode.OFFLINE
                    }
                }
            )
        } catch (e: Exception) {
            _connectivityMode.value = NetworkConnectivityMode.OFFLINE
        }
    }

    /**
     * Intelligent Routing Algorithm:
     * Evaluates Connectivity + Force Offline Preference + Backend Reachability.
     * Guaranteed failover to local engine with zero crashes.
     */
    suspend fun routeRequest(
        query: String,
        history: List<ApiMessageItem>,
        installedPackages: List<String>
    ): RoutedResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val forceOffline = preferencesManager.forceOffline.first()
        val isNetworkAvailable = _connectivityMode.value == NetworkConnectivityMode.ONLINE

        if (forceOffline || !isNetworkAvailable) {
            return@withContext RoutedResult(
                isCloudExecuted = false,
                modelUsed = "On-Device Local AI Engine",
                response = null,
                latencyMs = System.currentTimeMillis() - startTime,
                fallbackOccurred = false
            )
        }

        // Network is online, attempt cloud backend
        val baseUrl = preferencesManager.backendUrl.first()
        val selectedModel = preferencesManager.selectedModel.first()

        // If backend URL is local/placeholder and unreachable, gracefully fall back immediately
        if (baseUrl.isBlank() || baseUrl.contains("localhost") || baseUrl.contains("10.0.2.2") || baseUrl.contains("internal")) {
            return@withContext RoutedResult(
                isCloudExecuted = false,
                modelUsed = "On-Device Local AI Engine",
                response = null,
                latencyMs = System.currentTimeMillis() - startTime,
                fallbackOccurred = false
            )
        }

        try {
            val apiService = FridayApiClient.getService(baseUrl)
            val call = apiService.sendChat(
                AssistantChatRequest(
                    query = query,
                    conversationHistory = history,
                    installedPackages = installedPackages
                )
            )

            if (call.isSuccessful && call.body() != null) {
                return@withContext RoutedResult(
                    isCloudExecuted = true,
                    modelUsed = "Cloud AI ($selectedModel)",
                    response = call.body(),
                    latencyMs = System.currentTimeMillis() - startTime,
                    fallbackOccurred = false
                )
            } else {
                _connectivityMode.value = NetworkConnectivityMode.BACKEND_UNAVAILABLE
                return@withContext RoutedResult(
                    isCloudExecuted = false,
                    modelUsed = "On-Device Local AI Engine (Fallback)",
                    response = null,
                    latencyMs = System.currentTimeMillis() - startTime,
                    fallbackOccurred = true
                )
            }
        } catch (e: Exception) {
            _connectivityMode.value = NetworkConnectivityMode.BACKEND_UNAVAILABLE
            return@withContext RoutedResult(
                isCloudExecuted = false,
                modelUsed = "On-Device Local AI Engine (Fallback)",
                response = null,
                latencyMs = System.currentTimeMillis() - startTime,
                fallbackOccurred = true
            )
        }
    }
}

