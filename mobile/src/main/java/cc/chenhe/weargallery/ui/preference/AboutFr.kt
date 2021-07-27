package cc.chenhe.weargallery.ui.preference

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import cc.chenhe.weargallery.common.util.getVersionCode
import cc.chenhe.weargallery.common.util.getVersionName
import cc.chenhe.weargallery.databinding.FrAboutBinding

class AboutFr : Fragment() {

    private lateinit var binding: FrAboutBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FrAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = findNavController()
        val conf = AppBarConfiguration(navController.graph)
        binding.toolbar.setupWithNavController(navController, conf)

        binding.version.text =
            getVersionName(requireContext()) + " (" + getVersionCode(requireContext()) + ")"

        binding.license.setOnClickListener {
            findNavController().navigate(AboutFrDirections.actionAboutFrToLicenseFr())
        }

        binding.telegram.setOnClickListener {
            openInBrowser("https://" + binding.telegram.text)
        }
        binding.github.setOnClickListener {
            openInBrowser("https://" + binding.github.text)
        }
    }

    private fun openInBrowser(url: String) {
        try {
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(url)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
        }
    }
}