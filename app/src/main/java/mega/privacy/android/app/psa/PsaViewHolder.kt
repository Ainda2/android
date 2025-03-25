package mega.privacy.android.app.psa

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.TextUtils
import android.view.View
import androidx.core.view.isVisible
import coil.load
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.databinding.PsaLayoutBinding
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.psa.Psa

/**
 * The view holder for normal PSA view, implementing the display logic of PSA.
 */
class PsaViewHolder(
    psaLayout: View,
    private val dismissPsa: (Int) -> Unit,
) {
    private val binding = PsaLayoutBinding.bind(psaLayout)
    private var bound = false

    /**
     * Bind view for the PSA.
     *
     * @param psa the PSA to display
     */
    fun bind(psa: Psa) {
        bound = true
        binding.root.visibility = View.VISIBLE

        binding.root.setBackgroundColor(
            if (Util.isDarkMode(binding.root.context)) {
                getColorForElevation(binding.root.context, 8f)
            } else {
                Color.WHITE
            }
        )

        if (!TextUtils.isEmpty(psa.imageUrl)) {
            binding.image.visibility = View.VISIBLE
            binding.image.load(Uri.parse(psa.imageUrl))
        }

        binding.title.text = psa.title
        binding.text.text = psa.text

        if (!TextUtils.isEmpty(psa.positiveText) && !TextUtils.isEmpty(psa.positiveLink)) {
            binding.leftButton.text = psa.positiveText
            binding.leftButton.setOnClickListener {
                val context = binding.root.context
                val intent = Intent(context, WebViewActivity::class.java)
                intent.data = Uri.parse(psa.positiveLink)
                context.startActivity(intent)
                onDismiss(psa.id)
            }

            binding.rightButton.visibility = View.VISIBLE
            binding.rightButton.setOnClickListener { onDismiss(psa.id) }
        } else {
            binding.leftButton.setText(R.string.general_dismiss)
            binding.leftButton.setOnClickListener { onDismiss(psa.id) }
        }
    }

    /**
     * Toggle visibility of the PSA view.
     *
     * @param shouldShow if the PSA view should be visible
     */
    fun toggleVisible(shouldShow: Boolean) {
        binding.root.visibility = if (shouldShow && bound) View.VISIBLE else View.GONE
    }

    /**
     * Check if the PSA view is visible.
     *
     * @return if the PSA view is visible
     */
    fun visible() = binding.root.isVisible

    /**
     * Get height of the PSA view.
     *
     * @return height of the PSA view
     */
    fun psaLayoutHeight() = binding.root.measuredHeight

    /**
     * Hide PSA view and dismiss it in server.
     *
     * @param id the id of the PSA
     */
    private fun onDismiss(id: Int) {
        binding.root.visibility = View.GONE
        dismissPsa(id)
        bound = false
    }
}
