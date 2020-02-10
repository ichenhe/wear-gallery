package cc.chenhe.weargallery.ui.recycler;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.List;

/**
 * 实现了加载更多。
 *
 * @param <T>
 */
abstract class BaseAdapter<T> extends RecyclerView.Adapter<ViewHolder> {
    private static final int TYPE_FOOTER_VIEW = -10000;
    private static final int TYPE_HEADER_VIEW = 200000;

    protected Context context;
    protected List<T> data;

    private View loadingView; // 更多数据加载中view
    private View loadEndView; // 更多数据加载完毕view
    private View loadFailedView; // 更多数据加载失败view
    private RelativeLayout footerLayout;
    private SparseArray<View> headerViews = new SparseArray<>();

    private boolean isOpenLoadMore; // 是否开启加载更多
    private boolean isLoadingMore; // 是否正在加载更多
    private boolean showHeaderView;
    private boolean showCustomFooterView;
    private OnLoadMoreListener loadMoreListener;
    private OnCustomFooterClickListener customFooterClickListener;

    protected abstract int getItemType(int position, T data);

    public BaseAdapter(Context context, @NonNull List<T> data) {
        this.context = context;
        this.data = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (showHeaderView && headerViews.get(viewType) != null)
            return ViewHolder.create(headerViews.get(viewType));
        ViewHolder viewHolder = null;
        if (viewType == TYPE_FOOTER_VIEW) {
            if (footerLayout == null)
                footerLayout = new RelativeLayout(context);
            viewHolder = ViewHolder.create(footerLayout);
        }
        return viewHolder;
    }

    @Override
    public int getItemCount() {
        return data.size() + getFooterViewCount() + getHeaderCount();
    }

    @Override
    public final int getItemViewType(int position) {
        if (showHeaderView && isHeaderView(position))
            return headerViews.keyAt(position);
        if (isFooterView(position))
            return TYPE_FOOTER_VIEW;
        return getItemType(position - getHeaderCount(), data.get(position - getHeaderCount()));
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        // StaggeredGridLayoutManager 时让 footer view 占据一整行
        int position = holder.getLayoutPosition();
        if (isFooterView(position) || isHeaderView(position)) {
            ViewGroup.LayoutParams lp = holder.getItemView().getLayoutParams();
            if (lp != null && lp instanceof StaggeredGridLayoutManager.LayoutParams) {
                StaggeredGridLayoutManager.LayoutParams p = (StaggeredGridLayoutManager.LayoutParams) lp;
                p.setFullSpan(true);
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        // GridLayoutManager 时让 footer view 占据一整行
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager != null && layoutManager instanceof GridLayoutManager) {
            final GridLayoutManager gridManager = (GridLayoutManager) layoutManager;
            gridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return isFooterView(position) || isHeaderView(position) ? gridManager.getSpanCount() : 1;
                }
            });
        }
        startLoadMore(recyclerView, layoutManager);
    }

    /**
     * 设置加载更多滑动监听
     */
    private void startLoadMore(RecyclerView recyclerView, final RecyclerView.LayoutManager layoutManager) {
        if (!isOpenLoadMore) return;
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState != RecyclerView.SCROLL_STATE_IDLE)
                    return;
                if (findLastVisibleItemPosition(layoutManager) == getItemCount() - 1)
                    scrollLoadMore();
            }
        });
    }

    /**
     * 找到最后一个可见的 view 位置。layoutManager 不支持返回 -1.
     */
    private int findLastVisibleItemPosition(RecyclerView.LayoutManager layoutManager) {
        if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            int[] lastVisibleItemPositions = ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(null);
            return max(lastVisibleItemPositions);
        }
        return -1;
    }

    private int max(int num[]) {
        int max = num[0];
        for (int i : num)
            if (i > max)
                max = i;
        return max;
    }

    /**
     * 滚动到底部触发加载更多。
     */
    private void scrollLoadMore() {
        if (footerLayout.getChildAt(0) == loadingView && !isLoadingMore)
            if (loadMoreListener != null) {
                isLoadingMore = true;
                loadMoreListener.onLoadMore(false);
            }
    }

    /**
     * 数据全部加载完成。（没有更多数据）
     */
    public void loadEnd() {
        if (loadEndView != null)
            setFooterView(loadEndView);
        else
            setFooterView(new View(context));
    }

    /**
     * 更多数据加载失败。
     */
    public void loadFailed() {
        setFooterView(loadFailedView);
        if (loadFailedView != null)
            loadFailedView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setFooterView(loadingView);
                    if (loadMoreListener != null)
                        loadMoreListener.onLoadMore(true);
                }
            });
    }

    /**
     * 根据 position 获取 data.
     */
    public T getItem(int position) {
        if (data.isEmpty())
            return null;
        return data.get(position);
    }

    /**
     * 返回 footer view 数量。
     */
    protected int getFooterViewCount() {
        return (isOpenLoadMore || showCustomFooterView) && !data.isEmpty() ? 1 : 0;
    }

    /**
     * 返回 header view 数量。
     */
    public int getHeaderCount() {
        if (!showHeaderView) {
            return 0;
        }
        return headerViews.size();
    }

    /**
     * 判断是否是 footer view.
     */
    private boolean isFooterView(int position) {
        return (isOpenLoadMore || showCustomFooterView) && position >= getItemCount() - 1;
    }

    /**
     * 判断是否是 header view.
     */
    protected boolean isHeaderView(int position) {
        return position < getHeaderCount();
    }

    protected boolean isCommonItemView(int viewType) {
        return viewType != TYPE_FOOTER_VIEW &&
                !(viewType >= TYPE_HEADER_VIEW);
    }

    /**
     * 清除 footer view.
     */
    private void removeFooterView() {
        if (footerLayout != null)
            footerLayout.removeAllViews();
    }

    /**
     * 设置 footer view. 将替换原来的 view.
     */
    private void setFooterView(View footerView) {
        if (footerView == null)
            return;
        if (footerLayout == null)
            footerLayout = new RelativeLayout(context);
        removeFooterView();
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        footerLayout.addView(footerView, params);
    }

    /**
     * 设置自定义 footer view.<br/>
     * 此功能不能和 LoadMore 同时使用。
     */
    public void setCustomFooterView(View footerView) {
        if (isOpenLoadMore) {
            Log.e("BaseAdapter", "can not display custom footer view while LoadMore is enable");
            return;
        }
        showCustomFooterView = true;
        setFooterView(footerView);
    }

    public void setCustomFooterView(int layoutId) {
        setCustomFooterView(LayoutInflater.from(context).inflate(layoutId, null));
    }

    public void setCustomFooterClickListener(final OnCustomFooterClickListener listener) {
        this.customFooterClickListener = listener;
        if (listener != null) {
            footerLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (BaseAdapter.this.customFooterClickListener != null)
                        BaseAdapter.this.customFooterClickListener.onCustomFooterClick(footerLayout);
                }
            });
        } else {
            footerLayout.setOnClickListener(null);
        }
    }

    /**
     * 添加 HeaderView.
     */
    public void addHeaderView(View view) {
        if (view == null) {
            return;
        }
        RelativeLayout layout = new RelativeLayout(context);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.addView(view, params);
        headerViews.put(TYPE_HEADER_VIEW + getHeaderCount(), layout);
    }

    /**
     * 添加加载更多的数据。
     *
     * @param newData 新增的数据。
     */
    public void addMoreData(List<T> newData) {
        isLoadingMore = false;
        int pos = data.size();
        data.addAll(newData);
        notifyItemInserted(pos + getHeaderCount());
    }

    /**
     * 替换数据。
     */
    public void setData(@NonNull List<T> data) {
        isLoadingMore = false;
        this.data.clear();
        this.data.addAll(data);
        notifyDataSetChanged();
    }

    /**
     * 设置底部加载中 view.
     */
    public void setLoadingView(View loadingView) {
        this.loadingView = loadingView;
        setFooterView(loadingView);
    }

    /**
     * 设置底部加载中 view.
     */
    public void setLoadingView(int loadingLayoutId) {
        setLoadingView(LayoutInflater.from(context).inflate(loadingLayoutId, null));
    }

    /**
     * 设置底部全部加载完毕（没有更多数据） view.
     */
    public void setLoadEndView(View loadEndView) {
        this.loadEndView = loadEndView;
    }

    /**
     * 设置底部全部加载完毕（没有更多数据） view.
     */
    public void setLoadEndView(int loadEndLayoutId) {
        setLoadEndView(LayoutInflater.from(context).inflate(loadEndLayoutId, null));
    }

    /**
     * 设置底部加载更多失败 view.
     */
    public void setLoadFailedView(View loadFailedView) {
        this.loadFailedView = loadFailedView;
    }

    /**
     * 设置底部加载更多失败 view.
     */
    public void setLoadFailedView(int loadFailedLayoutId) {
        setLoadFailedView(LayoutInflater.from(context).inflate(loadFailedLayoutId, null));
    }

    /**
     * 设置加载更多监听器。
     *
     * @param loadMoreListener <code>null</code> 取消监听。
     */
    public void setOnLoadMoreListener(@Nullable OnLoadMoreListener loadMoreListener) {
        this.loadMoreListener = loadMoreListener;
    }

    public void reset() {
        if (loadingView != null)
            setFooterView(loadingView);
        isLoadingMore = false;
        data.clear();
    }

    public void openLoadMore() {
        if (showCustomFooterView) {
            Log.e("BaseAdapter", "can not enable LoadMore while display custom footer view.");
            return;
        }
        this.isOpenLoadMore = true;
    }

    public void isShowHeaderView(boolean showHeaderView) {
        this.showHeaderView = showHeaderView;
    }

    public List<T> getAllData() {
        return data;
    }

    /**
     * 删除项目。
     *
     * @param position 位置，不考虑 header.
     */
    public void remove(int position) {
        data.remove(position);
        notifyItemRemoved(position + getHeaderCount());
    }

    /**
     * 通知单个项目改变。
     *
     * @param pos 位置，不考虑 header.
     */
    public void notifyItemDataChanged(int pos) {
        notifyItemChanged(pos + getHeaderCount());
    }
}
