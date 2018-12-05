package cc.chenhe.weargallery.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.List;

import cc.chenhe.weargallery.listener.OnRecyclerViewClickListener;

/**
 * RecyclerAdapter 适配器基类
 * Created by jarek(王健) on 2016/9/12.
 */
public class BaseRecyclerAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    protected OnRecyclerViewClickListener mOnClickListener;

    protected Context mContext;
    protected LayoutInflater mInflater;

    protected BaseRecyclerAdapter(Context context, List<T> list) {
        this.mContext = context;
        this.list = list;
        this.mInflater = LayoutInflater.from(mContext);
    }

    protected List<T> list;
    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {

    }


    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    public T getItem(int pos){
        return list.get(pos);
    }

    public void setOnClickListener(OnRecyclerViewClickListener onClickListener) {
        this.mOnClickListener = onClickListener;
    }

}
