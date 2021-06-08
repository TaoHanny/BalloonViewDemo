package com.instwall.balloonviewdemo.control.utils;

import com.instwall.net.ApiBase;
import com.instwall.net.ErrorHandler;
import com.instwall.net.NetCore;
import com.instwall.net.NetCoreException;
import com.instwall.net.ResultParser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ashy.earl.common.util.L;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ReportHttpApi extends ApiBase<String> {

    private final String TAG = "ReportHttpApi";
    private String array;
    public ReportHttpApi(String array){
        super("BalloonView");
        this.array = array;
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

//        L.d("ReportHttpApi", "onResponse : " + msg);
        return msg;
    }



    private final String URL = "http://grayds.instwall.com/openapi/json";
    private Request getRequest(){
        Request request = null;
        try {
            JSONObject json = new JSONObject().put("datafrom", "scenicSpots");
            JSONArray jsonArray ;
            if(array==null || "".equals(array)){
                jsonArray = new JSONArray();
            }else {
                jsonArray = new JSONArray(array);
            }
            json.put("rpt_data",jsonArray);
            JSONObject bodyJson = getBody();
            JSONArray paramsArray = new JSONArray();
            paramsArray.put(json);
            bodyJson.put("params",paramsArray);
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(JSON, bodyJson.toString());
            request = new Request.Builder().url(URL).post(body).build();
        } catch (JSONException e) {
            e.printStackTrace();
        }


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
                "    \"id\":"+timeTmp+",\n" +
                "    \"method\":\"rpt_showdata_status\"\n" +
                "}";
    }
}