 package com.instwall.balloonviewdemo.core;

 import android.app.Service;
 import android.content.Context;
 import android.content.Intent;
 import android.os.IBinder;
 import android.text.TextUtils;
 import android.util.Log;

 import com.instwall.balloonviewdemo.model.Constants;

 import com.koushikdutta.async.AsyncServer;

 import com.koushikdutta.async.http.body.AsyncHttpRequestBody;
 import com.koushikdutta.async.http.server.AsyncHttpServer;
 import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
 import com.koushikdutta.async.http.server.AsyncHttpServerResponse;

 import java.io.BufferedInputStream;
 import java.io.ByteArrayOutputStream;
 import java.io.IOException;


 public class WebService extends Service {

     private final String TAG = "WebService";

     static final String ACTION_START_WEB_SERVICE = "com.instwall.colortools.action.START_WEB_SERVICE";
     static final String ACTION_STOP_WEB_SERVICE = "com.instwall.colortools.action.STOP_WEB_SERVICE";

     private static final String TEXT_CONTENT_TYPE = "text/html;charset=utf-8";
     private static final String CSS_CONTENT_TYPE = "text/css;charset=utf-8";
     private static final String BINARY_CONTENT_TYPE = "application/octet-stream";
     private static final String JS_CONTENT_TYPE = "application/javascript";
     private static final String PNG_CONTENT_TYPE = "application/x-png";
     private static final String JPG_CONTENT_TYPE = "application/jpeg";
     private static final String SWF_CONTENT_TYPE = "application/x-shockwave-flash";
     private static final String WOFF_CONTENT_TYPE = "application/x-font-woff";
     private static final String TTF_CONTENT_TYPE = "application/x-font-truetype";
     private static final String SVG_CONTENT_TYPE = "image/svg+xml";
     private static final String EOT_CONTENT_TYPE = "image/vnd.ms-fontobject";
     private static final String MP3_CONTENT_TYPE = "audio/mp3";
     private static final String MP4_CONTENT_TYPE = "video/mpeg4";
     private final AsyncHttpServer server = new AsyncHttpServer();
     private final AsyncServer mAsyncServer = new AsyncServer();


     public static void start(Context context) {
         Intent intent = new Intent(context, WebService.class);
         intent.setAction(ACTION_START_WEB_SERVICE);
         context.startService(intent);
         Log.d("WebService", "start: ");
     }

     public static void stop(Context context) {
         Intent intent = new Intent(context, WebService.class);
         intent.setAction(ACTION_STOP_WEB_SERVICE);
         context.startService(intent);
     }

     @Override
     public IBinder onBind(Intent intent) {
         return null;
     }

     @Override
     public int onStartCommand(Intent intent, int flags, int startId) {
         if (intent != null) {
             String action = intent.getAction();
             if (ACTION_START_WEB_SERVICE.equals(action)) {
                 startServer();
             } else if (ACTION_STOP_WEB_SERVICE.equals(action)) {
                 stopSelf();
             }
         }
         return super.onStartCommand(intent, flags, startId);
     }

     @Override
     public void onDestroy() {
         super.onDestroy();
         if (server != null) {
             server.stop();
         }
         if (mAsyncServer != null) {
             mAsyncServer.stop();
         }
     }

     private void startServer() {
         server.get("/images/.*", this::sendResources);
         server.get("/scripts/.*", this::sendResources);
         server.get("/css/.*", this::sendResources);
         //index page
         server.get("/", (AsyncHttpServerRequest request, AsyncHttpServerResponse response) -> {
             try {
                 response.send(getIndexContent());
             } catch (IOException e) {
                 e.printStackTrace();
                 response.code(500).end();
             }
         });


         //upload
         server.post("/*", (AsyncHttpServerRequest request, AsyncHttpServerResponse response) -> {
                AsyncHttpRequestBody requestBody = request.getBody();

                String json =  requestBody.get().toString();
                if(json.length()<10) response.code(400).send("请输入坐标和最大标签");
                json = json.substring(7);
                json = json.substring(0,json.length()-2);
                if(listener!=null){
                    listener.onPostData(json);
                }
                     Log.d(TAG, "startServer: "+json);
            response.code(200).send("调整成功");
            }
         );

         server.listen(mAsyncServer, Constants.HTTP_PORT);
     }

     private String getIndexContent() throws IOException {
         BufferedInputStream bInputStream = null;
         try {
             bInputStream = new BufferedInputStream(getAssets().open("index.html"));
             ByteArrayOutputStream baos = new ByteArrayOutputStream();
             int len = 0;
             byte[] tmp = new byte[10240];
             while ((len = bInputStream.read(tmp)) > 0) {
                 baos.write(tmp, 0, len);
             }
             return new String(baos.toByteArray(), "utf-8");
         } catch (IOException e) {
             e.printStackTrace();
             throw e;
         } finally {
             if (bInputStream != null) {
                 try {
                     bInputStream.close();
                 } catch (IOException e) {
                     e.printStackTrace();
                 }
             }
         }
     }

     private void sendResources(final AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
         try {
             String fullPath = request.getPath();
             fullPath = fullPath.replace("%20", " ");
             String resourceName = fullPath;
             if (resourceName.startsWith("/")) {
                 resourceName = resourceName.substring(1);
             }
             if (resourceName.indexOf("?") > 0) {
                 resourceName = resourceName.substring(0, resourceName.indexOf("?"));
             }
             if (!TextUtils.isEmpty(getContentTypeByResourceName(resourceName))) {
                 response.setContentType(getContentTypeByResourceName(resourceName));
             }
             Log.d(TAG, "sendResources() resourceName = "+resourceName);
             BufferedInputStream bInputStream = new BufferedInputStream(getAssets().open(resourceName));
             response.sendStream(bInputStream, bInputStream.available());
         } catch (IOException e) {
             e.printStackTrace();
             response.code(404).end();
         }
     }

     private String getContentTypeByResourceName(String resourceName) {
         if (resourceName.endsWith(".css")) {
             return CSS_CONTENT_TYPE;
         } else if (resourceName.endsWith(".js")) {
             return JS_CONTENT_TYPE;
         } else if (resourceName.endsWith(".swf")) {
             return SWF_CONTENT_TYPE;
         } else if (resourceName.endsWith(".png")) {
             return PNG_CONTENT_TYPE;
         } else if (resourceName.endsWith(".jpg") || resourceName.endsWith(".jpeg")) {
             return JPG_CONTENT_TYPE;
         } else if (resourceName.endsWith(".woff")) {
             return WOFF_CONTENT_TYPE;
         } else if (resourceName.endsWith(".ttf")) {
             return TTF_CONTENT_TYPE;
         } else if (resourceName.endsWith(".svg")) {
             return SVG_CONTENT_TYPE;
         } else if (resourceName.endsWith(".eot")) {
             return EOT_CONTENT_TYPE;
         } else if (resourceName.endsWith(".mp3")) {
             return MP3_CONTENT_TYPE;
         } else if (resourceName.endsWith(".mp4")) {
             return MP4_CONTENT_TYPE;
         }
         return "";
     }

     private static OnLocalListener listener;

     public static void setListener(OnLocalListener localListener){
         listener = localListener;
     }

     public interface OnLocalListener{
         void onPostData(String json);
     }

 }