package cc.chenhe.weargallery.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import cc.chenhe.weargallery.bean.ImageFolderBean;

/**
 * Created by 晨鹤 on 2016/12/7.
 */

public class ImageFolderSelectObservable extends Observable {

    private static ImageFolderSelectObservable sObserver = null;

    private List<ImageFolderBean> mSelectFolders;

    /*选中状态改变的位置，-1表示全选/反选*/
    private int changedPosition = -1;

    private ImageFolderSelectObservable() {
        mSelectFolders = new ArrayList<>();
    }

    public static ImageFolderSelectObservable getInstance() {
        if (sObserver == null) {
            synchronized (ImageFolderSelectObservable.class) {
                if (sObserver == null)
                    sObserver = new ImageFolderSelectObservable();
            }
        }
        return sObserver;
    }

    public int getChangedPosition() {
        return changedPosition;
    }

    public void setChangedPosition(int changedPosition) {
        this.changedPosition = changedPosition;
    }

    /**
     * 通知选择已改变
     */
    public void updateFolderSelectChanged() {
        setChanged();
        notifyObservers();
    }

    public void addFoldersAndClearBefore(List<ImageFolderBean> folders) {
        mSelectFolders.clear();
        if (folders != null) {
            mSelectFolders.addAll(folders);
        }
    }

    public void clearAndRelease() {
        mSelectFolders = null;
        sObserver = null;
    }

    public List<ImageFolderBean> getSelectFolders() {
        return mSelectFolders;
    }
}
