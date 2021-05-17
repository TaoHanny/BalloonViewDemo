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

import java.util.ArrayList;
import java.util.List;

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

    private void reportDataTask(){
        ReportTask task = new ReportTask();
        handler.post(task);
    }

    public void getHttpTask(){
        GetHttpTask task = new GetHttpTask();
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
                list = gson.fromJson(info, new TypeToken<List<Rpt_data>>() {}.getType());
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
            ReportHttpApi reportHttpApi = new ReportHttpApi(info);
            reportHttpApi.makeRequest(new Callback<String, NetCoreException>() {
                @Override
                public void onResult(String rst, NetCoreException e) {
                    if(e != null){
                        Log.d(TAG, "onResult() e = "+e.toString());
                    }
                    if(!TextUtils.isEmpty(rst)){
                        Log.d(TAG, "onResult() rst = "+rst);
                        List<Rpt_data> list = new ArrayList<>();
                        Gson gson = new Gson();
                        if(info!=null && !"".equals(info)) {
                            list = gson.fromJson(info, new TypeToken<List<Rpt_data>>() {}.getType());
                            for (Rpt_data data : list){
                                long currentTime = System.currentTimeMillis() / 1000;
                                long difference = currentTime - data.getSyncTime();
                                //大于两小时，清除本地缓存
                                if(difference > 7200){
                                    list.remove(data);
                                }
                            }
                            if(list.size()>0){
                                String json = gson.toJson(list,new TypeToken<List<Rpt_data>>(){}.getType());
                                SaveUtil.writeInfo(json);
                            }
                        }
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
                        Log.d(TAG, "onResult() e = "+e.toString());
                    }
                    if(!TextUtils.isEmpty(rst)){
                        Log.d(TAG, "onResult() rst = "+rst);
                        notifyData(rst);
                    }
                }
            });
        }
    }


    private List<OnSaveListener> listenerList = new ArrayList<>();

    public void addListener(OnSaveListener listener){
        listenerList.add(listener);
    }

    public void remoteListener(OnSaveListener listener){
        listenerList.remove(listener);
    }

    public void notifyData(String json){
        for (OnSaveListener listener : listenerList) {
            listener.onGetHttpData(json);
        }
    }


    public interface OnSaveListener{
        void onGetHttpData(String json);

    }

}