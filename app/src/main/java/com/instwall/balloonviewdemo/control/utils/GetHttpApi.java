package com.instwall.balloonviewdemo.control.utils;

import com.instwall.net.ApiBase;
import com.instwall.net.ErrorHandler;
import com.instwall.net.NetCore;
import com.instwall.net.ResultParser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

public class GetHttpApi extends ApiBase<String> {



    public GetHttpApi(){
        super("get_showdatas_list");
    }

    @Nullable
    @Override
    protected String requestApi(@NotNull NetCore netCore) throws Throwable {
        JSONObject json  = getBody();
        String result = netCore.requestApi("GC", "/openapi/json",
                "get_showdatas_list", json.toString(),
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
                "    \"params\":[\n" +
                "        {\n" +
                "            \"datafrom\":\"scenicSpots\",\n" +
                "            \"acttype\":\"showWords\",\n" +
                "            \"forstatus\":\"waiting\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"id\":"+timeTmp+",\n" +
                "    \"method\":\"get_showdatas_list\"\n" +
                "}";
    }
}