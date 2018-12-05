package cc.chenhe.weargallery.ui.recycler;

public interface OnItemClickListener<T> {
    void onItemClick(ViewHolder holder, T data, int pos);
}
