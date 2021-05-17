package com.instwall.balloonviewdemo;

import android.app.Application;

import ashy.earl.common.app.App;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        App.appOnCreate(this);
    }
}