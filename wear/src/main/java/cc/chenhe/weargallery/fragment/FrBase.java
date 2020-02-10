package cc.chenhe.weargallery.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import cc.chenhe.weargallery.R;


/**
 * 实现懒加载。并封装一些常用方法。
 */
public abstract class FrBase extends Fragment {
    private boolean isViewCreated = false;
    private View rootView;

    @Override
    public void onDestroyView() {
        isViewCreated = false;
        super.onDestroyView();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = onGetContentView(inflater, container);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isViewCreated = true;
        lazyLoad();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        lazyLoad();
    }

    private void lazyLoad() {
        if (isViewCreated && getUserVisibleHint()) {
            isViewCreated = false; // 防止重复加载
            onLazyLoad();
        }
    }

    @SuppressWarnings("unchecked")
    protected <T extends View> T findViewById(int id) {
        return (T) rootView.findViewById(id);
    }

    protected View getContentView() {
        return rootView;
    }

    /**
     * 开始进行懒加载。
     */
    protected void onLazyLoad() {
    }

    protected View onGetContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return inflater.inflate(onGetContentViewId(), container, false);
    }

    protected int onGetContentViewId() {
        return 0;
    }

    protected void switchFragment(int layoutId, Fragment fragment) {
        FragmentManager fm = getFragmentManager();
        if (fm != null) {
            fm.beginTransaction()
                    .hide(this)
                    .setCustomAnimations(R.anim.fr_slide_right_in, R.anim.fr_slide_right_in)
                    .add(layoutId, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    protected void replaceFragment(int layoutId, Fragment fragment, boolean addToBackStack) {
        FragmentManager fm = getFragmentManager();
        if (fm != null) {
            FragmentTransaction transaction = fm.beginTransaction();
            if (!addToBackStack)
                transaction.setCustomAnimations(R.anim.fr_slide_right_in, R.anim.fr_slide_right_in);
            transaction.replace(layoutId, fragment);
            if (addToBackStack)
                transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    protected void replaceFragment(int layoutId, Fragment fragment) {
        replaceFragment(layoutId, fragment, false);
    }

}
