package cc.chenhe.weargallery.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import cc.chenhe.weargallery.GlideApp;
import cc.chenhe.weargallery.R;
import cc.chenhe.weargallery.bean.ImageFolderBean;
import cc.chenhe.weargallery.utils.ImageSelectObservable;


/**
 * 图片选择适配器
 * Created by 王健(Jarek) on 2016/9/12.
 */
public class ImageGridApter extends BaseRecyclerAdapter<ImageFolderBean, RecyclerView.ViewHolder> {

    /*当前是否为选择模式*/
    private boolean isSelectMode;

    /**
     * 标注是否是单选图片模式
     */
    private boolean mIsSelectSingleImge;

    /**
     * 已选图片列表,从ImageSelectObservable获取
     */
    private List<ImageFolderBean> mSelectlist;

    private int maxCount;


    public ImageGridApter(Context context, List<ImageFolderBean> list, boolean isSelectSingleImge, int maxCount, boolean isSelectMode) {
        super(context, list);
        this.mIsSelectSingleImge = isSelectSingleImge;
        this.maxCount = maxCount;
        this.isSelectMode = isSelectMode;

        mSelectlist = ImageSelectObservable.getInstance().getSelectImages();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.rv_item_grid_pics, parent, false);
        return new GridViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final GridViewHolder viewHolder = (GridViewHolder) holder;
        ImageFolderBean imageBean = list.get(position);
        imageBean.position = viewHolder.getAdapterPosition();

        notifyImageChanged(viewHolder.picIv, imageBean);
        notifyCheckChanged(viewHolder, imageBean);

        /*点击监听*/
        setSelectOnClickListener(viewHolder.selectView, imageBean, viewHolder.getAdapterPosition());
        setSelectOnLongClickListener(viewHolder.selectView, imageBean, viewHolder.getAdapterPosition());
        setOnItemClickListener(viewHolder.mCardView, viewHolder.getAdapterPosition());
        setOnItemLongClickListener(viewHolder.mCardView, viewHolder.getAdapterPosition());
    }

    /**
     * 图片加载
     *
     * @param imageView ImageView
     * @param imageBean ImageFolderBean
     */
    private void notifyImageChanged(ImageView imageView, ImageFolderBean imageBean) {
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GlideApp.with(mContext)
                .load(imageBean.path)
                .placeholder(R.drawable.bg_pic_defaut)
                .into(imageView);
    }

    /**
     * 选择按钮更新
     *
     * @param viewHolder GridViewHolder
     * @param imageBean  ImageFolderBean
     */
    private void notifyCheckChanged(GridViewHolder viewHolder, ImageFolderBean imageBean) {
        if (!isSelectMode || mIsSelectSingleImge) { //单选模式，不显示选择按钮
            viewHolder.checked.setVisibility(View.GONE);
            viewHolder.frontIv.setVisibility(View.GONE);
        } else {
            viewHolder.checked.setVisibility(View.VISIBLE);
            if (mSelectlist.contains(imageBean)) {  //当已选列表里包括当前item时，选择状态为已选，并显示在选择列表里的位置
                viewHolder.checked.setEnabled(true);
                viewHolder.checked.setText(String.valueOf(imageBean.selectPosition));
                viewHolder.frontIv.setVisibility(View.VISIBLE);
            } else {
                viewHolder.checked.setEnabled(false);
                viewHolder.checked.setText("");
                viewHolder.frontIv.setVisibility(View.GONE);
            }
        }
    }


    /**
     * 选择按钮点击监听
     *
     * @param view      点击view
     * @param imageBean 对应的实体类
     * @param position  点击位置
     */
    private void setSelectOnClickListener(View view, final ImageFolderBean imageBean, final int position) {

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSelectMode()) {
                    //当前不是选择模式，模拟 card view 被单击
                    if (mOnClickListener != null) {
                        if (mIsSelectSingleImge) {
                            mSelectlist.add(list.get(position));
                        }
                        mOnClickListener.onItemClick(v, position);
                    }
                    return;
                }
                if (mSelectlist.contains(imageBean)) { //点击的item为已选过的图片时，删除
                    mSelectlist.remove(imageBean);
                    subSelectPosition();
                } else { //不在选择列表里，添加
                    if (mSelectlist.size() >= maxCount) {
                        Toast.makeText(mContext, mContext.getResources().getString(R.string.grid_pics_select_photo_max, maxCount), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mSelectlist.add(imageBean);
                    imageBean.selectPosition = mSelectlist.size();
                }

                //通知点击项发生了改变
                notifyItemChanged(position);

                if (mOnClickListener != null) { //回调，页面需要展示选择的图片张数
                    mOnClickListener.onItemClick(v, -1);
                }
            }
        };

        view.setOnClickListener(listener);

    }

    /**
     * 选择按钮长按监听
     *
     * @param view      点击view
     * @param imageBean 对应的实体类
     * @param position  点击位置
     */
    private void setSelectOnLongClickListener(View view, final ImageFolderBean imageBean, final int position) {
        View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (!isSelectMode()) {
                    //当前不是选择模式，模拟 card view 被单击
                    if (mOnClickListener != null) {
                        mOnClickListener.onItemLongClick(view, position);
                    }
                    return false;
                }
                return false;
            }
        };
        view.setOnLongClickListener(longClickListener);
    }


    /**
     * item点击监听，多选时查看大图，单选时返回选择图片
     *
     * @param view     点击view
     * @param position 点击位置
     */
    private void setOnItemClickListener(View view, final int position) {
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnClickListener != null) {
                    if (mIsSelectSingleImge) {
                        mSelectlist.add(list.get(position));
                    }
                    mOnClickListener.onItemClick(v, position);
                }
            }
        };
        view.setOnClickListener(listener);
    }

    private void setOnItemLongClickListener(View view, final int position) {
        View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (mOnClickListener != null) {
                    mOnClickListener.onItemLongClick(view, position);
                }
                return false;
            }
        };
        view.setOnLongClickListener(longClickListener);
    }


    /**
     * 更新选择的顺序
     */
    private void subSelectPosition() {
        for (int index = 0, len = mSelectlist.size(); index < len; index++) {
            ImageFolderBean folderBean = mSelectlist.get(index);
            folderBean.selectPosition = index + 1;
            notifyItemChanged(folderBean.position);
        }

    }

    /**
     * 所有选择的图片
     *
     * @return List<ImageFolderBean>
     */
    public List<ImageFolderBean> getSelectlist() {
        return mSelectlist;
    }

    private class GridViewHolder extends RecyclerView.ViewHolder {
        View containerView;
        ImageView picIv;
        ImageView frontIv;
        public TextView checked;
        TextView selectView;
        CardView mCardView;

        GridViewHolder(View convertView) {
            super(convertView);

            containerView = convertView.findViewById(R.id.main_frame_layout);
            picIv = convertView.findViewById(R.id.iv_pic);
            checked = convertView.findViewById(R.id.tv_select);
            frontIv = convertView.findViewById(R.id.iv_forgound);
            mCardView = convertView.findViewById(R.id.card_view);
            selectView = convertView.findViewById(R.id.tv_select_v);
        }
    }

    public void setSelectMode(boolean selectMode) {
        setSelectMode(selectMode, -1);
    }

    public void setSelectMode(boolean selectMode, int pos) {
        isSelectMode = selectMode;
        if (selectMode && pos >= 0 && pos < getItemCount()) {
            mSelectlist.add(getItem(pos));
            getItem(pos).selectPosition = mSelectlist.size();
        }
        notifyDataSetChanged();
    }

    public boolean isSelectMode() {
        return isSelectMode;
    }
}
