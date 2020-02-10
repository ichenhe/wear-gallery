package cc.chenhe.weargallery.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.TextView;

import cc.chenhe.weargallery.R;

/**
 * 提供 Loading 控件的管理。<br/>
 * 要求试图中必须有 id 为 <code>placeholder_loading</code> 的 <code>wear_loading</code> layout.
 */
public abstract class FrLoading extends FrBase {

    private View placeHolder;
    private View loading;
    private ViewStub viewStub;
    private View retryLayout;
    private TextView btnRetry;

    public int getLoadingPlaceHolderId() {
        return 0;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        placeHolder = findViewById(getLoadingPlaceHolderId());
        if (placeHolder == null) return getContentView();
        loading = findViewById(R.id.psWearLoading);
        viewStub = findViewById(R.id.vsWearLoading);
        return getContentView();
    }

    /**
     * 显示 loading 进度圈。隐藏重试按钮。
     *
     * @param isLoading 是否为加载中。<code>false</code> 则隐藏整个占位组件。
     */
    public final void setLoading(boolean isLoading) {
        if (isLoading) {
            placeHolder.setVisibility(View.VISIBLE);
            loading.setVisibility(View.VISIBLE);
            if (retryLayout != null)
                retryLayout.setVisibility(View.GONE);
        } else {
            placeHolder.setVisibility(View.GONE);
        }
    }

    /**
     * 显示重试按钮，隐藏 loading 进度圈。
     */
    public final void setRetry() {
        if (retryLayout == null) {
            viewStub.inflate();
            retryLayout = findViewById(R.id.retryLayout);
            btnRetry = findViewById(R.id.tvRetryBtnWearLoading);
            btnRetry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onRetry();
                }
            });
        }
        retryLayout.setVisibility(View.VISIBLE);
        loading.setVisibility(View.GONE);
    }

    /**
     * 当重试按钮被点击时调用。
     */
    public void onRetry() {
    }

}
