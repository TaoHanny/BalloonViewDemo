package com.instwall.balloonviewdemo.view.pathlayout;

import android.graphics.Path;
import android.view.View;

import com.instwall.balloonviewdemo.view.custom.BalloonView;

import java.util.ArrayList;
import java.util.List;

public class TreePathGenerator implements PathGenerator{
    @Override
    public Path generatePath(Path old, View view, int width, int height) {
        if (old == null) {
            old = new Path();
        } else {
            old.reset();
        }
        int count = 0;
        List<Balloon> list = getList();
        for (Balloon balloon : list){
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
    }
}