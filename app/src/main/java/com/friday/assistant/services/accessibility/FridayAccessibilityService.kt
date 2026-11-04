package com.friday.assistant.services.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.friday.assistant.core.security.MasterSwitchManager
import com.friday.assistant.core.security.SecurityBlocklist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FridayAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: FridayAccessibilityService? = null
            private set

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _currentPackage = MutableStateFlow<String>("")
        val currentPackage: StateFlow<String> = _currentPackage.asStateFlow()

        private val _isSafetyLockEngaged = MutableStateFlow(false)
        val isSafetyLockEngaged: StateFlow<Boolean> = _isSafetyLockEngaged.asStateFlow()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        this.serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString() ?: ""
        if (pkgName.isNotBlank()) {
            _currentPackage.value = pkgName

            // CRITICAL SECURITY ENFORCEMENT:
            // If current foreground application is on the permanent payment/banking blocklist,
            // immediately engage safety lock and block any automation operations!
            if (SecurityBlocklist.isPackageBlocked(pkgName)) {
                _isSafetyLockEngaged.value = true
                return
            } else {
                _isSafetyLockEngaged.value = false
            }
        }
    }

    override fun onInterrupt() {
        _isSafetyLockEngaged.value = true
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceActive.value = false
    }

    /**
     * User-authorized: Finds and clicks a view by visible text or content description.
     */
    fun clickByText(text: String): Boolean {
        if (!isSafeToOperate()) return false

        val rootNode = rootInActiveWindow ?: return false
        val matchedNodes = rootNode.findAccessibilityNodeInfosByText(text)

        for (node in matchedNodes) {
            if (node.isClickable) {
                val success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (success) return true
            } else {
                // Check parent clickable container
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable) {
                        val success = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        if (success) return true
                    }
                    parent = parent.parent
                }
            }
        }
        return false
    }

    /**
     * User-authorized: Finds and clicks a view by resource ID.
     */
    fun clickById(viewId: String): Boolean {
        if (!isSafeToOperate()) return false

        val rootNode = rootInActiveWindow ?: return false
        val matchedNodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)

        for (node in matchedNodes) {
            if (node.isClickable) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
        return false
    }

    /**
     * User-authorized: Performs scroll gesture on the active window.
     */
    fun scroll(forward: Boolean = true): Boolean {
        if (!isSafeToOperate()) return false

        val rootNode = rootInActiveWindow ?: return false
        val action = if (forward) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }
        return rootNode.performAction(action)
    }

    /**
     * User-authorized: Sets text in an editable input field.
     */
    fun setTextInField(text: String, viewId: String? = null): Boolean {
        if (!isSafeToOperate()) return false

        val rootNode = rootInActiveWindow ?: return false
        val targetNode = if (viewId != null) {
            rootNode.findAccessibilityNodeInfosByViewId(viewId).firstOrNull { it.isEditable }
        } else {
            findFirstEditableNode(rootNode)
        }

        if (targetNode != null) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }
        return false
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstEditableNode(child)
            if (found != null) return found
        }
        return null
    }

    /**
     * Verifies that Master Automation Switch is ON and current screen is NOT on the Security Blocklist.
     */
    private fun isSafeToOperate(): Boolean {
        if (!MasterSwitchManager.isEnabled()) return false
        if (_isSafetyLockEngaged.value) return false
        if (SecurityBlocklist.isPackageBlocked(_currentPackage.value)) return false
        return true
    }
}

