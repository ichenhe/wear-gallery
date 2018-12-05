package cc.chenhe.weargallery;

import android.content.Context;
import android.support.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import cc.chenhe.weargallery.bean.eventmsg.LocalImageChangedMsg;
import cc.chenhe.weargallery.uilts.Utils;
import fi.iki.elonen.NanoHTTPD;

public class WebServer extends NanoHTTPD {
    private static final int PORT = 7160;

    private Context context;

    public WebServer(Context context) {
        super(PORT);
        this.context = context.getApplicationContext();
    }


    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        if (uri.equals("/upload") && session.getMethod() == Method.POST) {
            return upload(session, "image");
        }

        String filename = uri.substring(1);
        if (uri.equals("/"))
            filename = "index.html";
        String mimeType = "text/html";
        if (filename.contains(".js")) {
            mimeType = "text/javascript";
        } else if (filename.contains(".css")) {
            mimeType = "text/css";
        } else if (filename.contains(".gif")) {
            mimeType = "text/gif";
        } else if (filename.contains(".jpeg") || filename.contains(".jpg")) {
            mimeType = "text/jpeg";
        } else if (filename.contains(".png")) {
            mimeType = "image/png";
        }

        InputStream in = null;
        try {
            in = context.getAssets().open("web" + File.separator + filename);
            return newFixedLengthResponse(Response.Status.OK, mimeType, in, in.available());
        } catch (IOException e) {
            e.printStackTrace();
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_HTML, e.getMessage());
        }
    }

    /**
     * 处理上传文件请求。
     *
     * @param session   会话。
     * @param fieldName 文件所在的字段名。
     * @return 响应数据。
     */
    private Response upload(IHTTPSession session, String fieldName) {
        Map<String, String> files = new HashMap<>();
        try {
            session.parseBody(files);
        } catch (IOException | ResponseException e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_HTML, e.getMessage());
        }

        Map<String, String> params = session.getParms();
        if (files.containsKey(fieldName) && params.containsKey(fieldName)) {
            File tmp = new File(files.get(fieldName));
            File target = new File(Utils.Path_cache_original, params.get(fieldName));
            // 刷新本地图片列表
            EventBus.getDefault().post(new LocalImageChangedMsg());
            return copyFile(tmp, target) ?
                    newFixedLengthResponse(Response.Status.OK, "application/json", "{\"code\":0}") :
                    newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_HTML, "Copy file failed.");
        }
        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_HTML, "Unknown file or params");
    }

    private boolean copyFile(@NonNull File src, @NonNull File target) {
        if (!src.isFile() || !src.exists())
            return false;
        if (target.isFile() && target.exists())
            if (!target.delete()) return false;
        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[2048];
            int len;
            while ((len = in.read(buffer)) != -1)
                out.write(buffer, 0, len);
            out.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
