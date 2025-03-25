package mega.privacy.android.app.presentation.settings.chat.imagequality

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.extensions.enableEdgeToEdgeAndConsumeInsets
import mega.privacy.android.app.presentation.container.AppContainerWrapper
import mega.privacy.android.app.presentation.security.PasscodeCheck
import javax.inject.Inject

/**
 * Activity which allows to change the chat image quality setting.
 */
@AndroidEntryPoint
class SettingsChatImageQualityActivity : AppCompatActivity() {

    @Inject
    lateinit var appContainerWrapper: AppContainerWrapper

    @Inject
    lateinit var passCodeFacade: PasscodeCheck

    @SuppressLint("CommitTransaction")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeAndConsumeInsets()
        super.onCreate(savedInstanceState)
        appContainerWrapper.setPasscodeCheck(passCodeFacade)
        setContentView(R.layout.settings_activity)
        setSupportActionBar(findViewById(R.id.settings_toolbar))
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsChatImageQualityFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }

            else -> {
                super.onOptionsItemSelected(item)
            }
        }
}
