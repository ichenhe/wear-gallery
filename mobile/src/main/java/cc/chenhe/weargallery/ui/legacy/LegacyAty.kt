package cc.chenhe.weargallery.ui.legacy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import cc.chenhe.weargallery.databinding.AtyLegacyBinding
import cc.chenhe.weargallery.utils.resetStatusBarTextColor


class LegacyAty : AppCompatActivity() {

    private lateinit var binding: AtyLegacyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = AtyLegacyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        resetStatusBarTextColor(binding.root)
    }
}