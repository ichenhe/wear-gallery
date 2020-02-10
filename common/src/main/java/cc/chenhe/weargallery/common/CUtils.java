package cc.chenhe.weargallery.common;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

/**
 * 通用工具类
 * Created by 晨鹤 on 2016/12/13.
 */

public class CUtils {
    /*手机批量发送图片时等待手表message回执的时间*/
    public static final long SEND_IMAGES_DATA_TIME_OUT = 20000;

    /**
     * 获取版本号
     *
     * @return 当前应用的版本号
     */
    public static int getVersionCode(Context context) {
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(),
                    0);
            return info.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * 获取版本号名
     *
     * @return 当前应用的版本名
     */
    public static String getVersionName(Context context) {
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(),
                    0);
            return info.versionName;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 判断文件是否存在
     *
     * @param strFile
     * @return
     */
    public static boolean fileIsExists(String strFile) {
        try {
            File f = new File(strFile);
            if (!f.exists()) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 将图片写入到磁盘
     *
     * @param img  图片数据流
     * @param path 路径+名称
     */
    public static void writeImageToDisk(byte[] img, String path) {
        try {
            File file = new File(path);
            FileOutputStream fops = new FileOutputStream(file);
            fops.write(img);
            fops.flush();
            fops.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将图片写入到磁盘
     *
     * @param in   图片输入流
     * @param path 路径+名称
     */
    public static void writeImageToDisk(InputStream in, String path) {
        File file = new File(path);
        if (file.isFile() && file.exists())
            if (!file.delete()) return;
        try (FileOutputStream fops = new FileOutputStream(file)) {
            byte[] buffer = new byte[2048];
            int len;
            while ((len = in.read(buffer)) != -1) {
                fops.write(buffer, 0, len);
            }
            fops.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] is2ByteArray(InputStream input) {
        if (input == null)
            return null;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n = 0;
        try {
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return output.toByteArray();
    }

    /**
     * bitmap转byte[]
     *
     * @param bm
     * @param size 文件大小
     * @return
     */
    public static byte[] bitmap2Bytes(Bitmap bm, long size) {
        if (bm == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int qua = 50;
        bm.compress(Bitmap.CompressFormat.WEBP, qua, baos);
//        while (baos.is2ByteArray().length>size && qua>5){
//            baos.reset();
//            qua=qua-5;
//            bm.compress(Bitmap.CompressFormat.WEBP, qua, baos);
//        }
        byte[] bytes = baos.toByteArray();
        bm.recycle();
        return bytes;
    }

    public static Bitmap bytes2Bimap(byte[] b) {
        if (b.length != 0) {
            return BitmapFactory.decodeByteArray(b, 0, b.length);
        } else {
            return null;
        }
    }

    /**
     * 删除文件或文件夹
     */
    public static boolean deleteFile(File file) {
        if (file.isFile()) {
            return file.delete();
        }
        if (file.isDirectory()) {
            String[] children = file.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteFile(new File(file, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return file.delete();
    }

    /**
     * 获取文件夹大小
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static long getFolderSize(File file) throws Exception {
        long size = 0;
        try {
            File[] fileList = file.listFiles();
            for (int i = 0; i < fileList.length; i++) {
                // 如果下面还有文件
                if (fileList[i].isDirectory()) {
                    size = size + getFolderSize(fileList[i]);
                } else {
                    size = size + fileList[i].length();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }

    /**
     * 格式化文件大小单位
     *
     * @param size Byte
     * @return
     */
    public static String getFormatSize(double size) {
        if (size == 0)
            return "0B";
        double kiloByte = size / 1024;
        if (kiloByte < 1) {
            return size + "B";
        }

        double megaByte = kiloByte / 1024;
        if (megaByte < 1) {
            BigDecimal result1 = new BigDecimal(Double.toString(kiloByte));
            return result1.setScale(2, BigDecimal.ROUND_HALF_UP)
                    .toPlainString() + "KB";
        }

        double gigaByte = megaByte / 1024;
        if (gigaByte < 1) {
            BigDecimal result2 = new BigDecimal(Double.toString(megaByte));
            return result2.setScale(2, BigDecimal.ROUND_HALF_UP)
                    .toPlainString() + "MB";
        }

        double teraBytes = gigaByte / 1024;
        if (teraBytes < 1) {
            BigDecimal result3 = new BigDecimal(Double.toString(gigaByte));
            return result3.setScale(2, BigDecimal.ROUND_HALF_UP)
                    .toPlainString() + "GB";
        }
        BigDecimal result4 = new BigDecimal(teraBytes);
        return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString()
                + "TB";
    }

    /**
     * 取文件扩展名
     *
     * @param fileName 文件名或路径
     * @return 扩展名
     */
    public static String getExtraName(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    public static String getExtraName(@NonNull File file) {
        return getExtraName(file.getName());
    }
}
