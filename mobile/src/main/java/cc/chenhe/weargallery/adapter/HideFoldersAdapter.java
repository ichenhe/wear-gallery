package cc.chenhe.weargallery.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import cc.chenhe.weargallery.R;

/**
 * Created by 晨鹤 on 2016/12/13.
 */

public class HideFoldersAdapter extends BaseRecyclerAdapter<String, RecyclerView.ViewHolder> {

    public HideFoldersAdapter(Context context, List<String> list) {
        super(context, list);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.rv_item_hide_folders, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, final int position) {
        super.onBindViewHolder(viewHolder, position);
        MyViewHolder myViewHolder = (MyViewHolder) viewHolder;
        myViewHolder.tvPath.setText(list.get(position));
        myViewHolder.btnCancelHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOnClickListener != null) {
                    mOnClickListener.onItemClick(view, position);
                }
            }
        });
    }

    public void remove(int position){
        list.remove(position);
        notifyDataSetChanged();
    }

    public List<String> getData(){
        return list;
    }

    protected class MyViewHolder extends RecyclerView.ViewHolder {

        public TextView tvPath;
        public View btnCancelHide;

        public MyViewHolder(View itemView) {
            super(itemView);
            tvPath = (TextView) itemView.findViewById(R.id.tv_path);
            btnCancelHide = itemView.findViewById(R.id.tv_cancel_hide);
        }
    }
}
