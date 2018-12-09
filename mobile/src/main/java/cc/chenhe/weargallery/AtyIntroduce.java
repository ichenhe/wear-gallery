package cc.chenhe.weargallery;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;

public class AtyIntroduce extends IntroActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setButtonBackVisible(false);

        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED)
            addSlide(new SimpleSlide.Builder()
                    .title(R.string.intro_permission_title)
                    .description(R.string.intro_permission_content)
                    .background(R.color.slide_first)
                    .backgroundDark(R.color.slide_first_dark)
                    .permission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    .build());

        addSlide(new SimpleSlide.Builder()
                .title(R.string.intro_install_title)
                .description(R.string.intro_install_content)
                .background(R.color.slide_second)
                .backgroundDark(R.color.slide_second_dark)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title(R.string.intro_auto_run_title)
                .description(R.string.intro_auto_run_content)
                .background(R.color.slide_third)
                .backgroundDark(R.color.slide_third_dark)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title(R.string.intro_os_title)
                .description(R.string.intro_os_content)
                .background(R.color.slide_fourth)
                .backgroundDark(R.color.slide_fourth_dark)
                .build());
    }

}
