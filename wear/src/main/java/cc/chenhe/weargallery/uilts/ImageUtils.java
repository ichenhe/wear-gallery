package cc.chenhe.weargallery.uilts;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cc.chenhe.weargallery.common.bean.ImageFolderBeanT;

/**
 * 图片加载类
 * Created by 王健(Jarek) on 2016/9/12.
 * Edited by 晨鹤 on 2016/12/20.
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
                ArrayList<ImageFolderBeanT> imageFolders = new ArrayList<>();
                ContentResolver contentResolver = context.getContentResolver();
                /*查询：第一个图片路径、 文件夹名、 文件夹分类的图片总数*/
                String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME, "COUNT(1) AS count"};
                String selection = "0==0) GROUP BY (" + MediaStore.Images.Media.BUCKET_ID;
                String sortOrder = MediaStore.Images.Media.DATE_MODIFIED;
                try (Cursor cursor = contentResolver
                        .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, selection, null, sortOrder)) {
                    if (cursor != null && cursor.moveToFirst()) {

                        int columnPath = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                        int columnFileName = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                        int columnCount = cursor.getColumnIndex("count");

                        do {
                            ImageFolderBeanT folderBean = new ImageFolderBeanT();
                            folderBean.setFirstImagePath(cursor.getString(columnPath));
                            folderBean.setCount(cursor.getInt(columnCount));
                            folderBean.setDirAndName(new File(folderBean.getFirstImagePath()).getParent());
                            String bucketName = cursor.getString(columnFileName);

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
                    e.printStackTrace();
                    handler.sendEmptyMessage(what);
                }
            }
        }).start();
    }

    /**
     * 获取目录下所有图片的数量
     *
     * @param parentPath 父目录
     * @return 文件名list
     */
    public static int getChildPicturesCount(String parentPath) {
        File[] imgFile = new File(parentPath).listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                String tmp = f.getName().toLowerCase();
                return tmp.endsWith(".png") || tmp.endsWith(".jpg")
                        || tmp.endsWith(".jpeg") || tmp.endsWith(".webp") || tmp.endsWith("gif");
            }
        });
        if (imgFile == null)
            return 0;
        return imgFile.length;
    }

    /**
     * 获取目录下所有图片，并按照修改时间排序
     *
     * @param parentPath 父目录
     * @return 文件名list
     */
    public static List<String> getChildPicturesName(String parentPath) {
        List<String> mImgs;

        File[] imgFile = new File(parentPath).listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                String tmp = f.getName().toLowerCase();
                return tmp.endsWith(".png") || tmp.endsWith(".jpg")
                        || tmp.endsWith(".jpeg") || tmp.endsWith(".webp") || tmp.endsWith("gif");
            }
        });
        ArrayList<File> files = new ArrayList<>();
        if (imgFile != null) {
            Collections.addAll(files, imgFile);
            Collections.sort(files, new FileComparator());//排序
        } else {
            return null;
        }
        mImgs = new ArrayList<>();
        for (File file : files) {
            mImgs.add(file.getName());
        }
        return mImgs;
    }

    /**
     * 文件按时间排序
     */
    private static class FileComparator implements Comparator<File> {

        @Override
        public int compare(File lhs, File rhs) {
            if (lhs.lastModified() < rhs.lastModified()) {
                return 1;//最后修改的照片在前
            } else {
                return -1;
            }
        }
    }

    /**
     * 获得毛玻璃效果
     *
     * @param sentBitmap       原始图像
     * @param radius           虚化程度，建议：8
     * @param canReuseInBitmap 是否覆盖原始图像
     * @return 虚化的图像
     */
    public static Bitmap doBlur(Bitmap sentBitmap, int radius, boolean canReuseInBitmap) {

        Bitmap bitmap;
        if (canReuseInBitmap) {
            bitmap = sentBitmap;
        } else {
            bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        }

        if (radius < 1) {
            return (null);
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h);

        return (bitmap);
    }

}
