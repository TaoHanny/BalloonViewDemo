package com.instwall.balloonviewdemo;

import android.app.Application;

import com.instwall.balloonviewdemo.core.shell.SimpleRemoteShell;

import ashy.earl.common.app.App;
import ashy.earl.common.util.L;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        App.appOnCreate(this);
        L.setupLogger(new L.AndLog(new L.AndroidLog(), new L.FileLog()));
        SimpleRemoteShell.get().start();
    }
}