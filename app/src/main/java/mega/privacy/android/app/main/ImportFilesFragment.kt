package mega.privacy.android.app.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.constants.StringsConstants.INVALID_CHARACTERS
import mega.privacy.android.app.databinding.FragmentImportFilesBinding
import mega.privacy.android.app.main.adapters.ImportFilesAdapter
import mega.privacy.android.app.main.adapters.ImportFilesAdapter.OnImportFilesAdapterFooterListener
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ShareTextInfo
import mega.privacy.android.domain.entity.document.DocumentEntity

/**
 * Fragment for importing files.
 */
class ImportFilesFragment : Fragment(), OnImportFilesAdapterFooterListener {

    private lateinit var binding: FragmentImportFilesBinding

    private val viewModel: FileExplorerViewModel by activityViewModels()

    private var adapter: ImportFilesAdapter? = null
    private var uploadFragment: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentImportFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Sets up the view and observers.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupObservers()
        setupView()
        super.onViewCreated(view, savedInstanceState)
    }

    /**
     * Sets the elevation of the action bar.
     */
    override fun onResume() {
        super.onResume()
        changeActionBarElevation()
    }

    private fun setupObservers() {
        with(viewModel) {
            collectFlow(viewModel.uiState.mapNotNull { it.documents.takeIf { it.isNotEmpty() } }) {
                showFilesInfo(it)
            }
            textInfo.observe(viewLifecycleOwner) { info: ShareTextInfo? ->
                info?.let { showImportingTextInfo(it) }
            }
        }
    }

    /**
     * Shows the view when it is importing text.
     *
     * @param info ShareTextInfo containing all the required info to share the text.
     */
    private fun showImportingTextInfo(info: ShareTextInfo) {
        var setNames = true

        if (adapter == null) {
            adapter = ImportFilesAdapter(requireActivity(), info, nameFiles)
            setNames = false
        }

        val headerText: String = if (info.isUrl) {
            getString(R.string.file_properties_shared_folder_public_link_name)
        } else {
            resources.getQuantityString(R.plurals.general_num_files, 1)
        }

        setupAdapter(setNames, headerText)
    }

    /**
     * Shows the view when it is importing files.
     *
     * @param documents List of DocumentEntity containing all the required info to share the files.
     */
    private fun showFilesInfo(documents: List<DocumentEntity>) {
        var setNames = true

        if (adapter == null) {
            adapter = ImportFilesAdapter(
                requireActivity(),
                documents,
                nameFiles
            )
            setNames = false
        }

        val headerText =
            resources.getQuantityString(R.plurals.general_num_files, documents.size)
        setupAdapter(setNames, headerText)
    }

    /**
     * Sets the adapter.
     *
     * @param setNames   True if should set the file names to the adapter, false otherwise.
     * @param headerText The text to show as the header of the content.
     */
    private fun setupAdapter(setNames: Boolean, headerText: String) {
        binding.contentText.text = headerText

        if (setNames) {
            adapter?.setImportNameFiles(nameFiles)
        }

        binding.fileListView.adapter = adapter
        adapter?.setFooterListener(this)
    }

    private fun setupView() {
        with(binding) {
            fileListView.layoutManager = LinearLayoutManager(requireContext())
            fileListView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    changeActionBarElevation()
                }
            })
        }
    }

    /**
     * Checks if all the files to import have a right name.
     * - If so, shows the fragment which the user chose to import.
     * - If not, shows an error warning.
     *
     * @param fragment The fragment chosen by the user.
     */
    private fun confirmImport(fragment: Int) {
        adapter?.updateCurrentFocusPosition(binding.fileListView)
        uploadFragment = -1

        val nameFiles = nameFiles
        var emptyNames = 0
        var wrongNames = 0

        for (name in nameFiles.values) {
            if (name.trim { it <= ' ' }.isEmpty()) {
                emptyNames++
            } else if (Constants.NODE_NAME_REGEX.matcher(name).find()) {
                wrongNames++
            }
        }

        if (wrongNames > 0 || emptyNames > 0) {
            val warning: String = when {
                emptyNames > 0 && wrongNames > 0 -> {
                    getString(R.string.general_incorrect_names)
                }

                emptyNames > 0 -> {
                    resources.getQuantityString(R.plurals.empty_names, emptyNames)
                }

                else -> {
                    getString(
                        R.string.invalid_characters_defined,
                        INVALID_CHARACTERS
                    )
                }
            }

            (requireActivity() as FileExplorerActivity).showSnackbar(warning)
        } else {
            (requireActivity() as FileExplorerActivity).chooseFragment(fragment)
        }
    }

    /**
     * Changes action bar elevation.
     */
    fun changeActionBarElevation() {
        (requireActivity() as FileExplorerActivity).changeActionBarElevation(
            binding.fileListView.canScrollVertically(Constants.SCROLLING_UP_DIRECTION),
            FileExplorerActivity.IMPORT_FRAGMENT
        )
    }

    private val nameFiles: HashMap<String, String>
        get() = HashMap(viewModel.uiState.value.namesByOriginalName)

    /**
     * Handle clicking cloud drive option
     */
    override fun onClickCloudDriveButton() {
        confirmImport(FileExplorerActivity.CLOUD_FRAGMENT)
    }

    /**
     * Handle clicking chat option
     */
    override fun onClickChatButton() {
        confirmImport(FileExplorerActivity.CHAT_FRAGMENT)
    }


    companion object {
        /**
         * Creates a new instance of [ImportFilesFragment].
         *
         * @return The new instance.
         */
        fun newInstance(): ImportFilesFragment = ImportFilesFragment()
    }
}