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
    private static final String CONFIG_KEY = "config";
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


    public static void writeConfigJson(String json){
        mSpf = App.getAppContext().getSharedPreferences(NAME,MODE_PRIVATE);
        SharedPreferences.Editor editor = mSpf.edit();
        editor.putString(CONFIG_KEY,json);
        editor.commit();
    }

    public static String readConfigInfo(){
        mSpf = App.getAppContext().getSharedPreferences(NAME,MODE_PRIVATE);
        String info = mSpf.getString(CONFIG_KEY,JSON);
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

//    private static final String JSON = "{\"coordinate\":[{\"x\":440,\"y\":0},{\"x\":840,\"y\":0},{\"x\":973,\"y\":67}," +
//            "{\"x\":973,\"y\":267},{\"x\":907,\"y\":400},{\"x\":813,\"y\":533},{\"x\":747,\"y\":720},{\"x\":533,\"y\":720}," +
//            "{\"x\":467,\"y\":533},{\"x\":333,\"y\":400},{\"x\":307,\"y\":267},{\"x\":307,\"y\":67},{\"x\":293,\"y\":0}],\"max\":10}";

//    private static final String JSON = "{\"coordinate\":[{\"x\":533,\"y\":712},{\"x\":532,\"y\":450},{\"x\":5,\"y\":446},{\"x\":104,\"y\":182}," +
//            "{\"x\":318,\"y\":89},{\"x\":697,\"y\":4},{\"x\":1047,\"y\":106},{\"x\":1234,\"y\":270},{\"x\":1240,\"y\":412},{\"x\":1215,\"y\":447}," +
//            "{\"x\":934,\"y\":451},{\"x\":929,\"y\":709},{\"x\":913,\"y\":715},{\"x\":536,\"y\":714},{\"x\":533,\"y\":686}],\"max\":12}";

    private static final String JSON = "{\"coordinate\":[{\"x\":319,\"y\":718},{\"x\":399,\"y\":628},{\"x\":427,\"y\":585},{\"x\":464,\"y\":539}," +
            "{\"x\":12,\"y\":522},{\"x\":16,\"y\":398},{\"x\":85,\"y\":317},{\"x\":227,\"y\":157},{\"x\":466,\"y\":54},{\"x\":688,\"y\":12}," +
            "{\"x\":1027,\"y\":116},{\"x\":1180,\"y\":234},{\"x\":1255,\"y\":328},{\"x\":1251,\"y\":520},{\"x\":957,\"y\":533}," +
            "{\"x\":981,\"y\":672},{\"x\":1070,\"y\":711},{\"x\":1073,\"y\":715},{\"x\":319,\"y\":715}],\"max\":41,\"scale\":1.0}";
}