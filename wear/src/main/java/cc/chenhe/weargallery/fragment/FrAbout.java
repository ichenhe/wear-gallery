package cc.chenhe.weargallery.fragment;

import android.content.Context;
import android.widget.TextView;

import cc.chenhe.weargallery.R;
import cc.chenhe.weargallery.common.CUtils;

public class FrAbout extends FrSwipeDismiss {
    Context context;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    protected int onGetContentViewId() {
        return R.layout.fr_about;
    }

    @Override
    protected void onLazyLoad() {
        super.onLazyLoad();
        ((TextView) findViewById(R.id.tvVersion))
                .setText(CUtils.getVersionName(context));
    }
}
