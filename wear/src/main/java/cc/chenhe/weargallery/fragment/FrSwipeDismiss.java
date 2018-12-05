package cc.chenhe.weargallery.fragment;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.wearable.view.SwipeDismissFrameLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

@SuppressWarnings("deprecation")
public abstract class FrSwipeDismiss extends FrLoading {

    private class SwipeDismissCallback extends SwipeDismissFrameLayout.Callback {

        @Override
        public void onDismissed(SwipeDismissFrameLayout layout) {
            super.onDismissed(layout);
            removeSelf();
        }
    }

    private SwipeDismissCallback callback = new SwipeDismissCallback();

    @Override
    protected View onGetContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        View view = super.onGetContentView(inflater, container);
        SwipeDismissFrameLayout swipeDismissFrameLayout = new SwipeDismissFrameLayout(getActivity());
        swipeDismissFrameLayout.addCallback(callback);
        swipeDismissFrameLayout.addView(view);
        return swipeDismissFrameLayout;
    }

    protected void removeSelf() {
        FragmentManager fm = getFragmentManager();
        if (isAdded() && fm != null) {
            fm.beginTransaction().remove(FrSwipeDismiss.this)
                    .commitAllowingStateLoss();
            fm.popBackStack();
        }
    }

}
