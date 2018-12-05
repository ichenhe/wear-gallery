package cc.chenhe.weargallery.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.wearable.view.drawer.WearableNavigationDrawer;

import cc.chenhe.lib.weartools.activity.WTFragmentActivity;
import cc.chenhe.weargallery.R;
import cc.chenhe.weargallery.common.CUtils;
import cc.chenhe.weargallery.fragment.FrLocalGallery;
import cc.chenhe.weargallery.fragment.FrMobileGallery;
import cc.chenhe.weargallery.fragment.FrSettings;
import cc.chenhe.weargallery.uilts.Settings;
import cc.chenhe.weargallery.uilts.Utils;

public class AtyMain extends WTFragmentActivity implements ViewPager.OnPageChangeListener {
    private static final int REQ_INTRO = 1;

    public static final int INDEX_LOCAL_GALLERY = 0;
    public static final int INDEX_PHONE_GALLERY = 1;
    public static final int INDEX_SETTINGS = 2;

    private Context context;
    private WearableNavigationDrawer wearableNavigationDrawer;
    private ViewPager viewPager;

    private Fragment[] fragments = new Fragment[3];

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aty_main);
        context = this;
        Utils.initFolder(context);
        wearableNavigationDrawer = findViewById(R.id.top_navigation_drawer);
        wearableNavigationDrawer.setShouldOnlyOpenWhenAtTop(true);
        wearableNavigationDrawer.setAdapter(new DrawerAdapter());
        wearableNavigationDrawer.peekDrawer();
        viewPager = findViewById(R.id.viewPager);
        viewPager.setOffscreenPageLimit(2);
        viewPager.addOnPageChangeListener(this);

        if (!checkPermission() || Settings.lastStartVersion(context) < CUtils.getVersionCode(context))
            startActivityForResult(new Intent(this, AtyIntroduce.class), REQ_INTRO);
        else
            initViewPager();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_INTRO) {
            if (checkPermission())
                initViewPager();
            else
                finish();
            Settings.lastStartVersion(context, CUtils.getVersionCode(context));
        }
    }

    private boolean checkPermission() {
        return !(Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest
                .permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED);
    }

    private void initViewPager() {
        viewPager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager()));
    }

    @Override
    public void onPageScrolled(int i, float v, int i1) {

    }

    @Override
    public void onPageSelected(int position) {
        wearableNavigationDrawer.setCurrentItem(position, false);
    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }

    private class DrawerAdapter extends WearableNavigationDrawer.WearableNavigationDrawerAdapter {

        private String text[] = {getString(R.string.drawer_local_gallery),
                getString(R.string.drawer_phone_gallery),
                getString(R.string.drawer_explore)};
        private int icon[] = {R.drawable.ic_drawer_watch,
                R.drawable.ic_drawer_phone,
                R.drawable.ic_drawer_explore};

        @Override
        public String getItemText(int pos) {
            return text[pos];
        }

        @Override
        public Drawable getItemDrawable(int pos) {
            return getResources().getDrawable(icon[pos]);
        }

        @Override
        public void onItemSelected(int pos) {
            int num = getSupportFragmentManager().getBackStackEntryCount();
            for (int i = 0; i < num; i++)
                getSupportFragmentManager().popBackStackImmediate();
            viewPager.setCurrentItem(pos);
        }

        @Override
        public int getCount() {
            return icon.length;
        }
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {

        ViewPagerAdapter(FragmentManager fm) {
            super(fm);
            fragments[INDEX_LOCAL_GALLERY] = new FrLocalGallery();
            fragments[INDEX_PHONE_GALLERY] = new FrMobileGallery();
            fragments[INDEX_SETTINGS] = new FrSettings();
        }

        @Override
        public Fragment getItem(int position) {
            return fragments[position];
        }

        @Override
        public int getCount() {
            return fragments.length;
        }
    }
}
