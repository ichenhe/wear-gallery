package cc.chenhe.weargallery.bean.eventmsg;

public class WatchfaceImageChangedMsg {
    public WatchfaceImageChangedMsg(String imageFilePath) {
        this.imageFilePath = imageFilePath;
    }

    public String imageFilePath;
}
