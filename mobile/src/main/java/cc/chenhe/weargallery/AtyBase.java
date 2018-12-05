package cc.chenhe.weargallery;

import android.view.MenuItem;

import cc.chenhe.lib.weartools.activity.WTAppCompatActivity;

public abstract class AtyBase extends WTAppCompatActivity {

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}
