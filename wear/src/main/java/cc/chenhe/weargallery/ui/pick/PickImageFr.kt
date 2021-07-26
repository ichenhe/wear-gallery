package cc.chenhe.weargallery.ui.pick

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.bean.ImageFolder
import cc.chenhe.weargallery.common.ui.BaseListAdapter
import cc.chenhe.weargallery.databinding.FrPickImageBinding
import cc.chenhe.weargallery.uilts.shouldShowEmptyLayout
import me.chenhe.wearvision.dialog.AlertDialog
import org.koin.androidx.viewmodel.ext.android.viewModel

class PickImageFr : Fragment() {

    interface OnPickListener {
        fun onPick(image: Image)
    }

    private val model: PickImageViewModel by viewModel()
    private lateinit var binding: FrPickImageBinding
    private lateinit var adapter: PickImageAdapter

    var onPickListener: OnPickListener? = null
    private var folders: List<ImageFolder>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FrPickImageBinding.inflate(inflater, container, false).also {
            it.lifecycleOwner = this.viewLifecycleOwner
            it.model = model
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PickImageAdapter().apply {
            itemClickListener = object : BaseListAdapter.SimpleItemClickListener() {
                override fun onItemClick(view: View, position: Int) {
                    super.onItemClick(view, position)
                    onPickListener?.onPick(adapter.getItemData(position))
                }
            }
        }

        binding.imagesRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = this@PickImageFr.adapter
        }

        model.data.observe(viewLifecycleOwner) { data ->
            adapter.submitList(data.data)
            if (shouldShowEmptyLayout(data)) {
                binding.emptyLayout.viewStub?.inflate()
            }
        }

        model.folders.observe(viewLifecycleOwner) {
            folders = it
        }

        binding.pickImageHeader.setOnClickListener {
            val folders = this.folders ?: return@setOnClickListener
            // Build selection items
            val titles = List(folders.size + 1) { i ->
                if (i == 0) requireContext().getString(R.string.pick_image_all) else folders[i - 1].name
            }
            val ids = List(folders.size + 1) { i ->
                if (i == 0) PickImageViewModel.BUCKET_ALL else folders[i - 1].id
            }
            var currentChoice = ids.indexOf(model.currentBucketId.value)
            if (currentChoice == -1) {
                currentChoice = 0
            }
            // show selection dialog
            AlertDialog(requireContext()).apply {
                setTitle(R.string.pick_image_folder)
                setSingleChoiceItems(titles.toTypedArray(), currentChoice) { _, _ -> dismiss() }
                setOnDismissListener {
                    val newId = ids.getOrElse(getCheckedItemPosition()) { currentChoice }
                    if (newId != model.currentBucketId.value) {
                        model.setBucketId(newId)
                    }
                }
            }.show()
        }
    }

}