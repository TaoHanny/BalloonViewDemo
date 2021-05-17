package com.instwall.balloonviewdemo.view;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.instwall.balloonviewdemo.R;
import com.instwall.balloonviewdemo.control.SaveTaskManager;
import com.instwall.balloonviewdemo.model.ParamsData;
import com.instwall.balloonviewdemo.view.balloon.KsgLikeView;
import com.instwall.balloonviewdemo.view.custom.BalloonView;
import com.instwall.balloonviewdemo.view.custom.FlyView;


import com.instwall.balloonviewdemo.view.pathlayout.PathInfo;
import com.instwall.balloonviewdemo.view.pathlayout.TreePathGenerator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class MainActivity extends AppCompatActivity {


    private int mApplyFlag = PathInfo.APPLY_FLAG_DRAW_AND_TOUCH;

    private int mClipType = PathInfo.CLIP_TYPE_IN;
    private BalloonView flyView;
    private KsgLikeView mLikeView;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private SaveTaskManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        mLikeView = findViewById(R.id.fly_view);
        flyView = findViewById(R.id.fly_view);

        new PathInfo.Builder(new TreePathGenerator(), flyView)
                .setApplyFlag(mApplyFlag)
                .setClipType(mClipType)
                .create()
                .apply();

        manager = SaveTaskManager.getInstance();
        manager.addListener(onSaveListener);
        manager.getHttpTask();
        flyView.setSnowDuration(200);
        mHandler.postDelayed(mLikeRunnable, 1000);
    }

    private final Runnable mLikeRunnable = new Runnable() {
        @Override
        public void run() {
            flyView.startAnimation();
            initData();
        }
    };


    @Override
    protected void onStop() {
        super.onStop();
        manager.remoteListener(onSaveListener);
    }

    private void initData(){
        try {
            JSONObject jsonObject = new JSONObject(JSON);
            JSONArray jsonArray = jsonObject.optJSONArray("data_list");
            if(jsonArray == null || jsonArray.length() == 0) return;
            Gson gson = new Gson();
            List<ParamsData> dataList = gson.fromJson(jsonArray.toString(), new TypeToken<List<ParamsData>>(){}.getType());
            flyView.pushSnows(dataList);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private SaveTaskManager.OnSaveListener onSaveListener = new SaveTaskManager.OnSaveListener() {
        @Override
        public void onGetHttpData(String json) {

        }
    };
    private String JSON = "{\n" +
            "    \"datafrom\":\"scenicSpots\",\n" +
            "    \"data_list\":[\n" +
            "        {\n" +
            "            \"sid\":\"002\",\n" +
            "            \"words\":\"祝大家节日快乐\",\n" +
            "            \"synctime\":1618566725,\n" +
            "            \"playtime\":15,\n" +
            "            \"acttype\":\"showWords\",\n" +
            "            \"tpltype\":\"A\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"sid\":\"003\",\n" +
            "            \"words\":\"祝大家节日快乐\",\n" +
            "            \"synctime\":1618566735,\n" +
            "            \"playtime\":15,\n" +
            "            \"acttype\":\"showWords\",\n" +
            "            \"tpltype\":\"C\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"sid\":\"004\",\n" +
            "            \"words\":\"祝大家节日快乐\",\n" +
            "            \"synctime\":1618566745,\n" +
            "            \"playtime\":15,\n" +
            "            \"acttype\":\"showWords\",\n" +
            "            \"tpltype\":\"B\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"sid\":\"005\",\n" +
            "            \"words\":\"祝大家节日快乐\",\n" +
            "            \"synctime\":1618566755,\n" +
            "            \"playtime\":15,\n" +
            "            \"acttype\":\"showWords\",\n" +
            "            \"tpltype\":\"D\"\n" +
            "        }\n" +
            "    ]\n" +
            "}";







}