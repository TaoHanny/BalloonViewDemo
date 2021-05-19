package com.instwall.balloonviewdemo.model;

public class ParamsData {
    private String status;
    private String showWords;
    private String sid;
    private int playtime;
    private String tpltype;
    private String synctime;
    public void setStatus(String status) {
        this.status = status;
    }
    public String getStatus() {
        return status;
    }

    public void setShowWords(String showWords) {
        this.showWords = showWords;
    }
    public String getShowWords() {
        return showWords;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }
    public String getSid() {
        return sid;
    }

    public void setPlaytime(int playtime) {
        this.playtime = playtime;
    }
    public int getPlaytime() {
        return playtime;
    }

    public void setTpltype(String tpltype) {
        this.tpltype = tpltype;
    }
    public String getTpltype() {
        return tpltype;
    }

    public void setSynctime(String synctime) {
        this.synctime = synctime;
    }
    public String getSynctime() {
        return synctime;
    }
}

/**
 {
 "status":"waiting",
 "showWords":"祝大家节日快乐",
 "sid":"test003",
 "playtime":15,
 "tpltype":"C",
 "synctime":"1618566735"
 }
 */
