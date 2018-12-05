package cc.chenhe.weargallery.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;

import cc.chenhe.weargallery.R;
import cc.chenhe.weargallery.ui.AlertDialog;

public class AtyIntroduce extends IntroActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setButtonBackVisible(false);
        setPagerIndicatorVisible(false);

        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission
                .READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }

        addSlide(new SimpleSlide.Builder()
                .title(R.string.intro_gif_title)
                .description(R.string.intro_gif_content)
                .background(R.color.slide_first)
                .backgroundDark(R.color.slide_first_dark)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title(R.string.intro_lan_title)
                .description(R.string.intro_lan_content)
                .background(R.color.slide_second)
                .backgroundDark(R.color.slide_second_dark)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title(R.string.intro_wf_title)
                .description(R.string.intro_wf_content)
                .background(R.color.slide_third)
                .backgroundDark(R.color.slide_third_dark)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title(R.string.intro_enjoy_title)
                .description(R.string.intro_enjoy_content)
                .background(R.color.slide_fourth)
                .backgroundDark(R.color.slide_fourth_dark)
                .build());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                //未授权
                new AlertDialog.Builder(this)
                        .setTitle(R.string.main_local_err_permission_title)
                        .setMessage(R.string.main_local_err_permission_content)
                        .setPositiveButtonListener(new AlertDialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (Build.VERSION.SDK_INT >= 23)
                                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
                            }
                        })
                        .setNegativeButtonListener(new AlertDialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .show();
            }
        }
    }
}
