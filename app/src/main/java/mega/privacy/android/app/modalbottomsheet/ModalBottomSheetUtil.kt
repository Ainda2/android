package mega.privacy.android.app.modalbottomsheet

import mega.privacy.android.shared.resources.R as sharedR
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilWrapper
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File

/**
 * Util object for modal bottom sheets.
 */
object ModalBottomSheetUtil {

    /**
     * Launches an intent to open a node in the apps installed in the device if any.
     *
     * @param context           Required Context.
     * @param node              MegaNode to open.
     * @param nodeDownloader    Download action to perform if the file type is not supported.
     */
    @JvmStatic
    fun BottomSheetDialogFragment?.openWith(
        context: Context,
        node: MegaNode?,
        megaNodeUtilWrapper: MegaNodeUtilWrapper,
        nodeDownloader: (() -> Unit)? = null,
    ): AlertDialog? {
        if (node == null) {
            Timber.w("Node is null")
            return null
        }
        val app = MegaApplication.getInstance()
        val megaApi = app.megaApi
        val mimeType = MimeTypeList.typeForName(node.name).type
        if (MimeTypeList.typeForName(node.name).isURL) {
            megaNodeUtilWrapper.manageURLNode(context, MegaApplication.getInstance().megaApi, node)
            return null
        }
        val mediaIntent = Intent(Intent.ACTION_VIEW)
        val localPath = FileUtil.getLocalFile(node)
        if (localPath != null) {
            val mediaFile = File(localPath)
            mediaIntent.setDataAndType(
                FileProvider.getUriForFile(
                    app,
                    Constants.AUTHORITY_STRING_FILE_PROVIDER,
                    mediaFile
                ), MimeTypeList.typeForName(node.name).type
            )
            mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            megaNodeUtilWrapper.setupStreamingServer()
            val url = megaApi.httpServerGetLocalLink(node)
            if (url == null) {
                Util.showSnackbar(
                    context,
                    context.getString(R.string.error_open_file_with)
                )
            } else {
                mediaIntent.setDataAndType(Uri.parse(url), mimeType)
            }
        }

        if (MegaApiUtils.isIntentAvailable(app, mediaIntent)) {
            mediaIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(mediaIntent)
        } else if (this != null && nodeDownloader != null && localPath == null) {
            return this.showCannotOpenFileDialog(context, nodeDownloader)
        } else {
            Util.showSnackbar(
                context,
                context.getString(R.string.intent_not_available_file)
            )
        }

        return null
    }


    /**
     * Shows a warning dialog informing a file cannot be opened because the type is not supported.
     *
     * @param context           Required context.
     * @param nodeDownloader    Download action to perform if the user confirms it.
     * @return The AlertDialog.
     */
    @JvmStatic
    fun BottomSheetDialogFragment.showCannotOpenFileDialog(
        context: Context,
        nodeDownloader: () -> Unit,
    ): AlertDialog =
        MaterialAlertDialogBuilder(context)
            .setTitle(getString(R.string.dialog_cannot_open_file_title))
            .setMessage(getString(R.string.dialog_cannot_open_file_text))
            .setPositiveButton(
                getString(R.string.context_download)
            ) { _, _ ->
                nodeDownloader()
                this.dismissAllowingStateLoss()
            }
            .setNegativeButton(getString(sharedR.string.general_dialog_cancel_button), null)
            .show()

    /**
     * Checks if a bottom sheet dialog fragment is shown.
     *
     * @return True if the bottom sheet is shown, false otherwise.
     */
    @JvmStatic
    fun BottomSheetDialogFragment?.isBottomSheetDialogShown(): Boolean =
        this?.isAdded == true
}