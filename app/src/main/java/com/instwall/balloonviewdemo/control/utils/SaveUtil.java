package com.instwall.balloonviewdemo.control.utils;

import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.instwall.balloonviewdemo.model.ParamsData;

import java.util.ArrayList;
import java.util.List;

import ashy.earl.common.app.App;

import static android.content.Context.MODE_PRIVATE;

public class SaveUtil {

    private static SharedPreferences mSpf;
    private static final String KEY = "dataStatus";
    private static final String NAME = "tree";


    public static void writeInfo(String json){
        mSpf = App.getAppContext().getSharedPreferences(NAME,MODE_PRIVATE);
        SharedPreferences.Editor editor = mSpf.edit();
        editor.putString(KEY,json);
        editor.commit();
    }

    public static String readInfo(){
        mSpf = App.getAppContext().getSharedPreferences(NAME,MODE_PRIVATE);
        String info = mSpf.getString(KEY,"");
        return info;
    }



    public static class Rpt_data {

        private String sid;
        private String status;
        private long syncTime;

        public Rpt_data(String sid, String status) {
            this.sid = sid;
            this.status = status;
            syncTime = System.currentTimeMillis() / 1000;
        }

        public void setSid(String sid) {
            this.sid = sid;
        }

        public String getSid() {
            return sid;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }

        public long getSyncTime() {
            return syncTime;
        }

        public void setSyncTime(long syncTime) {
            this.syncTime = syncTime;
        }
    }
} 