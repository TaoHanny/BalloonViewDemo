package com.instwall.balloonviewdemo.control.utils;

import android.util.Log;

import com.instwall.net.ApiBase;
import com.instwall.net.ErrorHandler;
import com.instwall.net.NetCore;
import com.instwall.net.NetCoreException;
import com.instwall.net.ResultParser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class GetHttpApi extends ApiBase<String> {


    private final String TAG = "GetHttpApi";
    public GetHttpApi(){
        super("get_showdatas_list");
    }

    @Nullable
    @Override
    protected String requestApi(@NotNull NetCore netCore) throws Throwable {
        OkHttpClient okHttpClient = netCore.get().okHttp(NetCore.TIMEOUT);
        Call call = okHttpClient.newCall(getRequest());
        Response response =  call.execute();
        ResponseBody body = response.body();
        String msg = body == null ? "" : body.string();
        // Check response
        if (response.code() != 200) {
            throw new NetCoreException(NetCoreException.TYPE_SERVER, response.code(), msg, null, msg);
        }
//        Log.d(TAG, "requestApi() -> msg = " + msg);
        return msg;
    }


    private final String URL = "http://grayds.instwall.com/openapi/json";
    private Request getRequest(){
        JSONObject json  = getBody();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON,json.toString());
        Request request = new Request.Builder().url(URL).post(body).build();
        return request;
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
                "        \"client_id\":\"CO0syCzWJzzDUY9pVPCb1rhC\",\n" +
                "        \"client_secret\":\"4eZ1Z5CbJvShWNAm.7L7rBdx5naRNP\"\n" +
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