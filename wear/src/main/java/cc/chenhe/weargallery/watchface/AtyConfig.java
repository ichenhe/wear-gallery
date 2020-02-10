package cc.chenhe.weargallery.watchface;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import cc.chenhe.weargallery.R;

public class AtyConfig extends FragmentActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wf_aty_config);
        changeFragment();
    }

    private void changeFragment() {
        FrConfig config = FrConfig.create();
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.frame, config)
                .commit();
    }
}
