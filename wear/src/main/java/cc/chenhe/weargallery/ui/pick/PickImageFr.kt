package cc.chenhe.weargallery.ui.pick

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.recyclerview.widget.GridLayoutManager
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.ui.BaseListAdapter
import cc.chenhe.weargallery.databinding.FrPickImageBinding
import cc.chenhe.weargallery.uilts.shouldShowEmptyLayout
import cc.chenhe.weargallery.wearvision.dialog.AlertDialog
import org.koin.androidx.viewmodel.ext.android.viewModel

class PickImageFr : Fragment() {

    interface OnPickListener {
        fun onPick(image: Image)
    }

    private val model: PickImageViewModel by viewModel()
    private lateinit var binding: FrPickImageBinding
    private lateinit var adapter: PickImageAdapter

    var onPickListener: OnPickListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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

        binding.pickImageHeader.setOnClickListener {
            val folders = model.localFolderImages.value ?: return@setOnClickListener
            val titles = List(folders.size + 1) { i ->
                if (i == 0) requireContext().getString(R.string.pick_image_all) else folders[i - 1].bucketName
            }
            val ids = List(folders.size + 1) { i ->
                if (i == 0) PickImageViewModel.BUCKET_ALL else folders[i - 1].bucketId
            }
            var currentChoice = ids.indexOf(model.currentBucketId.value)
            if (currentChoice == -1) {
                currentChoice = 0
            }
            AlertDialog(requireContext()).apply {
                setTitle(R.string.pick_image_folder)
                setSingleChoiceItems(titles.toTypedArray(), currentChoice,
                        DialogInterface.OnClickListener { _, _ -> dismiss() })
                setOnDismissListener {
                    val newId = ids.getOrElse(getCheckedItemPosition()) { currentChoice }
                    if (newId != currentChoice) {
                        model.setBucketId(newId)
                    }
                }
            }.show()
        }
    }

}