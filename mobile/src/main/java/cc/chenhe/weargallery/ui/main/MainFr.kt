package cc.chenhe.weargallery.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import cc.chenhe.weargallery.databinding.FrMainBinding
import cc.chenhe.weargallery.ui.legacy.LegacyAty

class MainFr : Fragment() {

    private lateinit var binding: FrMainBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FrMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.legacy.setOnClickListener {
            startActivity(Intent(requireContext(), LegacyAty::class.java))
        }
        binding.preference.setOnClickListener {
            findNavController().navigate(MainFrDirections.actionMainFrToPreferenceFr())
        }
    }
}