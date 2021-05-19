package com.instwall.balloonviewdemo.view.pathlayout;

import android.graphics.Path;
import android.util.Log;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.instwall.balloonviewdemo.model.ParamsData;
import com.instwall.balloonviewdemo.view.custom.BalloonView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TreePathGenerator implements PathGenerator{

    private final List<Balloon> balloonList = new ArrayList<>();

    public TreePathGenerator(String json){
        List<Balloon> cacheList = getBalloonList(json);
        if(cacheList!=null && cacheList.size()>=3){
            balloonList.clear();
            for (int i = 0; i < cacheList.size(); i++) {
                Balloon balloon = cacheList.get(i);
                balloon.x = (balloon.x * 3) / 2;
                balloon.y = (balloon.y * 3) / 2;
                balloonList.add(balloon);
            }
        }
        Log.d("TreePathGenerator", "TreePathGenerator() list = "+balloonList.toString());
    }

    @Override
    public Path generatePath(Path old, View view, int width, int height) {
        if (old == null) {
            old = new Path();
        } else {
            old.reset();
        }
        int count = 0;
        List<Balloon> list = balloonList;
        Log.d("TreePathGenerator", "generatePath() list = "+balloonList.toString());
        for (int i = 0; list.size()!=0 && i <= list.size(); i++) {
            Balloon balloon;
            if(i==list.size()){
                balloon = list.get(0);
            }else {
                balloon = list.get(i);
            }
            if(count == 0){
                old.moveTo(balloon.x,balloon.y);
            }else {
                old.lineTo(balloon.x,balloon.y);
            }
            count++;
        }
        old.close();
        return old;
    }

    public List<Balloon> getBalloonList(String json){
        try {
            JSONObject jsonObject = new JSONObject(json);
            String jsonArr = jsonObject.getString("coordinate");
            Gson gson = new Gson();
            List<Balloon> dataList = gson.fromJson(jsonArr, new TypeToken<List<Balloon>>(){}.getType());
            if(dataList!=null && dataList.size() >= 3 ){

            }
            return dataList;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<Balloon> getList(){

        List<Balloon> list = new ArrayList<>();
        list.add(new Balloon(660,0));
        list.add(new Balloon(1260,0));
        list.add(new Balloon(1460,100));
        list.add(new Balloon(1460,400));
        list.add(new Balloon(1360,600));
        list.add(new Balloon(1220,800));
        list.add(new Balloon(1120,1080));

        list.add(new Balloon(800,1080));
        list.add(new Balloon(700,800));
        list.add(new Balloon(560,600));
        list.add(new Balloon(460,400));
        list.add(new Balloon(460,100));
        list.add(new Balloon(660,0));
       return list;
    }


    class Balloon{
        public int x;
        public int y;

        public Balloon(int x , int y){
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "Balloon{" +
                    "x=" + x +
                    ", y=" + y +
                    '}';
        }
    }
}