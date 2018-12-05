package cc.chenhe.weargallery.ui.recycler;

public interface OnLoadMoreListener  {
    /**
     * 加载更多回调。
     *
     * @param isRetry 是否是重试。
     */
    void onLoadMore(boolean isRetry);
}
