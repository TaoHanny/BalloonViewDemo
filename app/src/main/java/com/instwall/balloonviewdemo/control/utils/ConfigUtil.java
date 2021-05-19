package com.instwall.balloonviewdemo.control.utils;

import android.content.SharedPreferences;

import ashy.earl.common.app.App;

import static android.content.Context.MODE_PRIVATE;

public class ConfigUtil {

    private static SharedPreferences mSpf;
    private static final String KEY = "ConfigJson";
    private static final String NAME = "config";

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
} 