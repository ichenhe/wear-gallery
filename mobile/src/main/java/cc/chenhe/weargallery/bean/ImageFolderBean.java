package cc.chenhe.weargallery.bean;

import java.io.Serializable;

/**
 * 图片所在文件夹类
 * Created by 王健(Jarek) on 2016/9/12.
 */
public class ImageFolderBean implements Serializable {

    private static final long serialVersionUID = 6645873496414509455L;
    /** 文件夹下第一张图片路径 */
    public String path;
    /**缩略图*/
    public String thumbnailsPath;
    /** 总图片数 */
    public int pisNum = 0;
    /** 文件夹名 */
    public String fileName;

    /**当图片选择后，索引值*/
    public int selectPosition;

    public int _id;

    /**当前图片在列表中顺序*/
    public int position;

}
