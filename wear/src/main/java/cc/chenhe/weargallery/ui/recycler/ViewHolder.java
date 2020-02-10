package cc.chenhe.weargallery.ui.recycler;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ViewHolder extends RecyclerView.ViewHolder {
    private View itemView;
    private SparseArray<View> views;

    public static ViewHolder create(Context context, int layoutId, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    public static ViewHolder create(View itemView) {
        return new ViewHolder(itemView);
    }

    private ViewHolder(View itemView) {
        super(itemView);
        this.itemView = itemView;
        views = new SparseArray<>();
    }

    /**
     * 通过 id 获取 View.
     *
     * @param viewId id.
     * @param <T>    数据类型。
     */
    public <T extends View> T getView(int viewId) {
        View view = views.get(viewId);
        if (view == null) {
            view = itemView.findViewById(viewId);
            views.put(viewId, view);
        }
        return (T) view;
    }

    /**
     * 获取根 View.
     */
    public View getItemView() {
        return itemView;
    }

    public void setText(int viewId, String text) {
        TextView view = getView(viewId);
        view.setText(text);
    }

    public void setText(int viewId, int resId) {
        TextView view = getView(viewId);
        view.setText(resId);
    }

    public void setVisibility(int viewId, int visibility) {
        getView(viewId).setVisibility(visibility);
    }

    public void setVisibility(int viewId, boolean visibility) {
        getView(viewId).setVisibility(visibility ? View.VISIBLE : View.GONE);
    }

    public void setOnClickListener(int viewId, View.OnClickListener listener) {
        getView(viewId).setOnClickListener(listener);
    }
}
