package cc.chenhe.weargallery.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;


import java.util.ArrayList;
import java.util.List;

import cc.chenhe.weargallery.bean.ImageFolderBean;

/**
 * 图片加载类
 * Created by 王健(Jarek) on 2016/9/12.
 */
public class ImageUtils {

    /**
     * 加载所有包含图片的文件夹
     *
     * @param context Activity
     * @param handler Handler 异步加载完成后通知
     * @param what    Handler.what
     */
    public static void loadLocalFolderContainsImage(final Context context, final Handler handler, final int what) {

        new Thread(new Runnable() {

            @Override
            public void run() {
                ArrayList<ImageFolderBean> imageFolders = new ArrayList<>();
                ContentResolver contentResolver = context.getContentResolver();
                /*查询id、  缩略图、原图、文件夹ID、 文件夹名、 文件夹分类的图片总数*/
                String[] columns = {MediaStore.Images.Media._ID, MediaStore.Images.Thumbnails.DATA, MediaStore.Images.Media.DATA, MediaStore.Images.Media.BUCKET_ID,
                        MediaStore.Images.Media.BUCKET_DISPLAY_NAME, "COUNT(1) AS count"};
                String selection = "0==0) GROUP BY (" + MediaStore.Images.Media.BUCKET_ID;
                String sortOrder = MediaStore.Images.Media.DATE_MODIFIED;
                Cursor cursor = null;
                try {
                    cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, selection, null, sortOrder);
                    if (cursor != null && cursor.moveToFirst()) {

                        int columnPath = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                        int columnId = cursor.getColumnIndex(MediaStore.Images.Media._ID);

                        int columnFileName = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                        int columnCount = cursor.getColumnIndex("count");

                        do {
                            ImageFolderBean folderBean = new ImageFolderBean();
                            folderBean.path = cursor.getString(columnPath);
                            folderBean._id = cursor.getInt(columnId);
                            folderBean.pisNum = cursor.getInt(columnCount);

                            String bucketName = cursor.getString(columnFileName);
                            folderBean.fileName = bucketName;

                            if (!Environment.getExternalStorageDirectory().getPath().contains(bucketName)) {
                                imageFolders.add(0, folderBean);
                            }
                        } while (cursor.moveToNext());
                    }
                    Message msg = new Message();
                    msg.what = what;
                    msg.obj = imageFolders;
                    handler.sendMessage(msg);
                } catch (Exception e) {
                    handler.sendEmptyMessage(what);
                    e.printStackTrace();
                } finally {
                    if (cursor != null)
                        cursor.close();
                }
            }
        }).start();
    }


    /**
     * 获取相册指定目录下的全部图片路径
     *
     * @param c          Context
     * @param folderPath 指定目录
     * @param handler    Handler 异步加载完成后通知
     * @param what       Handler.what
     */
    public static void queryGalleryPicture(final Context c, final String folderPath, final Handler handler, final int what) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                ArrayList<ImageFolderBean> list = new ArrayList<>();
                String[] columns = new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA};

                /*查询文件路径包含上面指定的文件夹路径的图片--这样才能保证查询到的文件属于当前文件夹下*/
                String whereclause = MediaStore.Images.ImageColumns.DATA + " like'" + folderPath + "/%'";
                Log.i("queryGalleryPicture", "galleryPath:" + folderPath);

                Cursor corsor = null;
                List<ImageFolderBean> selects = ImageSelectObservable.getInstance().getSelectImages();

                try {
                    corsor = c.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, whereclause, null,
                            null);
                    if (corsor != null && corsor.getCount() > 0 && corsor.moveToFirst()) {
                        do {
                            String path = corsor.getString(corsor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
                            int id = corsor.getInt(corsor.getColumnIndex(MediaStore.Images.ImageColumns._ID));

                            ImageFolderBean photoItem = new ImageFolderBean();
                            photoItem.path = path;
                            photoItem._id = id;

                            /**遍历查询之前选择的图片是否在其中*/
                            for (int index = 0, len = selects.size(); index < len; index++) {
                                if (selects.get(index).path.equals(photoItem.path)) {
                                    photoItem.selectPosition = selects.get(index).selectPosition;
                                    selects.remove(index);
                                    selects.add(photoItem);
                                    break;
                                }
                            }

                            list.add(0, photoItem);
                        } while (corsor.moveToNext());
                    }

                    Message msg = new Message();
                    msg.what = what;
                    msg.obj = list;
                    handler.sendMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (corsor != null)
                        corsor.close();
                }
            }
        }).start();
    }

}
