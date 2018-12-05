package cc.chenhe.weargallery.common.bean;

/**
 * 用于传输到手表的专用图片文件夹包装类
 * 减少了数据大小
 * Created by 晨鹤 on 2016/12/15.
 */

public class ImageFolderBeanT {

    /*图片的文件夹路径，不以/结尾*/
    private String dir;

    /*第一张图片的路径*/
    private String firstImagePath;

    /*文件夹的名称*/
    private String name;

    /*图片的数量*/
    private int count;

    public ImageFolderBeanT() {
    }

    public ImageFolderBeanT(String dir, String firstImagePath, String name, int count) {
        this.dir = dir;
        this.firstImagePath = firstImagePath;
        this.name = name;
        this.count = count;
    }

    public String getDir() {
        return dir;
    }

    public void setDirAndName(String dir) {
        this.dir = dir;
        int lastIndexOf = dir.lastIndexOf("/");
        this.name = dir.substring(lastIndexOf+1);
    }

    public void setDirOnly(String dir) {
        this.dir = dir;
    }

    public String getFirstImagePath() {
        return firstImagePath;
    }

    public void setFirstImagePath(String firstImagePath) {
        this.firstImagePath = firstImagePath;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
