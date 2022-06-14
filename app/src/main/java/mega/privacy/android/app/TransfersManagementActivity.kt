package mega.privacy.android.app

import android.content.*
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.transferWidget.TransfersWidget
import mega.privacy.android.app.constants.BroadcastConstants.*
import mega.privacy.android.app.constants.EventConstants.EVENT_SCANNING_TRANSFERS_CANCELLED
import mega.privacy.android.app.constants.EventConstants.EVENT_SHOW_SCANNING_TRANSFERS_DIALOG
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.ManagerActivity.PENDING_TAB
import mega.privacy.android.app.main.ManagerActivity.TRANSFERS_TAB
import mega.privacy.android.app.usecase.GetNetworkConnectionUseCase
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import javax.inject.Inject

/**
 * Activity for showing concrete UI items related to transfers management.
 */
@AndroidEntryPoint
open class TransfersManagementActivity : PasscodeActivity() {

    companion object {
        const val IS_CANCEL_TRANSFERS_SHOWN = "IS_CANCEL_TRANSFERS_SHOWN"
    }

    @Inject
    lateinit var getNetworkConnectionUseCase: GetNetworkConnectionUseCase

    private var transfersWidget: TransfersWidget? = null

    private var scanningTransfersDialog: AlertDialog? = null
    private var cancelTransfersDialog: AlertDialog? = null

    /**
     * Broadcast to update the transfers widget.
     */
    private var transfersUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateTransfersWidget(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupObservers()

        if (savedInstanceState != null) {
            when {
                savedInstanceState.getBoolean(IS_CANCEL_TRANSFERS_SHOWN, false) -> {
                    showCancelTransfersDialog()
                }
                transfersManagement.shouldShowScanningTransfersDialog() -> {
                    showScanningTransfersDialog()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(IS_CANCEL_TRANSFERS_SHOWN, isAlertDialogShown(cancelTransfersDialog))
        super.onSaveInstanceState(outState)
    }

    /**
     * Registers the transfers BroadcastReceivers and observers.
     */
    private fun setupObservers() {
        getNetworkConnectionUseCase.getConnectionUpdates()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { online ->
                    if (online) {
                        transfersManagement.resetNetworkTimer()
                    } else {
                        transfersManagement.startNetworkTimer()
                    }
                },
                onError = { error -> logError("Network update error", error) }
            )
            .addTo(composite)

        registerReceiver(
            transfersUpdateReceiver,
            IntentFilter(BROADCAST_ACTION_INTENT_TRANSFER_UPDATE)
        )

        LiveEventBus.get(EVENT_SHOW_SCANNING_TRANSFERS_DIALOG, Boolean::class.java)
            .observe(this) { show ->
                when {
                    show && transfersManagement.shouldShowScanningTransfersDialog() -> {
                        showScanningTransfersDialog()
                    }
                    !show && !transfersManagement.shouldShowScanningTransfersDialog() -> {
                        scanningTransfersDialog?.dismiss()
                    }
                }
            }
    }

    /**
     * Sets a view as transfers widget.
     *
     * @param transfersWidgetLayout RelativeLayout view to set
     */
    protected fun setTransfersWidgetLayout(transfersWidgetLayout: RelativeLayout) {
        setTransfersWidgetLayout(transfersWidgetLayout, null)
    }

    /**
     * Sets a view as transfers widget.
     *
     * @param transfersWidgetLayout RelativeLayout view to set
     * @param context               Current Context.
     *                              Only used to identify if the view belongs to the ManagerActivity.
     */
    protected fun setTransfersWidgetLayout(
        transfersWidgetLayout: RelativeLayout,
        context: Context?
    ) {
        transfersWidget =
            TransfersWidget(context ?: this, megaApi, transfersWidgetLayout, transfersManagement)

        transfersWidgetLayout.findViewById<View>(R.id.transfers_button)
            .setOnClickListener {
                if (context is ManagerActivity) {
                    context.drawerItem = DrawerItem.TRANSFERS
                    context.selectDrawerItem(context.drawerItem)
                } else {
                    openTransfersSection()
                }

                if (transfersManagement.isOnTransferOverQuota()) {
                    transfersManagement.setHasNotToBeShowDueToTransferOverQuota(true)
                }
            }
    }

    /**
     * Defines the click action of the transfers widget.
     * Launches an Intent to navigate to In progress tab in Transfers section.
     */
    protected fun openTransfersSection() {
        if (megaApi.isLoggedIn == 0 || dbH.credentials == null) {
            LogUtil.logWarning("No logged in, no action.")
            return
        }

        startActivity(
            Intent(this, ManagerActivity::class.java)
                .setAction(ACTION_SHOW_TRANSFERS)
                .putExtra(TRANSFERS_TAB, PENDING_TAB)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )

        finish()
    }

    /**
     * Updates the state of the transfers widget when the correspondent LocalBroadcast has been received.
     *
     * @param intent    Intent received in the LocalBroadcast
     */
    protected fun updateTransfersWidget(intent: Intent?) {
        if (intent == null || transfersWidget == null) {
            return
        }

        val transferType = intent.getIntExtra(TRANSFER_TYPE, EXTRA_BROADCAST_INVALID_VALUE)

        if (transferType == EXTRA_BROADCAST_INVALID_VALUE) {
            transfersWidget?.update()
        } else {
            transfersWidget?.update(transferType)
        }
    }

    /**
     * Shows a scanning transfers dialog.
     */
    private fun showScanningTransfersDialog() {
        if (isActivityInBackground || isAlertDialogShown(scanningTransfersDialog)) {
            return
        }

        scanningTransfersDialog = MaterialAlertDialogBuilder(this)
            .setView(R.layout.dialog_scanning_transfers)
            .setPositiveButton(
                StringResourcesUtils.getString(R.string.cancel_transfers)
            ) { _, _ ->
                showCancelTransfersDialog()
            }
            .create()
            .apply {
                setCancelable(false)
                setCanceledOnTouchOutside(false)
                show()
            }
    }

    /**
     * Shows a confirmation dialog before cancel all scanning transfers.
     */
    private fun showCancelTransfersDialog() {
        if (isActivityInBackground || isAlertDialogShown(cancelTransfersDialog)) {
            return
        }

        cancelTransfersDialog = MaterialAlertDialogBuilder(this)
            .setTitle(StringResourcesUtils.getString(R.string.cancel_transfers))
            .setMessage(StringResourcesUtils.getString(R.string.warning_cancel_transfers))
            .setPositiveButton(
                StringResourcesUtils.getString(R.string.button_proceed)
            ) { _, _ ->
                LiveEventBus.get(EVENT_SCANNING_TRANSFERS_CANCELLED, Boolean::class.java).post(true)
                transfersManagement.cancelScanningTransfers()
                Util.showSnackbar(
                    this,
                    StringResourcesUtils.getString(R.string.transfers_cancelled)
                )
            }
            .setNegativeButton(StringResourcesUtils.getString(R.string.general_dismiss), null)
            .create()
            .apply {
                setCancelable(false)
                setCanceledOnTouchOutside(false)
                show()
            }
    }

    override fun onResume() {
        super.onResume()

        if (transfersManagement.shouldShowScanningTransfersDialog()) {
            showScanningTransfersDialog()
        }

        transfersWidget?.update()
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(transfersUpdateReceiver)

        scanningTransfersDialog?.dismiss()
        cancelTransfersDialog?.dismiss()
    }

    /**
     * Updates the transfers widget.
     */
    fun updateTransfersWidget() {
        transfersWidget?.update()
    }

    /**
     * Updates the state of the transfers widget.
     */
    fun updateTransfersWidgetState() {
        transfersWidget?.updateState()
    }

    /**
     * Hides the transfers widget.
     */
    fun hideTransfersWidget() {
        transfersWidget?.hide()
    }

    /**
     * Gets the pending transfers.
     *
     * @return Pending transfers.
     */
    fun getPendingTransfers(): Int = transfersWidget?.pendingTransfers ?: 0
}