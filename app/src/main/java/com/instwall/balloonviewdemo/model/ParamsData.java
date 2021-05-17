package com.instwall.balloonviewdemo.model;

public class ParamsData {
    private String sid ;
    private String words;
    private long synctime;
    private String acttype;
    private String tpltype;

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getWords() {
        return words;
    }

    public void setWords(String words) {
        this.words = words;
    }

    public long getSynctime() {
        return synctime;
    }

    public void setSynctime(long synctime) {
        this.synctime = synctime;
    }

    public String getActtype() {
        return acttype;
    }

    public void setActtype(String acttype) {
        this.acttype = acttype;
    }

    public String getTpltype() {
        return tpltype;
    }

    public void setTpltype(String tpltype) {
        this.tpltype = tpltype;
    }
}

/**
 {
 "datafrom":"scenicSpots",
 "data_list":[
 {
 "sid":"002",
 "words":"祝大家节日快乐",
 "synctime":1618566725,
 "playtime":15,
 "acttype":"showWords",
 "tpltype":"A"
 },
 {
 "sid":"003",
 "words":"祝大家节日快乐",
 "synctime":1618566735,
 "playtime":15,
 "acttype":"showWords",
 "tpltype":"C"
 },
 {
 "sid":"004",
 "words":"祝大家节日快乐",
 "synctime":1618566745,
 "playtime":15,
 "acttype":"showWords",
 "tpltype":"B"
 },
 {
 "sid":"005",
 "words":"祝大家节日快乐",
 "synctime":1618566755,
 "playtime":15,
 "acttype":"showWords",
 "tpltype":"D"
 }
 ]
 }
 */
