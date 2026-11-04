package com.friday.assistant.core.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * F.R.I.D.A.Y. Master Automation Switch
 * Provides immediate user kill-switch control over all automation, app launching, and accessibility operations.
 */
object MasterSwitchManager {

    private val _isAutomationEnabled = MutableStateFlow(true)
    val isAutomationEnabled: StateFlow<Boolean> = _isAutomationEnabled.asStateFlow()

    fun setAutomationEnabled(enabled: Boolean) {
        _isAutomationEnabled.value = enabled
    }

    fun isEnabled(): Boolean = _isAutomationEnabled.value
}

