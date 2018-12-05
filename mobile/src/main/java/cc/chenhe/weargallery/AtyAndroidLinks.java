package cc.chenhe.weargallery;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;

import me.drakeet.multitype.Items;
import me.drakeet.support.about.AbsAboutActivity;
import me.drakeet.support.about.Card;
import me.drakeet.support.about.Category;
import me.drakeet.support.about.OnRecommendedClickedListener;
import me.drakeet.support.about.Recommended;
import me.drakeet.support.about.extension.JsonConverter;
import me.drakeet.support.about.extension.RecommendedLoaderDelegate;
import me.drakeet.support.about.provided.GlideImageLoader;

/**
 * Created by 晨鹤 on 2017/12/19.
 */

public class AtyAndroidLinks extends AbsAboutActivity implements OnRecommendedClickedListener {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setImageLoader(new GlideImageLoader());
        setOnRecommendedClickedListener(this);
    }

    @Override
    protected void onCreateHeader(@NonNull ImageView icon, @NonNull TextView slogan, @NonNull TextView version) {
        icon.setImageResource(R.mipmap.ico_app);
        slogan.setText(R.string.app_name);
        version.setText(BuildConfig.VERSION_NAME);
    }

    @Override
    protected void onItemsCreated(@NonNull Items items) {
        items.add(new Category(getString(R.string.about_thank_title)));
        items.add(new Card(getString(R.string.about_thank_content)));

        items.add(new Category(getString(R.string.about_feedback_title)));
        items.add(new Card(getString(R.string.about_feedback_content)));

        items.add(new Category(getString(R.string.about_alpha_title)));
        items.add(new Card(getString(R.string.about_alpha_content)));

        items.add(new Category(getString(R.string.links_introduction_title)));
        items.add(new Card(getString(R.string.links_introduction_content)));

        // Load more Recommended items from remote server asynchronously
        RecommendedLoaderDelegate.attach(this, items.size(), new JsonConverter() {
            @Nullable
            @Override
            public <T> T fromJson(@NonNull String json, @NonNull Class<T> classOfT) throws Exception {
                return JSON.parseObject(json, classOfT);
            }

            @NonNull
            @Override
            public <T> String toJson(@Nullable T src, @NonNull Class<T> classOfT) {
                return JSON.toJSONString(src);
            }
        });
    }


    @Override
    public boolean onRecommendedClicked(@NonNull View itemView, @NonNull Recommended recommended) {
        if (recommended.openWithGooglePlay) {
            openMarket(this, recommended.packageName, recommended.downloadUrl);
        } else {
            openWithBrowser(this, recommended.downloadUrl);
        }
        return false;
    }


    private void openMarket(@NonNull Context context, @NonNull String targetPackage, @NonNull String defaultDownloadUrl) {
        try {
            Intent googlePlayIntent = context.getPackageManager().getLaunchIntentForPackage("com.android.vending");
            ComponentName comp = new ComponentName("com.android.vending", "com.google.android.finsky.activities.LaunchUrlHandlerActivity");
            // noinspection ConstantConditions
            googlePlayIntent.setComponent(comp);
            googlePlayIntent.setData(Uri.parse("market://details?id=" + targetPackage));
            context.startActivity(googlePlayIntent);
        } catch (Throwable e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(defaultDownloadUrl)));
            e.printStackTrace();
        }
    }

    public static void openWithBrowser(Context context, String url) {
        //在浏览器打开
        try {
            context.startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                    context.getString(R.string.links_chooser_browser)));
        } catch (ActivityNotFoundException e) {
            // nop
        }
    }

}