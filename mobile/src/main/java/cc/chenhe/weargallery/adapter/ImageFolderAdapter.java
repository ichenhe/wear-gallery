package cc.chenhe.weargallery.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import cc.chenhe.weargallery.GlideApp;
import cc.chenhe.weargallery.R;
import cc.chenhe.weargallery.bean.ImageFolderBean;
import cc.chenhe.weargallery.utils.ImageFolderSelectObservable;


/**
 * 图片选择目录适配器
 * Created by 王健(Jarek) on 2016/9/12.
 */
public class ImageFolderAdapter extends BaseRecyclerAdapter<ImageFolderBean, RecyclerView.ViewHolder> {

    /*当前是否为选择模式*/
    private boolean isSelectMode;
    private boolean scrolling = false;

    public ImageFolderAdapter(Context context, List<ImageFolderBean> list) {
        super(context, list);
        isSelectMode = false;
    }


    @NonNull
    @Override
    public PhotoFolderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.rv_item_pics_folder, parent, false);
        return new PhotoFolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        final PhotoFolderViewHolder holder = (PhotoFolderViewHolder) viewHolder;
        ImageFolderBean imageFolderBean = list.get(position);
        holder.tvFileName.setText(imageFolderBean.fileName);
        holder.tvFileNums.setText(String.format(mContext.getResources().getString(R.string.main_pics_num), imageFolderBean.pisNum));

        GlideApp.with(mContext)
                .load(imageFolderBean.path)
                .placeholder(R.drawable.bg_pic_defaut)
                .into(holder.ivImage);

        if (isSelectMode) {
            holder.ivArrow.setVisibility(View.GONE);
            holder.tvCheck.setVisibility(View.VISIBLE);

            AlphaAnimation fadeImage;
            if (ImageFolderSelectObservable.getInstance().getSelectFolders().contains(imageFolderBean)) {
                //已选中
                holder.tvCheck.setEnabled(true);
                if (!isScrolling()) {
                    fadeImage = new AlphaAnimation(0, 1);
                    fadeImage.setDuration(300);
                    fadeImage.setInterpolator(new DecelerateInterpolator());
                    holder.ivForgound.startAnimation(fadeImage);
                    holder.ivForgound.setTag(true);
                }
                holder.ivForgound.setVisibility(View.VISIBLE);
            } else {
                holder.tvCheck.setEnabled(false);
                if (!isScrolling()) {
                    fadeImage = new AlphaAnimation(1, 0);
                    fadeImage.setDuration(300);
                    fadeImage.setInterpolator(new DecelerateInterpolator());
                    holder.ivForgound.startAnimation(fadeImage);
                    holder.ivForgound.setTag(false);
                }
                holder.ivForgound.setVisibility(View.GONE);
            }
        } else {
            holder.ivArrow.setVisibility(View.VISIBLE);
            holder.tvCheck.setVisibility(View.GONE);
            holder.ivForgound.setVisibility(View.GONE);
            holder.ivForgound.setTag(false);
        }

        if (mOnClickListener != null) {
            holder.mCardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mOnClickListener.onItemClick(view, holder.getAdapterPosition());
                }
            });

            holder.mCardView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    mOnClickListener.onItemLongClick(view, holder.getAdapterPosition());
                    return false;
                }
            });
        }

    }

    public boolean isScrolling() {
        return scrolling;
    }

    public void setScrolling(boolean scrolling) {
        this.scrolling = scrolling;
    }

    public boolean isSelectMode() {
        return isSelectMode;
    }

    public void setSelectMode(boolean selectMode) {
        isSelectMode = selectMode;
    }

    /**
     * 自定义ViewHolder
     */
    protected class PhotoFolderViewHolder extends RecyclerView.ViewHolder {

        ImageView ivImage;
        TextView tvFileName;
        TextView tvFileNums;
        CardView mCardView;
        ImageView ivArrow, ivForgound;
        TextView tvCheck;

        PhotoFolderViewHolder(View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tv_file_name);
            tvFileNums = itemView.findViewById(R.id.tv_pic_nums);
            ivImage = itemView.findViewById(R.id.iv_icon);
            mCardView = itemView.findViewById(R.id.card_view);
            ivArrow = itemView.findViewById(R.id.iv_arrow);
            ivForgound = itemView.findViewById(R.id.iv_forgound);
            tvCheck = itemView.findViewById(R.id.ctv_check);
        }
    }

}
