package cc.chenhe.weargallery.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cc.chenhe.weargallery.databinding.FrWhetherShowPhoneImageBinding
import cc.chenhe.weargallery.uilts.showPhoneImages

class WhetherShoePhoneImageFr : Fragment() {
    private lateinit var binding: FrWhetherShowPhoneImageBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FrWhetherShowPhoneImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.enable.setOnClickListener {
            showPhoneImages(requireContext(), shouldQuery = false, show = true)
        }
        binding.disable.setOnClickListener {
            showPhoneImages(requireContext(), shouldQuery = false, show = false)
        }
    }
}