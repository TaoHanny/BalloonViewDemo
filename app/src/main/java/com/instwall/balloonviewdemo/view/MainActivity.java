package com.instwall.balloonviewdemo.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.VideoView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.instwall.balloonviewdemo.R;
import com.instwall.balloonviewdemo.control.SaveTaskManager;
import com.instwall.balloonviewdemo.control.SaveTaskManager.OnSaveListener;
import com.instwall.balloonviewdemo.core.WebService;
import com.instwall.balloonviewdemo.model.ParamsData;
import com.instwall.balloonviewdemo.view.custom.BalloonView;

import com.instwall.balloonviewdemo.view.pathlayout.PathInfo;
import com.instwall.balloonviewdemo.view.pathlayout.TreePathGenerator;
import com.instwall.im.ImClient;
import com.instwall.im.ImListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import ashy.earl.common.util.NetworkChangeHelper;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    private VideoView videoView;
    private BalloonView flyView;
    private SaveTaskManager manager;
    private final ImClient mImClient = ImClient.get();
    private NetworkChangeHelper mNetwork = NetworkChangeHelper.get();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        videoView = findViewById(R.id.videoView);
        flyView = findViewById(R.id.fly_view);
        manager = SaveTaskManager.getInstance();

        manager.addListener(onSaveListener);
        mImClient.addListener(mImListener);
        mNetwork.addNetworkListener(mNetworkListener);

        flyView.setSnowDuration(200);
        handler.sendEmptyMessageDelayed(MSG_FLY_START,1000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        WebService.start(this);
        manager.reportDataTask();
        videoView.setVideoURI(Uri.parse("android.resource://"+getPackageName()+"/raw/tree"));
        videoView.start();
        WebService.setListener(new WebService.OnLocalListener() {
            @Override
            public void onPostData(String json) {
                sendLayoutPath(json);
                manager.saveConfigJsonTask(json);
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mPlayer) {
                // TODO Auto-generated method stub
                mPlayer.start();
                mPlayer.setLooping(true);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        manager.remoteListener(onSaveListener);
        WebService.stop(this);
        mImClient.removeListener(mImListener);
    }


    private final ImListener mImListener = new ImListener() {

        @Override
        public void onStateChanged(int state) { }

        @Override
        public void onNewMsg(@NonNull String from, @NonNull String msg) {
            JSONObject json = ImClient.optServerCmdJson(msg);
            if (json == null || !"pt_showdatas_forbless".equals(json.optString("cmd"))) {
                return;
            }
            manager.getHttpTask();
        }
    };

    private final int MSG_PATH = 0x11;
    private final int MSG_FLY_START = 0x12;
    @SuppressLint("HandlerLeak")
    final Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if(msg.what == MSG_PATH){
                String json = (String) msg.obj;
                new PathInfo.Builder(new TreePathGenerator(json), flyView)
                        .setApplyFlag(PathInfo.APPLY_FLAG_DRAW_AND_TOUCH)
                        .setClipType(PathInfo.CLIP_TYPE_IN)
                        .create()
                        .apply();
            }else if (MSG_FLY_START == msg.what){
                flyView.startAnimation();
            }
        }
    };

    private static boolean IS_REBOOT_OR_NETWOEK = true;
    private OnSaveListener onSaveListener = new OnSaveListener() {
        @Override
        public void onGetHttpData(int type,String json) {
            if(type == OnSaveListener.TYPE_SNOW)
                updateData(json);
            else if(type == OnSaveListener.TYPE_CONFIG_JSON){
                sendLayoutPath(json);
            }
        }

        @Override
        public void onReportStatus(int status) {
            if(OnSaveListener.REPORT_DONE == status){
                if(IS_REBOOT_OR_NETWOEK){
                    manager.getHttpTask();
                    manager.getConfigJsonTask();
                    IS_REBOOT_OR_NETWOEK = false;
                }
            }
        }
    };

    private void sendLayoutPath(String json) {
        Message message = new Message();
        message.what = MSG_PATH;
        message.obj = json;
        handler.sendMessage(message);
    }

    private void updateData(String json){
        try {
            JSONArray jsonArray = new JSONArray(JSON);
            if(jsonArray == null || jsonArray.length() == 0) return;
            Gson gson = new Gson();
            List<ParamsData> dataList = gson.fromJson(jsonArray.toString(), new TypeToken<List<ParamsData>>(){}.getType());
            flyView.pushSnows(dataList);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private NetworkChangeHelper.NetworkListener mNetworkListener = new NetworkChangeHelper.NetworkListener() {
        @Override
        public void onNetworkChanged(boolean hasActiveNetwork, String type, String name) {
            if(hasActiveNetwork){
                Log.d(TAG, "onNetworkChanged: has active network");
                manager.reportDataTask();
            }else {
                Log.d(TAG, "onNetworkChanged: hasnot active network");
                IS_REBOOT_OR_NETWOEK = true;
            }
        }
    };



    private String JSON = "[{\"status\":\"waiting\",\"showWords\":\"祝大家节日快乐\",\"sid\":\"test003\",\"playtime\":15,\"tpltype\":\"C\",\"synctime\":" +
            "\"1618566735\"},{\"status\":\"waiting\",\"showWords\":\"祝大家节日快乐\",\"sid\":\"test004\",\"playtime\":15,\"tpltype\":\"C\",\"synctime\":" +
            "\"1618566735\"},{\"status\":\"waiting\",\"showWords\":\"祝大家节日快乐\",\"sid\":\"test005\",\"playtime\":15,\"tpltype\":\"C\",\"synctime\":" +
            "\"1618566735\"},{\"status\":\"waiting\",\"showWords\":\"祝大家节日快乐\",\"sid\":\"test006\",\"playtime\":15,\"tpltype\":\"C\",\"synctime\":" +
            "\"1618566735\"},{\"status\":\"waiting\",\"showWords\":\"祝大家节日快乐\",\"sid\":\"test007\",\"playtime\":15,\"tpltype\":\"C\",\"synctime\":" +
            "\"1618566735\"},{\"status\":\"waiting\",\"showWords\":\"祝大家节日快乐\",\"sid\":\"test008\",\"playtime\":15,\"tpltype\":\"C\",\"synctime\":" +
            "\"1618566735\"},{\"status\":\"waiting\",\"showWords\":\"祝大家节日快乐\",\"sid\":\"test009\",\"playtime\":15,\"tpltype\":\"C\",\"synctime\":" +
            "\"1618566735\"},{\"status\":\"waiting\",\"showWords\":\"祝大家节日快乐\",\"sid\":\"test0010\",\"playtime\":15,\"tpltype\":\"C\",\"synctime\":" +
            "\"1618566735\"},{\"status\":\"waiting\",\"showWords\":\"祝大家节日快乐\",\"sid\":\"test011\",\"playtime\":15,\"tpltype\":\"C\",\"synctime\":" +
            "\"1618566735\"}]";
}