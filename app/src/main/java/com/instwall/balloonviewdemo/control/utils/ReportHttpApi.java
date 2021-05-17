package com.instwall.balloonviewdemo.control.utils;

import com.instwall.net.ApiBase;
import com.instwall.net.ErrorHandler;
import com.instwall.net.NetCore;
import com.instwall.net.ResultParser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ReportHttpApi extends ApiBase<String> {

    private String array;
    public ReportHttpApi(String array){
        super("BalloonView");
        this.array = array;
    }

    @Nullable
    @Override
    protected String requestApi(@NotNull NetCore netCore) throws Throwable {
        JSONObject json = new JSONObject().put("datafrom", "scenicSpots");
        JSONArray jsonArray = new JSONArray(array);
        json.put("rpt_data",jsonArray);
        JSONObject bodyJson = getBody();
        JSONArray paramsArray = new JSONArray();
        paramsArray.put(json);
        bodyJson.put("params",paramsArray);
        String result = netCore.requestApi("GC", "/openapi/json",
                "rpt_showdata_status", bodyJson.toString(),
                new ResultParser<String>() {
                    @Override
                    public String parse(String string) throws Throwable {

                        return string;
                    }
                }, ErrorHandler.DEFALT_NODE_ERROR);
        return result;
    }

    private JSONObject getBody(){
        try {

            JSONObject bodyObject = new JSONObject(getJSON());
            return bodyObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getJSON(){
        long timeTmp = System.currentTimeMillis() / 1000;
        return "{\n" +
                "    \"oauth2\":{\n" +
                "        \"client_id\":\"TZifubgLzkrZnF7p8oJOuGqQ\",\n" +
                "        \"client_secret\":\"dRpXyEl4MlUsagt-pcyU0xIadtfbzf\"\n" +
                "    },\n" +
                "    \"id\":"+timeTmp+",\n" +
                "    \"method\":\"rpt_showdata_status\"\n" +
                "}";
    }
}