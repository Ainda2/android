package mega.privacy.android.app.activities.settingsActivities.passcodelock

import mega.privacy.android.shared.resources.R as sharedR
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.view.MenuItem
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo.IME_ACTION_DONE
import android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
import android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationResult
import androidx.biometric.BiometricPrompt.CryptoObject
import androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.ActivityPasscodeBinding
import mega.privacy.android.app.extensions.enableEdgeToEdgeAndConsumeInsets
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.PasscodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.logout.LogoutViewModel
import mega.privacy.android.app.presentation.passcode.PasscodeUnlockViewModel
import mega.privacy.android.app.utils.Constants.PIN_4
import mega.privacy.android.app.utils.Constants.PIN_6
import mega.privacy.android.app.utils.Constants.PIN_ALPHANUMERIC
import mega.privacy.android.app.utils.Util.dp2px
import mega.privacy.android.app.utils.Util.hideKeyboardView
import mega.privacy.android.app.utils.wrapper.PasscodePreferenceWrapper
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.mobile.analytics.event.ForgotPasscodeButtonPressedEvent
import mega.privacy.mobile.analytics.event.PasscodeBiometricUnlockDialogEvent
import mega.privacy.mobile.analytics.event.PasscodeLogoutButtonPressedEvent
import mega.privacy.mobile.analytics.event.PasscodeScreenEvent
import mega.privacy.mobile.analytics.event.PasscodeSettingScreenEvent
import timber.log.Timber
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject

@AndroidEntryPoint
class PasscodeLockActivity : BaseActivity() {

    companion object {
        const val ACTION_SET_PASSCODE_LOCK = "ACTION_SET"
        const val ACTION_RESET_PASSCODE_LOCK = "ACTION_RESET"
        private const val MAX_ATTEMPTS = 10
        private const val MIN_ATTEMPTS_TO_SHOW_WARNING = 5
        private const val UNLOCK_MODE = 0
        private const val SET_MODE = 1
        private const val RESET_MODE = 2
        private const val SECOND_ROUND = "SECOND_ROUND"
        private const val SB_FIRST = "SB_FIRST"
        private const val ATTEMPTS = "ATTEMPTS"
        private const val PASSCODE_TYPE = "PASSCODE_TYPE"
        private const val IS_CONFIRM_LOGOUT_SHOWN = "IS_CONFIRM_LOGOUT_SHOWN"
        private const val FINGERPRINT_SKIPPED = "FINGERPRINT_SKIPPED"
        private const val FINGERPRINT_ENABLED = "FINGERPRINT_ENABLED"
        private const val KEY_NAME = "MEGA_KEY"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val FORGET_PASSCODE = "FORGET_PASSCODE"
        private const val PASSWORD_ALREADY_TYPED = "PASSWORD_ALREADY_TYPED"
    }

    private var attempts = 0
    private var mode = UNLOCK_MODE
    private var setOrUnlockMode = true

    @Inject
    lateinit var passcodePreferenceWrapper: PasscodePreferenceWrapper

    private val unlockViewModel by viewModels<PasscodeUnlockViewModel>()

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    private lateinit var binding: ActivityPasscodeBinding
    private var passcodeType = PIN_4

    private var secondRound = false
    private val sbFirst = StringBuilder()
    private val sbSecond = StringBuilder()

    private var passcodeOptionsBottomSheetDialogFragment: PasscodeOptionsBottomSheetDialogFragment? =
        null

    private var isConfirmLogoutDialogShown: Boolean = false

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: PromptInfo
    private lateinit var cipher: Cipher
    private lateinit var keyStore: KeyStore
    private lateinit var keyGenerator: KeyGenerator
    private var fingerprintEnabled = false
    private var fingerprintSkipped = false
    private var forgetPasscode = false
    private var passwordAlreadyTyped = false
    private val viewModel by viewModels<PasscodeLockViewModel>()
    private val logoutViewModel by viewModels<LogoutViewModel>()

    /**
     * Handle events when a Back Press is detected
     */
    private val onBackPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (attempts < MAX_ATTEMPTS) {
                when (mode) {
                    UNLOCK_MODE -> {
                        if (forgetPasscode) {
                            forgetPasscode = false
                            lifecycleScope.launch {
                                initPasscodeScreen()
                            }
                        } else {
                            return
                        }
                    }

                    RESET_MODE -> {}//passcodeManagement.showPasscodeScreen = false
                    else -> finishActivity()
                }
            }
            setResult(RESULT_CANCELED)
            finishActivity()
        }
    }

    private fun finishActivity() {
        retryConnectionsAndSignalPresence()
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mode = when (intent.action) {
            ACTION_SET_PASSCODE_LOCK -> SET_MODE
            ACTION_RESET_PASSCODE_LOCK -> RESET_MODE
            else -> UNLOCK_MODE
        }

        if (mode == UNLOCK_MODE) {
            Analytics.tracker.trackEvent(PasscodeScreenEvent)
        } else {
            Analytics.tracker.trackEvent(PasscodeSettingScreenEvent)
        }

        setOrUnlockMode = mode == SET_MODE || mode == UNLOCK_MODE
        enableEdgeToEdgeAndConsumeInsets { insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            binding.passcodeOptionsButton.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = if (isImeVisible) {
                    imeHeight
                } else {
                    dp2px(40f, resources.displayMetrics)
                }
            }
        }

        binding = ActivityPasscodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (mode == UNLOCK_MODE) {
            binding.toolbarPasscode.isVisible = false
        } else {
            setSupportActionBar(binding.toolbarPasscode)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title =
                getString(
                    if (mode == SET_MODE) R.string.settings_passcode_lock
                    else R.string.title_change_passcode
                )
        }

        onBackPressedDispatcher.addCallback(this, onBackPressCallback)

        lifecycleScope.launch {
            if (savedInstanceState != null) {
                secondRound = savedInstanceState.getBoolean(SECOND_ROUND, false)

                if (secondRound) {
                    sbFirst.append(savedInstanceState.getString(SB_FIRST))
                }

                attempts = savedInstanceState.getInt(ATTEMPTS, 0)
                passcodeType = savedInstanceState.getString(PASSCODE_TYPE, PIN_4)
                fingerprintEnabled = savedInstanceState.getBoolean(FINGERPRINT_ENABLED, false)
                fingerprintSkipped = savedInstanceState.getBoolean(FINGERPRINT_SKIPPED, false)
                forgetPasscode = savedInstanceState.getBoolean(FORGET_PASSCODE, false)
                passwordAlreadyTyped = savedInstanceState.getBoolean(PASSWORD_ALREADY_TYPED, false)

                if (savedInstanceState.getBoolean(IS_CONFIRM_LOGOUT_SHOWN, false)) {
                    viewModel.checkLogoutConfirmation()
                }
            } else {
                passcodeType = passcodePreferenceWrapper.getPasscodeType()

                if (mode == UNLOCK_MODE) {
                    fingerprintEnabled = passcodePreferenceWrapper.isFingerPrintLockEnabled()
                }
            }
            initPasscodeScreen()
            setListeners()
        }
        setupObservers()
    }

    private fun setupObservers() {
        collectFlow(viewModel.uiState) { state ->
            state.logoutEvent?.let {
                askConfirmLogout(state.logoutEvent.first, state.logoutEvent.second)
                viewModel.onLogoutEventConsumed()
            }
        }
    }

    /**
     * Increments the number of failed attempts.
     */
    private fun incrementAttempts() {
        lifecycleScope.launch {
            attempts++
            passcodePreferenceWrapper.setFailedAttemptsCount(attempts)
        }
    }

    /**
     * Resets the number of failed attempts to 0.
     */
    private fun resetAttempts() {
        lifecycleScope.launch {
            attempts = 0
            passcodePreferenceWrapper.setFailedAttemptsCount(attempts)
        }
    }

    /**
     * If user has offline files or ongoing transfers shows a confirmation dialog before proceed to logout.
     * Otherwise, if user has no offline files or ongoing transfers, then proceeds to logout.
     *
     * @param existOfflineFiles
     * @param existOutgoingTransfers
     */
    private fun askConfirmLogout(existOfflineFiles: Boolean, existOutgoingTransfers: Boolean) {
        if (!existOfflineFiles && !existOutgoingTransfers) {
            logout()
            return
        }

        val messageId = when {
            existOfflineFiles && existOutgoingTransfers -> R.string.logout_warning_dialog_offline_and_transfers_message
            existOfflineFiles -> R.string.logout_warning_dialog_offline_message
            else -> R.string.logout_warning_dialog_transfers_message
        }

        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
            .setTitle(getString(R.string.logout_warning_dialog_title))
            .setMessage(getString(messageId))
            .setPositiveButton(getString(R.string.logout_warning_dialog_positive_button)) { _, _ ->
                isConfirmLogoutDialogShown = false
                logout()
            }
            .setNegativeButton(getString(sharedR.string.general_dialog_cancel_button)) { _, _ ->
                isConfirmLogoutDialogShown = false
            }
            .create()
            .show()

        isConfirmLogoutDialogShown = true
    }

    /**
     * Resets the number of failed attempts and then logs out.
     */
    private fun logout() {
        resetAttempts()
        logoutViewModel.logout()
    }

    /**
     * Sets the whole initial passcode screen.
     */
    private suspend fun initPasscodeScreen() {
        setTitleText()

        if (shouldShowFingerprintLock() && !fingerprintSkipped) {
            binding.passcodeScrollView.isVisible = false
            showFingerprintUnlock()
        } else {
            binding.passcodeScrollView.isVisible = true
            binding.passcodeOptionsButton.isVisible = !secondRound && mode != UNLOCK_MODE

            if (mode == UNLOCK_MODE) {
                attempts = passcodePreferenceWrapper.getFailedAttemptsCount()
                binding.passcodeOptionsButton.isVisible = false
            }

            binding.doNotMatchWarning.isVisible = false

            if (forgetPasscode) {
                hidePins()
                hideErrorElements()
                binding.passwordInput.isVisible = false

                binding.passwordLayout.apply {
                    isVisible = true
                    requestFocus()
                }

                binding.passwordField.setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == IME_ACTION_DONE) {
                        lifecycleScope.launch { checkPassword() }
                        true
                    } else false
                }

                if (passwordAlreadyTyped) {
                    showAttemptsError()
                }

                return
            }

            binding.passwordLayout.isVisible = false

            if (attempts > 0) {
                showAttemptsError()
            } else {
                hideErrorElements()
            }

            if (passcodeType == PIN_ALPHANUMERIC) {
                hidePins()

                binding.passwordInput.apply {
                    isVisible = true
                    requestFocus()

                    setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == IME_ACTION_DONE) {
                            checkPasscode()
                            true
                        } else false

                    }
                }
            } else {
                binding.passFirstInput.apply {
                    isVisible = true
                    requestFocus()

                    doAfterTextChanged { text ->
                        val isValid = text.isNullOrEmpty().not()
                        if (isValid) {
                            binding.passSecondInput.requestFocus()
                        }
                        isFocusable = !isValid
                        isFocusableInTouchMode = !isValid
                    }
                }

                binding.passSecondInput.apply {
                    isVisible = true

                    doAfterTextChanged { text ->
                        val isValid = text.isNullOrEmpty().not()
                        if (isValid) {
                            binding.passThirdInput.requestFocus()
                        }
                        isFocusable = !isValid
                        isFocusableInTouchMode = !isValid
                    }

                    previousDigitEditText = binding.passFirstInput
                }


                binding.passThirdInput.apply {
                    isVisible = true

                    doAfterTextChanged { text ->
                        val isValid = text.isNullOrEmpty().not()
                        if (isValid) {
                            binding.passFourthInput.requestFocus()
                        }
                        isFocusable = !isValid
                        isFocusableInTouchMode = !isValid
                    }

                    previousDigitEditText = binding.passSecondInput
                }

                binding.passFourthInput.apply {
                    isVisible = true

                    doAfterTextChanged { text ->
                        if (passcodeType == PIN_4) {
                            if (this.text.toString().isNotEmpty()) {
                                binding.passFirstInput.apply {
                                    isCursorVisible = false
                                    requestFocus()
                                }

                                checkPasscode()
                            }
                        } else {
                            val isValid = text.isNullOrEmpty().not()
                            if (isValid) {
                                binding.passFifthInput.requestFocus()
                            }
                            isFocusable = !isValid
                            isFocusableInTouchMode = !isValid
                        }
                    }

                    previousDigitEditText = binding.passThirdInput
                }

                val params = binding.passFourthInput.layoutParams as ConstraintLayout.LayoutParams

                if (passcodeType == PIN_4) {
                    binding.passFourthInput.imeOptions = IME_ACTION_DONE or IME_FLAG_NO_FULLSCREEN

                    params.marginEnd = 0

                    binding.passFifthInput.isVisible = false
                    binding.passSixthInput.isVisible = false
                } else {
                    binding.passFourthInput.imeOptions = IME_ACTION_NEXT or IME_FLAG_NO_FULLSCREEN

                    params.marginEnd = dp2px(16F, resources.displayMetrics)

                    binding.passFifthInput.apply {
                        isVisible = true

                        doAfterTextChanged { text ->
                            val isValid = text.isNullOrEmpty().not()
                            if (isValid) {
                                binding.passSixthInput.requestFocus()
                            }
                            isFocusable = !isValid
                            isFocusableInTouchMode = !isValid
                        }

                        previousDigitEditText = binding.passFourthInput
                    }

                    binding.passSixthInput.apply {
                        isVisible = true

                        doAfterTextChanged {
                            if (this.text.toString().isNotEmpty()) {
                                binding.passFirstInput.apply {
                                    isCursorVisible = false
                                    requestFocus()
                                }

                                checkPasscode()
                            }
                        }

                        previousDigitEditText = binding.passFifthInput
                    }
                }

                binding.passFourthInput.layoutParams = params

                binding.passwordInput.isVisible = false
            }
        }
    }

    /**
     * Hides pin views.
     */
    private fun hidePins() {
        binding.passFirstInput.isVisible = false
        binding.passSecondInput.isVisible = false
        binding.passThirdInput.isVisible = false
        binding.passFourthInput.isVisible = false
        binding.passFifthInput.isVisible = false
        binding.passSixthInput.isVisible = false
    }

    /**
     * Hides error views.
     */
    private fun hideErrorElements() {
        binding.failedAttemptsText.isVisible = false
        binding.failedAttemptsErrorText.isVisible = false
        binding.logoutButton.isVisible = false
        binding.forgetPasscodeButton.isVisible = false
    }

    /**
     * Sets the text of the title depending on the current situation.
     */
    private fun setTitleText() {
        binding.titleText.text = getString(
            when {
                forgetPasscode -> R.string.settings_passcode_enter_password_title
                secondRound && setOrUnlockMode -> R.string.unlock_pin_title_2
                secondRound -> R.string.reset_pin_title_2
                setOrUnlockMode -> R.string.unlock_pin_title
                else -> R.string.reset_pin_title
            }
        )
    }

    /**
     * Sets the necessary listeners to all available views in passcode screen.
     */
    private fun setListeners() {
        binding.passcodeScrollView.setOnScrollChangeListener { _, _, _, _, _ ->
            binding.toolbarPasscode.elevation =
                if (binding.passcodeScrollView.canScrollVertically(-1)) {
                    dp2px(4F, resources.displayMetrics).toFloat()
                } else 0F
        }

        binding.logoutButton.setOnClickListener {
            Analytics.tracker.trackEvent(PasscodeLogoutButtonPressedEvent)
            viewModel.checkLogoutConfirmation()
        }

        binding.forgetPasscodeButton.setOnClickListener {
            Analytics.tracker.trackEvent(ForgotPasscodeButtonPressedEvent)
            forgetPasscode = true
            lifecycleScope.launch {
                initPasscodeScreen()
            }
        }

        binding.passcodeOptionsButton.setOnClickListener {
            showPasscodeOptions()
        }
    }

    /**
     * Saves the typed passcode after check if it is completed and confirms it:
     *  - After the first time the passcode was typed if it's unlocking.
     *  - After the second time the passcode was typed if it's setting or resetting.
     */
    private fun checkPasscode() {
        if (!isPassCodeComplete()) {
            return
        }

        val sb = if (secondRound) sbSecond else sbFirst

        when (passcodeType) {
            PIN_4 -> savePin4(sb)
            PIN_6 -> savePin6(sb)
            PIN_ALPHANUMERIC -> sb.append(binding.passwordInput.text)
        }

        when {
            secondRound -> confirmPasscode()
            mode == UNLOCK_MODE -> lifecycleScope.launch {
                confirmUnlockPasscode()
            }

            else -> {
                secondRound = true
                clearTypedPasscode()
                binding.passcodeOptionsButton.isVisible = false
            }
        }
    }

    /**
     * Saves the typed pin if passcode type is PIN_4.
     *
     * @param sb StringBuilder in which the pin has to be saved.
     */
    private fun savePin4(sb: StringBuilder) {
        sb.apply {
            append(binding.passFirstInput.text)
            append(binding.passSecondInput.text)
            append(binding.passThirdInput.text)
            append(binding.passFourthInput.text)
        }
    }

    /**
     * Saves the typed pin if passcode type is PIN_6.
     *
     * @param sb StringBuilder in which the pin has to be saved.
     */
    private fun savePin6(sb: StringBuilder) {
        savePin4(sb)
        sb.apply {
            append(binding.passFifthInput.text)
            append(binding.passSixthInput.text)
        }
    }

    /**
     * Confirms the passcode after type it the second time if setting or resetting.
     * Updates the passcode in DB and finishes if successful.
     * Shows an error if not successful.
     */
    private fun confirmPasscode() {
        if (sbFirst.toString() == sbSecond.toString()) {
            scope.launch {
                passcodePreferenceWrapper.setPasscodeEnabled(true)
                passcodePreferenceWrapper.setPasscodeLockType(passcodeType)
                passcodePreferenceWrapper.setPasscode(sbFirst.toString())

                setResult(RESULT_OK)
                finish()
            }
        } else {
            clearTypedPasscode()
            sbSecond.clear()
            binding.doNotMatchWarning.isVisible = true
        }
    }

    /**
     * Confirms the passcode after type it the first time if unlocking.
     * Updates the passcode behaviour, failed attempts and finishes if successful.
     * Shows an error and increments failed attempts if not successful.
     */
    private suspend fun confirmUnlockPasscode() {
        if (sbFirst.toString() == passcodePreferenceWrapper.getPasscode()) {
            skipPasscode()
        } else {
            sbFirst.clear()
            incrementAttempts()
            clearTypedPasscode()
            showAttemptsError()
        }
    }

    /**
     * Skips successfully the passcode screen because the passcode or the password
     * was correctly introduced.
     */
    private fun skipPasscode() {
        resetAttempts()
        clearFocus()
        finish()
    }

    /**
     * Checks if the typed password is the same as the current logged in account.
     */
    private suspend fun checkPassword() {
        val typedPassword = binding.passwordField.text.toString()

        if (passcodePreferenceWrapper.checkPassword(typedPassword)) {
            skipPasscode()
        } else {
            passwordAlreadyTyped = true
            incrementAttempts()
            binding.passwordField.text?.clear()
            showAttemptsError()
        }
    }

    /**
     * Shows the right attempts error depending on the number of failed attempts.
     */
    private fun showAttemptsError() {
        binding.failedAttemptsText.apply {
            isVisible = true
            text = resources.getQuantityString(
                R.plurals.passcode_lock_alert_attempts,
                attempts,
                attempts
            )
        }

        when {
            attempts == MAX_ATTEMPTS -> {
                binding.passcodeParentView.isEnabled = false
                hideKeyboardView(this, currentFocus, 0)
                logout()
            }

            attempts >= MIN_ATTEMPTS_TO_SHOW_WARNING -> {
                binding.failedAttemptsErrorText.isVisible = true
                binding.logoutButton.isVisible = true
                binding.forgetPasscodeButton.isVisible = !forgetPasscode
            }

            attempts > 0 -> {
                binding.failedAttemptsErrorText.isVisible = false
                binding.logoutButton.isVisible = true
                binding.forgetPasscodeButton.isVisible = !forgetPasscode
            }

            else -> {
                binding.failedAttemptsErrorText.isVisible = false
                binding.logoutButton.isVisible = false
                binding.forgetPasscodeButton.isVisible = !forgetPasscode
            }
        }
    }

    /**
     * Checks if all the fields are filled in before confirm the passcode.
     */
    private fun isPassCodeComplete(): Boolean {
        when (passcodeType) {
            PIN_4 -> {
                return binding.passFirstInput.length() == 1
                        && binding.passSecondInput.length() == 1
                        && binding.passThirdInput.length() == 1
                        && binding.passFourthInput.length() == 1
            }

            PIN_6 -> {
                return binding.passFirstInput.length() == 1
                        && binding.passSecondInput.length() == 1
                        && binding.passThirdInput.length() == 1
                        && binding.passFourthInput.length() == 1
                        && binding.passFifthInput.length() == 1
                        && binding.passSixthInput.length() == 1
            }

            PIN_ALPHANUMERIC -> {
                return binding.passwordInput.text.isNotEmpty()
            }
        }

        return false
    }

    /**
     * Clears the passcode fields.
     */
    private fun clearTypedPasscode() {
        binding.passFirstInput.text?.clear()
        binding.passSecondInput.text?.clear()
        binding.passThirdInput.text?.clear()
        binding.passFourthInput.text?.clear()
        binding.passFifthInput.text?.clear()
        binding.passSixthInput.text?.clear()

        binding.passwordInput.text.clear()

        setTitleText()

        if (passcodeType == PIN_ALPHANUMERIC) {
            binding.passwordInput.requestFocus()
        } else {
            binding.passFirstInput.requestFocus()
        }
    }

    /**
     * Opens the bottom sheet dialog to change the passcode type.
     */
    private fun showPasscodeOptions() {
        if (passcodeOptionsBottomSheetDialogFragment.isBottomSheetDialogShown()) return

        passcodeOptionsBottomSheetDialogFragment =
            PasscodeOptionsBottomSheetDialogFragment.newInstance(passcodeType)
        passcodeOptionsBottomSheetDialogFragment?.show(
            supportFragmentManager,
            passcodeOptionsBottomSheetDialogFragment?.tag
        )
    }

    /**
     * Updates the passcode screen after change the type.
     *
     * @param passcodeType New passcode type.
     */
    suspend fun setPasscodeType(passcodeType: String) {
        this.passcodeType = passcodeType
        initPasscodeScreen()
        clearTypedPasscode()

        Handler(Looper.getMainLooper()).postDelayed({
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
                currentFocus,
                InputMethodManager.SHOW_IMPLICIT
            )
        }, 1000)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home)
            onBackPressedDispatcher.onBackPressed()

        return super.onOptionsItemSelected(item)
    }

    private fun clearFocus() {
        with(binding) {
            listOf(
                passFirstInput,
                passSecondInput,
                passThirdInput,
                passFourthInput,
                passFifthInput,
                passSixthInput,
            ).forEach { it.clearFocus() }
        }
    }

    override fun onUserLeaveHint() {
        if (mode != UNLOCK_MODE) {
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(SECOND_ROUND, secondRound)

        if (secondRound) {
            outState.putString(SB_FIRST, sbFirst.toString())
        }

        outState.putInt(ATTEMPTS, attempts)
        outState.putString(PASSCODE_TYPE, passcodeType)
        outState.putBoolean(IS_CONFIRM_LOGOUT_SHOWN, isConfirmLogoutDialogShown)
        outState.putBoolean(FINGERPRINT_ENABLED, fingerprintEnabled)
        outState.putBoolean(FINGERPRINT_SKIPPED, fingerprintSkipped)
        outState.putBoolean(FORGET_PASSCODE, forgetPasscode)
        outState.putBoolean(PASSWORD_ALREADY_TYPED, passwordAlreadyTyped)

        super.onSaveInstanceState(outState)
    }

    /**
     * Shows the fingerprint unlock dialog.
     */
    private fun showFingerprintUnlock() {
        biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    Timber.w("Error: $errString")

                    when (errorCode) {
                        ERROR_USER_CANCELED -> {
                            finish()
                        }

                        else -> {
                            fingerprintSkipped = true
                            lifecycleScope.launch { initPasscodeScreen() }
                        }
                    }
                }

                override fun onAuthenticationSucceeded(
                    result: AuthenticationResult,
                ) {
                    super.onAuthenticationSucceeded(result)
                    Timber.d("Fingerprint unlocked")
                    unlockViewModel.unlockWithBiometrics()
                    finish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Timber.w("Authentication failed")
                }
            })

        if (!this::promptInfo.isInitialized) {
            promptInfo = PromptInfo.Builder()
                .setTitle(getString(R.string.title_unlock_fingerprint))
                .setNegativeButtonText(getString(R.string.action_use_passcode))
                .setAllowedAuthenticators(BIOMETRIC_STRONG)
                .build()
        }

        Analytics.tracker.trackEvent(PasscodeBiometricUnlockDialogEvent)
        biometricPrompt.authenticate(promptInfo, CryptoObject(getCipher()))
    }

    /**
     * Gets the secret key to encrypt the authentication.
     */
    private fun getSecretKey(): SecretKey {
        if (!this::keyStore.isInitialized) {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        }

        if (!this::keyGenerator.isInitialized) {
            keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEY_STORE
            ).apply {
                init(
                    KeyGenParameterSpec.Builder(
                        KEY_NAME,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .setUserAuthenticationRequired(true)
                        .build()
                )

                generateKey()
            }
        }

        // Before the keystore can be accessed, it must be loaded.
        keyStore.load(null)

        return keyStore.getKey(KEY_NAME, null) as SecretKey
    }

    /**
     * Gets the Cipher object to encrypt the authentication.
     */
    private fun getCipher(): Cipher {
        if (!this::cipher.isInitialized) {
            val secretKey = getSecretKey()

            cipher = Cipher.getInstance(
                KeyProperties.KEY_ALGORITHM_AES + "/"
                        + KeyProperties.BLOCK_MODE_CBC + "/"
                        + KeyProperties.ENCRYPTION_PADDING_PKCS7
            ).apply { init(Cipher.ENCRYPT_MODE, secretKey) }
        }

        return cipher
    }

    /**
     * Checks if fingerprint lock should be shown.
     *
     * @return True if fingerprint lock should be shown, false otherwise.
     */
    private fun shouldShowFingerprintLock(): Boolean =
        !fingerprintSkipped && fingerprintEnabled && BiometricManager.from(this)
            .canAuthenticate(BIOMETRIC_STRONG) == BIOMETRIC_SUCCESS

}
