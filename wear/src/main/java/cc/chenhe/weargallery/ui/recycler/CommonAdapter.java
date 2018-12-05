package cc.chenhe.weargallery.ui.recycler;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public abstract class CommonAdapter<T> extends BaseAdapter<T> {
    private OnItemClickListener<T> itemClickListener;
    private OnHeaderItemClickListener headerItemClickListener;

    public abstract int getItemLayoutId(int viewType);

    public abstract void bindView(ViewHolder holder, int pos, int viewType);

    public CommonAdapter(Context context, @NonNull List<T> data) {
        super(context, data);
    }

    @NonNull
    @Override
    public final ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (isCommonItemView(viewType))
            return ViewHolder.create(context, getItemLayoutId(viewType), parent);
        return super.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        int viewType = holder.getItemViewType();
        if (isCommonItemView(viewType)) {
            bindView(holder, position - getHeaderCount(), viewType);
        }
        if (isCommonItemView(viewType) || isHeaderView(position))
            holder.getItemView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int pos = holder.getAdapterPosition();
                    if (isHeaderView(pos)) {
                        if (headerItemClickListener != null)
                            headerItemClickListener.onHeaderItemClick(holder, pos);
                    } else {
                        pos = pos - getHeaderCount();
                        if (itemClickListener != null)
                            itemClickListener.onItemClick(holder, data.get(pos), pos);
                    }
                }
            });
    }

    @Override
    protected int getItemType(int position, T data) {
        return 0;
    }

    public void setOnItemClickListener(OnItemClickListener<T> listener) {
        this.itemClickListener = listener;
    }

    public void setOnHeaderItemClickListener(OnHeaderItemClickListener listener) {
        this.headerItemClickListener = listener;
    }
}
