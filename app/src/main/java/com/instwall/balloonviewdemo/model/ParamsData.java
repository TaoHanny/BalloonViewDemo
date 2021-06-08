package com.instwall.balloonviewdemo.model;

public class ParamsData {
    private String status;
    private String words = " ";
    private String uname = " ";
    private String uicon;
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

    public void setWords(String showWords) {
        this.words = showWords;
    }
    public String getWords() {
        return words;
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

    public String getUname() {
        return uname;
    }

    public void setUname(String uname) {
        this.uname = uname;
    }

    public String getUicon() {
        return uicon;
    }

    public void setUicon(String uicon) {
        this.uicon = uicon;
    }
}

/**
 {
 "status":"waiting",
 "uname":"M\u674e\u5b50",
 "words":"\u751f\u65e5\u5feb\u4e50\u54c8\u54c8\u54c8\u54c8",
 "sid":"20210604145401EFKW",
 "playtime":5,
 "tpltype":"C",
 "restype":"showWords",
 "synctime":"1622789647",
 "uicon":"https://thirdwx.qlogo.cn/mmopen/vi_32/DYAIOgq83eqaMgaRyPApTY1Nhsz0upkibsX8icEnYjJXPHMK2Td19n1wBzy3iboGFODEfyBfCtKuRic447PDLNaszA/132"
 }
 */
