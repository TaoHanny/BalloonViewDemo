package com.instwall.balloonviewdemo.control;

import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.instwall.balloonviewdemo.control.utils.GetHttpApi;
import com.instwall.balloonviewdemo.control.utils.ReportHttpApi;
import com.instwall.balloonviewdemo.control.utils.SaveUtil;
import com.instwall.balloonviewdemo.control.utils.SaveUtil.Rpt_data;
import com.instwall.balloonviewdemo.view.custom.BalloonView.Snow;
import com.instwall.net.NetCoreException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ashy.earl.common.util.L;
import ashy.earl.net.Callback;

public class SaveTaskManager {

    private final String TAG = "SaveTaskManager";
    private HandlerThread handlerThread = new HandlerThread("SaveTask");
    private Handler handler;
    private SaveTaskManager(){
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }
    private static SaveTaskManager saveTaskManager;

    public static SaveTaskManager getInstance(){
        if(saveTaskManager!=null) return saveTaskManager;
        synchronized (SaveTaskManager.class){
            if(saveTaskManager==null){
                saveTaskManager = new SaveTaskManager();
            }
        }
        return saveTaskManager;
    }


    public void saveSnowTask(Snow snow){
        SaveSnowTask saveSnowTask = new SaveSnowTask(snow);
        handler.post(saveSnowTask);
    }

    public void reportDataTask(){
        ReportTask task = new ReportTask();
        handler.post(task);
    }

    public void getHttpTask(){
        GetHttpTask task = new GetHttpTask();
        handler.post(task);
    }

    public void saveConfigJsonTask(String json){
        SaveConfigJsonTask task = new SaveConfigJsonTask(json);
        handler.post(task);
    }

    public void getConfigJsonTask(){
        GetConfigJsonTask task = new GetConfigJsonTask();
        handler.post(task);
    }


    class SaveSnowTask implements Runnable{
        Snow snow;
        public SaveSnowTask(Snow snow){
            this.snow = snow;
        }
        @Override
        public void run() {
            String info = SaveUtil.readInfo();
            List<Rpt_data> list = new ArrayList<>();
            Gson gson = new Gson();
            if(info!=null && !"".equals(info)) {
                list = gson.fromJson(info, new TypeToken<List<Rpt_data>>(){}.getType());
            }
            list.add(new Rpt_data(snow.sid,"done"));
            String json = gson.toJson(list,new TypeToken<List<Rpt_data>>(){}.getType());
            SaveUtil.writeInfo(json);
            reportDataTask();
        }
    }

    class ReportTask implements Runnable{
        @Override
        public void run() {
            String info = SaveUtil.readInfo();
            Log.d(TAG, "ReportTask() -> run() info = " +info);
            if(info==null || "".equals(info)) {
                notifyStatus(OnSaveListener.REPORT_DONE);
                return;
            }
            ReportHttpApi reportHttpApi = new ReportHttpApi(info);
            reportHttpApi.makeRequest(new Callback<String, NetCoreException>() {
                @Override
                public void onResult(String rst, NetCoreException e) {
                    if(e != null){
                        L.d(TAG, "onResult() e = "+e.toString());
                        notifyStatus(OnSaveListener.REPORT_ERROR);
                        handler.postDelayed(new ReportTask(),5000);
                    }
                    if(!TextUtils.isEmpty(rst)){
                        L.d(TAG, "onResult() rst = "+rst);
                        List<Rpt_data> list;
                        Gson gson = new Gson();
                        if(info!=null && !"".equals(info)) {
                            list = gson.fromJson(info, new TypeToken<List<Rpt_data>>(){}.getType());
                            if(list==null || list.size() <= 0) return;
                            List<Rpt_data> cacheList = new ArrayList<>(list);
                            for (int i = 0; i < list.size(); i++){
                                Rpt_data data = list.get(i);
                                long currentTime = System.currentTimeMillis() / 1000;
                                long difference = currentTime - data.getSyncTime();
                                //大于两小时，清除本地缓存
                                if(difference > 7200){
                                    cacheList.remove(i);
                                }
                            }
                            String cacheJson = gson.toJson(cacheList,new TypeToken<List<Rpt_data>>(){}.getType());
                            Log.d(TAG, "ReportTask() -> cacheJSon = "+cacheJson);
                            SaveUtil.writeInfo(cacheJson);
                        }
                        notifyStatus(OnSaveListener.REPORT_DONE);
                    }
                }
            });
        }
    }

    class GetHttpTask implements Runnable{

        @Override
        public void run() {
            GetHttpApi getHttpApi = new GetHttpApi();
            getHttpApi.makeRequest(new Callback<String, NetCoreException>() {
                @Override
                public void onResult(String rst, NetCoreException e) {
                    if(e != null){
                        L.d(TAG, "onResult() e = "+e.toString());
                    }
                    if(!TextUtils.isEmpty(rst)){
                        L.d(TAG, "onResult() rst = "+rst);
                        try {
                            JSONObject bodyJson = new JSONObject(rst);
                            JSONArray dataArray = bodyJson.optJSONArray("data");
                            if(dataArray==null || dataArray.length()<=0) return;

                            JSONObject itemJson = dataArray.getJSONObject(0);
                            int wordsCount = itemJson.optInt("words_waitingcnt");
                            if(wordsCount <= 0) return;

                            JSONArray wordsList = itemJson.optJSONArray("words_list");
                            if(wordsList==null || wordsList.length()<=0) return;

                            notifyData(OnSaveListener.TYPE_SNOW,wordsList.toString());
                        } catch (JSONException jsonException) {
                            jsonException.printStackTrace();
                        }
                    }
                }
            });
        }
    }


    class GetConfigJsonTask implements Runnable{

        @Override
        public void run() {
            String json = SaveUtil.readConfigInfo();
            L.d(TAG, "GetConfigJsonTask() -> json = "+json);
            if(json != null && !"".equals(json)){
                notifyData(OnSaveListener.TYPE_CONFIG_JSON,json);
            }
        }
    }

    class SaveConfigJsonTask implements Runnable{
        String json;
        public SaveConfigJsonTask(String json){
            this.json = json;
        }
        @Override
        public void run() {
            SaveUtil.writeConfigJson(json);
            getConfigJsonTask();
        }
    }


    private List<OnSaveListener> listenerList = new ArrayList<>();

    public void addListener(OnSaveListener listener){
        listenerList.add(listener);
    }

    public void remoteListener(OnSaveListener listener){
        listenerList.remove(listener);
    }

    public void notifyData(int type,String json){
        for (OnSaveListener listener : listenerList) {
            listener.onGetHttpData(type,json);
        }
    }

    public void notifyStatus(int status){
        for (OnSaveListener listener : listenerList) {
            listener.onReportStatus(status);
        }
    }


    public interface OnSaveListener{
        static final int REPORT_DONE = 1;
        static final int REPORT_ERROR = 0;
        static final int TYPE_SNOW = 4;
        static final int TYPE_CONFIG_JSON = 5;
        void onGetHttpData(int dataType,String json);
        void onReportStatus(int status);
    }

}