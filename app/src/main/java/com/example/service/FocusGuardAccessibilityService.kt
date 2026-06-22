package com.example.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import android.content.Intent
import com.example.data.AppDatabase
import com.example.data.HistoryEntry
import com.example.data.PrefsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FocusGuardAccessibilityService : AccessibilityService() {

    private lateinit var prefsManager: PrefsManager
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private var isDebouncing = false

    companion object {
        var isServiceRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        prefsManager = PrefsManager(this)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        if (prefsManager.isBlockingEnabled) {
            FocusNotificationHelper.showActiveNotification(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        FocusNotificationHelper.cancelActiveNotification(this)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isServiceRunning = false
        FocusNotificationHelper.cancelActiveNotification(this)
        // Trigger notification when service unbinds / gets disabled
        FocusNotificationHelper.showDisabledNotification(this)
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        // Safety check: is blocking actually enabled in settings?
        if (!prefsManager.isBlockingEnabled) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg != "com.whatsapp" && pkg != "com.whatsapp.w4b") return

        val root = rootInActiveWindow ?: return

        if (isBlockedTabVisible(root)) {
            triggerBlockAction()
        }
    }

    private fun isBlockedTabVisible(root: AccessibilityNodeInfo): Boolean {
        val blockedTexts = listOf(
            "updates", "التحديثات", "status", "الحالات",
            "channels", "القنوات"
        )
        val blockedIds = listOf(
            "tab_updates", "tab_status", "tab_channels", "updates_tab", "status_tab"
        )

        val nodes = mutableListOf<AccessibilityNodeInfo>()
        collectNodes(root, nodes)

        var isBlockedSectionVisible = false
        for (node in nodes) {
            val text = node.text?.toString()?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            val id = node.viewIdResourceName?.lowercase() ?: ""
            
            // Check if selected, focused, or is currently on screen as an active tab
            val isSelectedOrFocused = node.isSelected || node.isFocused
            
            val matchesText = blockedTexts.any { it in text || it in desc }
            val matchesId = blockedIds.any { id.contains(it) }

            if ((matchesText || matchesId) && isSelectedOrFocused) {
                isBlockedSectionVisible = true
                break
            }
        }
        return isBlockedSectionVisible
    }

    private fun collectNodes(node: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
        node ?: return
        list.add(node)
        for (i in 0 until node.childCount) {
            collectNodes(node.getChild(i), list)
        }
    }

    private fun triggerBlockAction() {
        if (isDebouncing) return
        isDebouncing = true

        // 1. Kick the user back immediately
        performGlobalAction(GLOBAL_ACTION_BACK)

        // 2. Schedule secondary confirmation backup
        handler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_BACK)
            isDebouncing = false
        }, 150)

        // 3. Log event into Room Database on IO thread
        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                db.historyDao().insertEntry(HistoryEntry(tabType = "Updates/Channels"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Show a quick warning toast
        handler.post {
            Toast.makeText(
                applicationContext,
                "🚫 FocusGuard: WhatsApp Updates is blocked!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onInterrupt() {
        // Required
    }
}
