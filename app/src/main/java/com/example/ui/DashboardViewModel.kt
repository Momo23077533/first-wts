package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.HistoryEntry
import com.example.data.HistoryRepository
import com.example.data.PrefsManager
import com.example.service.FocusGuardAccessibilityService
import com.example.service.FocusNotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenState {
    Home,
    PinSetup,
    PinVerify
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsManager = PrefsManager(application)
    private val repository: HistoryRepository

    private val _screenState = MutableStateFlow(ScreenState.Home)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private val _isBlockingEnabled = MutableStateFlow(prefsManager.isBlockingEnabled)
    val isBlockingEnabled: StateFlow<Boolean> = _isBlockingEnabled.asStateFlow()

    private val _isServiceActive = MutableStateFlow(FocusGuardAccessibilityService.isServiceRunning)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    private val _isPinSet = MutableStateFlow(prefsManager.isPinSet())
    val isPinSet: StateFlow<Boolean> = _isPinSet.asStateFlow()

    // PIN Setup values
    val pinSetupInput = MutableStateFlow("")
    val pinSetupConfirmInput = MutableStateFlow("")
    val pinSetupError = MutableStateFlow<String?>(null)

    // PIN Verification values
    val pinVerifyInput = MutableStateFlow("")
    val pinVerifyError = MutableStateFlow<String?>(null)
    
    // Remaining Lockout Time in Seconds
    private val _lockoutTimeLeft = MutableStateFlow(0L)
    val lockoutTimeLeft: StateFlow<Long> = _lockoutTimeLeft.asStateFlow()

    // Dialog state for accessibility activation prompt
    val showAccessibilityPrompt = MutableStateFlow(false)

    // Flow from Room for total block attempts
    val totalBlocksCount: StateFlow<Int>

    private var countdownJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = HistoryRepository(database.historyDao())
        totalBlocksCount = repository.totalBlocksCount
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

        // Sync statuses periodically
        viewModelScope.launch {
            while (true) {
                _isServiceActive.value = isAccessibilityServiceEnabled(application)
                _isBlockingEnabled.value = prefsManager.isBlockingEnabled
                checkLockoutState()
                delay(1000)
            }
        }
    }

    private fun checkLockoutState() {
        val timeLeft = prefsManager.getRemainingLockoutTimeSec()
        _lockoutTimeLeft.value = timeLeft
        if (timeLeft <= 0 && prefsManager.failedAttempts >= 3 && countdownJob == null) {
            prefsManager.failedAttempts = 0
            prefsManager.lockoutUntilEpoch = 0L
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedService = "${context.packageName}/com.example.service.FocusGuardAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(':').any { it.equals(expectedService, ignoreCase = true) }
    }

    fun toggleBlocking() {
        if (prefsManager.isBlockingEnabled) {
            // Trying to turn OFF -> Require PIN if set
            if (prefsManager.isPinSet()) {
                pinVerifyInput.value = ""
                pinVerifyError.value = null
                _screenState.value = ScreenState.PinVerify
            } else {
                // No PIN set, turn off immediately
                setBlockingEnabled(false)
            }
        } else {
            // Trying to turn ON
            if (!isAccessibilityServiceEnabled(getApplication())) {
                showAccessibilityPrompt.value = true
                setBlockingEnabled(true)
            } else {
                setBlockingEnabled(true)
            }
        }
    }

    private fun setBlockingEnabled(enabled: Boolean) {
        prefsManager.isBlockingEnabled = enabled
        _isBlockingEnabled.value = enabled
        
        val context = getApplication<Application>()
        if (enabled && isAccessibilityServiceEnabled(context)) {
            FocusNotificationHelper.showActiveNotification(context)
        } else {
            FocusNotificationHelper.cancelActiveNotification(context)
        }
    }

    fun navigateToPinSetup() {
        pinSetupInput.value = ""
        pinSetupConfirmInput.value = ""
        pinSetupError.value = null
        _screenState.value = ScreenState.PinSetup
    }

    fun navigateToHome() {
        _screenState.value = ScreenState.Home
    }

    fun handlePinSetupSave() {
        val p1 = pinSetupInput.value
        val p2 = pinSetupConfirmInput.value

        if (p1.length < 4) {
            pinSetupError.value = "PIN must be at least 4 digits."
            return
        }
        if (p1 != p2) {
            pinSetupError.value = "PINs do not match."
            return
        }

        prefsManager.savePinHash(p1)
        _isPinSet.value = true
        _screenState.value = ScreenState.Home
    }

    fun handlePinVerify() {
        if (prefsManager.isLockedOut()) {
            pinVerifyError.value = "Too many failed attempts. Wait 30 seconds."
            return
        }

        val input = pinVerifyInput.value
        if (prefsManager.verifyPin(input)) {
            prefsManager.failedAttempts = 0
            prefsManager.lockoutUntilEpoch = 0L
            setBlockingEnabled(false)
            _screenState.value = ScreenState.Home
        } else {
            val newAttempts = prefsManager.failedAttempts + 1
            prefsManager.failedAttempts = newAttempts
            if (newAttempts >= 3) {
                prefsManager.lockoutUntilEpoch = System.currentTimeMillis() + 30000L
                pinVerifyError.value = "Too many incorrect attempts! Locked for 30s."
                startLockoutCountdown()
            } else {
                val rem = 3 - newAttempts
                pinVerifyError.value = "Incorrect PIN. $rem attempts remaining."
            }
        }
    }

    private fun startLockoutCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (prefsManager.isLockedOut()) {
                _lockoutTimeLeft.value = prefsManager.getRemainingLockoutTimeSec()
                delay(1000)
            }
            prefsManager.failedAttempts = 0
            prefsManager.lockoutUntilEpoch = 0L
            pinVerifyError.value = null
            _lockoutTimeLeft.value = 0L
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }
}
