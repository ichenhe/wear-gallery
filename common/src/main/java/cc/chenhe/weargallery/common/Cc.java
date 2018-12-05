package cc.chenhe.weargallery.common;

/**
 * 通讯path
 * Created by 晨鹤 on 2016/12/15.
 */

public class Cc {

    /*检查谷歌授权的结果*/
    public static final int RESULT_LICENSE_ALLOW = 1;//允许
    public static final int RESULT_LICENSE_NOT_GOOGLE_MODE = 5;//不是国际版
    public static final int RESULT_LICENSE_DISALLOW_OTHER = 2;//不允许
    public static final int RESULT_LICENSE_DISALLOW_RETRY = 4;//不允许，应该重试
    public static final int RESULT_LICENSE_APPERR = 3;//错误

    /*手表开始初始化，手机端依次进行：版本验证、授权验证、扫描图库。返回json*/
    public static final String PATH_WATCH_INIT = "/PATH_WATCH_INIT";//数据为watch端app code
    public static final String RESULT_INIT_KEY_OPERATION_TYPE = "OPERATION_TYPE";//key,值为整数型↓
    public static final int RESULT_INIT_OPERATION_TYPE_CHECK_GOOGEL_LICENSE = 0;//int，由上面常量定义
    public static final int RESULT_INIT_OPERATION_TYPE_APP_VERSION = 1;//boolean 版本相等为true
    public static final int RESULT_INIT_OPERATION_TYPE_GALLERY_LIST = 2;//String
    public static final String RESULT_INIT_KEY_DATA = "DATA";//key,值数据类型不同
    /*手表请求获取图库列表*/
    public static final String PATH_GET_GALLERY = "/PATH_GET_GALLERY";
    public static final String GET_GALLERY_ERR_PERMISSION = "ERR_PERMISSION";
    public static final String GET_GALLERY_ERR_SCAN = "ERR_SCAN";
    /*手表请求获取单张图片缩略图(用于图库列表预览)*/
    public static final String PATH_GET_MICRO_PICTURE = "/PATH_GET_MICRO_PICTURE";//数据里为文件path
    /*存储着图片的asset的key*/
    public static final String ASSET_KEY_PIC = "ASSET_KEY_PIC";
    /*手表请求手机隐藏指定相册*/
    public static final String PATH_HIDE_GALLERY = "/PATH_HIDE_GALLERY"; //数据里为相册路径
    /*手机请求手表删除缓存*/
    public static final String PATH_DEL_CACHE = "/PATH_DEL_CACHE";//删除缓存
    public static final String PATH_DEL_OFFLINE_IMAGE = "/DEL_OFFLINE_IMAGE";//删除离线图片
    /*手机请求获取手表缓存占用*/
    public static final String PATH_GET_WATCH_CACHE_SIZE = "/PATH_GET_WATCH_CANCHE_SIZE";
    /*手表请求获取手机指定目录下的图片文件名列表*/
    public static final String PATH_GET_FILE_NAME_LIST = "/PATH_GET_FILE_NAME_LIST";//数据里为父目录
    /*手表请求获取单张图片缩略图*/
    public static final String PATH_GET_SINGLE_PICTURE = "/PATH_GET_SINGLE_PICTURE";//数据里为文件path
    /*手表请求获取高清图*/
    public static final String PATH_GET_ORIGINAL_PICTURE = "/PATH_GET_ORIGINAL_PICTURE";//数据里为图片路径
    public static final String RESULT_IMAGE_EXTENSION_NAME = "extension_name"; //String文件扩展名
    /*手表请求删除手机图片*/
    public static final String PATH_DEL_PHONE_PICTURE = "/PATH_DEL_PHONE_PICTURE";
    public static final int RESULT_DEL_PHONE_PICTURE_OK = 0;
    public static final int RESULT_DEL_PHONE_PICTURE_FAILED = 1;
    public static final int RESULT_DEL_PHONE_PICTURE_NO_PERMISSION = 3;
    /*批量发送：手机开始发送一个图片*/
    public static final String PATH_SEND_PICS = "/PATH_SEND_PICS";
    public static final String KEY_FILE_PATH_ON_PHONE = "KEY_FILE_PATH_ON_PHONE";//String 图片在手机的路径
    public static final String KEY_SENDED_COUNT = "KEY_SENDED_COUNT";//int 已经发的文件数（包括当前正在发的）
    public static final String KEY_TOTAL_COUNT = "KEY_TOTAL_COUNT";//int 总文件数（可能变）
    public static final String KEY_SEND_TIME = "KEY_SEND_TIME";//long 发送的时间
    /*批量发送：手表确认收到了手机的图片*/
    public static final String PATH_CONFIRM_RECEIVE_PICS = "/PATH_CONFIRM_RECEIVE_PICS";
    /*批量发送：手机确认发送完毕*/
    public static final String PATH_SEND_PICS_FINISH = "/PATH_SEND_PICS_FINISH";
    /*批量发送：手机取消发送*/
    public static final String PATH_SEND_PICS_CANCEL = "/PATH_SEND_PICS_CANCEL";
    /*重置新手引导*/
//    public static final String PATH_RESET_TIP = "/PATH_RESET_TIP";
}
