package mega.privacy.android.app.presentation.documentscanner

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.components.session.SessionContainer
import mega.privacy.android.app.extensions.enableEdgeToEdgeAndConsumeInsets
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import javax.inject.Inject

/**
 * An Activity that shows a screen where Users can configure some aspects of their scanned documents
 */
@AndroidEntryPoint
internal class SaveScannedDocumentsActivity : AppCompatActivity() {

    /**
     * Retrieves the Device Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * Handles the Passcode
     */
    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeAndConsumeInsets()
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            SessionContainer {
                OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                    PasscodeContainer(
                        passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                        content = { SaveScannedDocumentsScreen() },
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_CLOUD_DRIVE_PARENT_HANDLE = "EXTRA_CLOUD_DRIVE_PARENT_HANDLE"
        const val EXTRA_SCAN_PDF_URI = "EXTRA_SCAN_PDF_URI"
        const val EXTRA_SCAN_SOLO_IMAGE_URI = "EXTRA_SCAN_SOLO_IMAGE_URI"
    }
}