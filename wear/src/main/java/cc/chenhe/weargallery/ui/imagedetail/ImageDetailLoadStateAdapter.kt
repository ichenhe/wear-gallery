package cc.chenhe.weargallery.ui.imagedetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import cc.chenhe.weargallery.databinding.PagerItemImageDetailLoadStateBinding

class ImageDetailLoadStateAdapter(
    private val retry: () -> Unit
) : LoadStateAdapter<ImageDetailLoadStateAdapter.LoadStateVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadStateVH {
        val b = PagerItemImageDetailLoadStateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LoadStateVH(b)
    }

    // FIXME: This is a temporary fix.
    //  For unknown reasons, the additional page is always loaded during the init loading if the cache is empty.
    //  If we display the loading status, The page automatically scroll to the end of the first page.
    //  Discussion: https://stackoverflow.com/questions/66627912/android-paging-3-experiencing-flickers-glitches-or-jumps-in-position-when-scr
    override fun displayLoadStateAsItem(loadState: LoadState): Boolean {
        return loadState is LoadState.Error
    }

    override fun onBindViewHolder(holder: LoadStateVH, loadState: LoadState) {
        holder.bind(loadState)
    }

    inner class LoadStateVH(private val binding: PagerItemImageDetailLoadStateBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(loadState: LoadState) {
            binding.retryGroup.isVisible = loadState is LoadState.Error
            binding.loadingGroup.isVisible = loadState is LoadState.Loading
            binding.retryButton.setOnClickListener {
                retry.invoke()
            }
        }
    }
}