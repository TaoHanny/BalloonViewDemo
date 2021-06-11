package com.instwall.balloonviewdemo.view.custom;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.instwall.balloonviewdemo.R;
import com.instwall.balloonviewdemo.control.SaveTaskManager;
import com.instwall.balloonviewdemo.model.ParamsData;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import ashy.earl.common.app.App;
import ashy.earl.common.util.L;

public class BalloonView extends View {

    public static final long DEFAULTDURATION = 300L;

    private float ySpeed;
    private long snowDuration;
    private static volatile List<Snow> snowList;
    private Matrix mtx = new Matrix();
    private ValueAnimator animator;
    private Random yRandom = new Random();
    private boolean isDelyStop;
    private boolean sendMsgable;

    private static volatile LinkedList<ParamsData> cacheDataLinked = new LinkedList<>();
    private float xWidth;
    private float yHeight;
    private int delayTimeInt = 55000;
    private List<Coordinate> coordinateList;
    private List<ParamsData> showList = new ArrayList<>();

    private Handler otherHandler;
    private HandlerThread handlerThread = new HandlerThread("ballView");

    private final String TAG = "BalloonView";
    public BalloonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BalloonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttr(context, attrs);
        init();
    }

        @SuppressLint("CustomViewStyleable")
    private void initAttr(Context context, AttributeSet attrs) {
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.FlyView);
        ySpeed = attributes.getFloat(R.styleable.FlyView_snow_ySpeed, 100.0f);
        snowDuration = attributes.getInt(R.styleable.FlyView_snow_duration, 0);
        sendMsgable = snowDuration > DEFAULTDURATION;
        attributes.recycle();
    }

    private void init() {
        handlerThread.start();
        otherHandler = new Handler(handlerThread.getLooper());
        setLayerType(View.LAYER_TYPE_NONE, null);
        snowList = new ArrayList<>();
        showList = getShowParamsList();
        coordinateList = getCoordinate();
        animator = ValueAnimator.ofFloat(1.0f, 0.0f);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setDuration(DEFAULTDURATION);
        animator.addUpdateListener(new animatorUpdateListenerImp());
        handler.sendEmptyMessageDelayed(MSG_ADD,10000);
    }

    public void startAnimation(){
        this.isDelyStop = false;
        if (animator.isRunning())
            animator.cancel();
        if (sendMsgable) {
            removeMessages();
            handler.sendEmptyMessageDelayed(MSG_STOP, snowDuration);
        }
        animator.start();
    }

    public void setSnowDuration(long snowDuration){
        this.snowDuration = snowDuration;
        sendMsgable = snowDuration > DEFAULTDURATION;
    }



    @Override
    protected void onDetachedFromWindow() {
        removeMessages();
        if (animator.isRunning())
            animator.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        xWidth = getWidth();
        yHeight = getHeight();
    }
    @ColorInt
    private int tagColor;
    @ColorInt
    private int tagLineColor;

    private int nameTextSize;
    private int contentTextSize;
    private Paint paint = new Paint();
    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        for (int i = 0; i < snowList.size(); i++) {

            Snow snow = snowList.get(i);
            float bpWidthHalf = snow.bpWidth / 2;
            float bpHeightHalf = snow.bpHeight / 2;
            if(snow.delayStatus == snow.DELAY_STATUS_START){
                canvas.rotate(snow.bitmapRotate,snow.pathPoint.x+bpWidthHalf,snow.pathPoint.y+bpHeightHalf);
            }else {
                canvas.rotate(snow.bitmapRotate,snow.pathPoint.x+bpWidthHalf,snow.pathPoint.y);
            }
            mtx.setTranslate(-bpWidthHalf, -bpHeightHalf);
            mtx.postTranslate(bpWidthHalf + snow.pathPoint.x, bpHeightHalf + snow.pathPoint.y);
            canvas.drawBitmap(snow.snowBitmap, mtx, null);

            final int W_CUT_RATIO = 7;
            final int H_CUT_RATIO = 10;
            float width = snow.snowBitmap.getWidth();
            float height = snow.snowBitmap.getHeight();

            if(snow.iconBitmap!=null){
                mtx.setTranslate(snow.iconBitmap.getWidth() / 2,snow.iconBitmap.getHeight() / 2);
                float iconX = snow.pathPoint.x + (width / W_CUT_RATIO) * 4 - 2;
                float iconY = snow.pathPoint.y + (height / 5) * 2 - snow.iconBitmap.getHeight() * 2 - 10;
                mtx.postTranslate(iconX,iconY);
                canvas.drawBitmap(snow.iconBitmap,mtx,null);
            }

            paint.setAntiAlias(true);
            paint.setStrokeWidth(1);
            float cutHeightTop = 0;
            float cutHeightBottom = 0;
            tagLineColor = Color.BLACK;
            if ("A".equals(snow.tplType) || "LOCAL1".equals(snow.tplType)){
                tagColor = getResources().getColor(R.color.tag_1);
                nameTextSize = 22;
                contentTextSize = 26;
                cutHeightTop = 28;
                cutHeightBottom = 28;
            }else if("C".equals(snow.tplType) || "LOCAL3".equals(snow.tplType)){
                tagColor = getResources().getColor(R.color.tag_3);
                nameTextSize = 20;
                contentTextSize = 24;
            }else{
                tagColor = getResources().getColor(R.color.tag_2);
                nameTextSize = 16;
                contentTextSize = 20;
                cutHeightTop = 28;
            }
            float [] posName = new float[snow.name.length() * 2];
            for (int j = 0;j < posName.length ;j+=2){
                posName[j] = snow.pathPoint.x + (width / W_CUT_RATIO) * 4 + (width / W_CUT_RATIO) /2;
                if (j==0){
                    posName[j+1] = snow.pathPoint.y + (height / 5) * 2 ;
                }else {
                    posName[j+1] = posName[j-1] + nameTextSize + 2;
                }
            }

            setPaint(true,tagColor,Paint.Style.STROKE,nameTextSize);
            canvas.drawPosText(snow.name, posName, paint);

            setPaint(false,tagColor,Paint.Style.FILL,nameTextSize);
            canvas.drawPosText(snow.name, posName, paint);


            if(snow.content!=null && snow.content.length()>18){
                snow.content = snow.content.substring(0,18);
            }
            float [] posContent = new float[snow.content.length() * 2];
            float maxHeight = snow.pathPoint.y + (height / H_CUT_RATIO) * 7 + cutHeightBottom;
            for (int j = 0;j < posContent.length ;j+=2){
                float currentHeight;
                if (j==0){
                    posContent[j] = snow.pathPoint.x + (width / W_CUT_RATIO) * 3 + (width / W_CUT_RATIO) / 4;
                    posContent[j+1] = snow.pathPoint.y + (height / H_CUT_RATIO) * 3 + cutHeightTop;
                }else {
                    currentHeight = posContent[j-1] + contentTextSize + 2;
                    if(currentHeight >= maxHeight){
                        posContent[j] = posContent[j-2] - width / W_CUT_RATIO;
                        posContent[j+1] = snow.pathPoint.y + (height / H_CUT_RATIO) * 3 + cutHeightTop;
                    }else {
                        posContent[j] = posContent[j-2] ;
                        posContent[j+1] = posContent[j-1] + contentTextSize + 2;
                    }
                }
            }

            setPaint(true,tagColor,Paint.Style.STROKE,contentTextSize);
            canvas.drawPosText(snow.content, posContent, paint);


            setPaint(false,tagColor,Paint.Style.FILL,contentTextSize);
            canvas.drawPosText(snow.content, posContent, paint);

            if(snow.delayStatus == snow.DELAY_STATUS_START){
                canvas.rotate(-snow.bitmapRotate,snow.pathPoint.x+bpWidthHalf,snow.pathPoint.y+bpHeightHalf);
            }else {
                canvas.rotate(-snow.bitmapRotate,snow.pathPoint.x+bpWidthHalf,snow.pathPoint.y);
            }
        }
    }

    private void setPaint(boolean boldBool , @ColorInt int color, Paint.Style style , int size){
        paint.setFakeBoldText(boldBool);
        paint.setColor(color);
        paint.setStyle(style);
        paint.setTextSize(size);
    }

    private static boolean firstStartViewBool = true;
    public void pushSnows(List<ParamsData> list){
        otherHandler.post(new PushDataTask(list));

    }


    private LinkedList<Snow> snowLinkedList = new LinkedList<>();
    private final int MSG_STOP = 0x01;
    private final int MSG_ADD_FULL = 0x02;
    private final int MSG_UPDATE = 0x03;
    private final int MSG_ADD_DELAY = 0x04;
    private final int MSG_ADD = 0x05;
    private static int localSnowListIndex = 0;
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_ADD_DELAY:
                    Snow snow = (Snow) msg.obj;
                    if(snowLinkedList.size()==0){
                        if(snowList.size() < coordinateList.size()){
                            if(snow!=null){
                                snowList.add(snow);
                                L.d(TAG,"onAnimationUpdate~~ SnowList.add() -> snow = "+snow.toString());
                            }
                        }else {
                            if(snow!=null){
                                snowLinkedList.addFirst(snow);
                                if(snowList.size() >= coordinateList.size()){
                                    for (int i = 0; i < snowList.size(); i++) {
                                        Snow data = snowList.get(i);
                                        if(data!=null){
                                            if("LOCAL1".equals(data.tplType)|| "LOCAL2".equals(data.tplType) ||
                                                    "LOCAL3".equals(data.tplType)) {
                                                data.timeCurrent = data.timeCurrent+snow.playDelay;
                                                snowList.set(i,data);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }else {
                        if(snowList.size() < coordinateList.size()){
                            Snow snow1 = snowLinkedList.removeLast();
                            snowList.add(snow1);
                            L.d(TAG,"onAnimationUpdate~~ SnowList.add() -> snow = "+snow1.toString());
                        }else {
                            if(snow!=null){
                                snowLinkedList.addFirst(snow);
                                if(snowList.size() >= coordinateList.size()){
                                    for (int i = 0; i < snowList.size(); i++) {
                                        Snow data = snowList.get(i);
                                        if(data!=null){
                                            if("LOCAL1".equals(data.tplType)|| "LOCAL2".equals(data.tplType) ||
                                                    "LOCAL3".equals(data.tplType)) {
                                                data.timeCurrent = data.timeCurrent+snow.playDelay;
                                                snowList.set(i,data);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;
                case MSG_ADD:
                    Log.d(TAG, "handleMessage() SnowList.add() cacheDataLinked.size = "+cacheDataLinked.size()+" ," +
                            " snowList.size() = "+snowList.size());
                    if(cacheDataLinked.size()==0 && snowList.size()<=2){
                        int timeTmp = 0;
                        if (snowList.size() == 0){
                            for (ParamsData data : showList){
                                ProduceSnowTask task = new ProduceSnowTask(data,timeTmp);
                                otherHandler.post(task);
                                timeTmp += 8000;
                            }
                        }else {
                            if(localSnowListIndex > 2){
                                localSnowListIndex = 0;
                            }
                            ParamsData data = showList.get(localSnowListIndex);
                            ProduceSnowTask task = new ProduceSnowTask(data,timeTmp);
                            otherHandler.post(task);
                            localSnowListIndex++;
                        }

                    }else if(cacheDataLinked.size() > 0){
                        ProduceSnowTask task = new ProduceSnowTask(true);
                        otherHandler.post(task);
                    }else {
                        handler.sendEmptyMessage(MSG_ADD_DELAY);
                    }
                    break;
                case MSG_STOP:
                    isDelyStop = true;
                    break;
                case MSG_UPDATE:
                    updateSnowView();
                    break;
            }
        }
    };

    private List<ParamsData> getShowParamsList(){
        String json =
                "[{\"status\":\"waiting\",\"words\":\"祝：大家身体健康，万事如意快乐！\",\"sid\":\"test001\",\"playtime\":15,\"tpltype\":\"LOCAL1\"," +
                        "\"synctime\":\"1618566735\",\"uname\":\"全体工作人员\",\"uicon\":\"http://139.155.180.131/icon.png\"}," +
                        "{\"status\":\"waiting\",\"words\":\"祝：大家心情愉快！\",\"sid\":\"test002\",\"playtime\":15,\"tpltype\":\"LOCAL2\"," +
                        "\"synctime\":\"1618567735\",\"uname\":\"全体工作人员\",\"uicon\":\"http://139.155.180.131/icon.png\"}," +
                        "{\"status\":\"waiting\",\"words\":\"祝：大家节日快乐！\",\"sid\":\"test003\",\"playtime\":15,\"tpltype\":\"LOCAL3\"," +
                        "\"synctime\":\"1618568735\",\"uname\":\"全体工作人员\",\"uicon\":\"http://139.155.180.131/icon.png\"}]";
        try {
            JSONArray jsonArray = new JSONArray(json);
            Gson gson = new Gson();
            List<ParamsData> dataList = gson.fromJson(jsonArray.toString(), new TypeToken<List<ParamsData>>(){}.getType());
            return dataList;
        } catch (JSONException e) {
            L.e(TAG,e.toString());
        }
        return new ArrayList<>();
    }

    private class PushDataTask implements Runnable{

        private List<ParamsData> list;
        public PushDataTask(List<ParamsData> list){
            this.list = list;
        }
        @Override
        public void run() {
            Collections.sort(list);
            for (ParamsData data : list){
                boolean isSnowExist = false;
                for (Snow snow : snowList){
                    if(data.getSid().equals(snow.sid)){
                        isSnowExist = true;
                        break;
                    }
                }
                boolean isParamsExist = false;
                for (ParamsData paramsData : cacheDataLinked){
                    if(data.getSid().equals(paramsData.getSid())){
                        isParamsExist =true;
                        break;
                    }
                }
                if (!isSnowExist && !isParamsExist){
                    cacheDataLinked.addFirst(data);
                    if(snowList.size() < coordinateList.size()){
                        handler.sendEmptyMessage(MSG_ADD);
                    }
                }
            }
            Log.d(TAG, "PushDataTask() snowList.size = "+snowList.size());
            firstStartViewBool = false;

        }
    }


    private class ProduceSnowTask implements Runnable{
        private ParamsData data;
        private int timeDelay = 0;
        private boolean trueDataBool = false;
        public ProduceSnowTask(boolean trueDataBool){
            this.trueDataBool= trueDataBool;
        }

        public ProduceSnowTask(ParamsData data, int timeDelay) {
            this.data = data;
            this.timeDelay = timeDelay;
        }

        @Override
        public void run() {
            if(trueDataBool){
                if(cacheDataLinked.size()<=0) return;
                data = cacheDataLinked.removeLast();
            }
            Snow snow = getSnow(data);
            Message message = new Message();
            message.what = MSG_ADD_DELAY;
            message.obj = snow;
            if(timeDelay==0){
                handler.sendMessage(message);
            }else {
                handler.sendMessageDelayed(message,timeDelay);
            }
        }
    }

    private final int[] imgArray = {R.drawable.heart1,R.drawable.heart2,R.drawable.heart3};

    @SuppressLint("CheckResult")
    @NotNull
    private Snow getSnow(ParamsData data) {
        BitmapDrawable bitmapDrawable ;
        String type = data.getTpltype();
        if ("A".equals(type) || "LOCAL1".equals(type)){
            bitmapDrawable = (BitmapDrawable) getResources().getDrawable(imgArray[0]);
        }else if("B".equals(type) || "LOCAL2".equals(type)){
            bitmapDrawable = (BitmapDrawable) getResources().getDrawable(imgArray[1]);
        }else {
            bitmapDrawable = (BitmapDrawable) getResources().getDrawable(imgArray[2]);
        }
        Bitmap bitmap = bitmapDrawable.getBitmap();
        Snow snow2 = new Snow(ySpeed, bitmap);
        snow2.sid = data.getSid();
        snow2.content = data.getWords();
        snow2.name = data.getUname();
        snow2.tplType = data.getTpltype();
        snow2.playDelay = data.getPlaytime() * 1000;
        Glide.with(App.getAppContext()).asBitmap().load(data.getUicon()).into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                Bitmap circleBitmap = Bitmap.createScaledBitmap(resource,
                        25, 25, true);
                snow2.iconBitmap = createCircleBitmap(circleBitmap);
            }
        });
        return snow2;
    }

    private Bitmap createCircleBitmap(Bitmap resource)
    {
        int width = resource.getWidth();
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Bitmap circleBitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(circleBitmap);
        canvas.drawCircle(width/2, width/2, width/2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(resource, 0, 0, paint);
        return circleBitmap;
    }


    private void removeMessages(){
        if (handler.hasMessages(MSG_STOP))
            handler.removeMessages(MSG_STOP);
    }


    private class animatorUpdateListenerImp implements ValueAnimator.AnimatorUpdateListener {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            handler.sendEmptyMessage(MSG_UPDATE);
        }
    }


    private void updateSnowView(){
        int[] indexArr = new int[coordinateList.size()];
        for (int i = 0; i < coordinateList.size() && i < snowList.size() ;i++){
            Snow snow = snowList.get(i);
            indexArr[snow.index] += 1;
        }
        boolean firstUpdate = true;
        for (int i = 0; i < coordinateList.size() && i < snowList.size() ;i++){
            Snow snow = snowList.get(i);
            int oldIndex = snow.index;
            if(indexArr[oldIndex] > 1){
                if(firstUpdate){
                    firstUpdate = false;
                    continue;
                }
                Log.d(TAG, "updateSnowView() 重复 =  "+snow.toString());
                for (int j = 0; j < coordinateList.size() && j < snowList.size() ; j++) {
                    if(indexArr[j] == 0){
                        snow.endPoint = getEndPointF(j);
                        snow.index = j;
                        snowList.set(i,snow);
                        Log.d(TAG, "updateSnowView() 重复  修改后 = "+snowList.get(i).toString());
                        indexArr[j] += 1;
                        break;
                    }
                }
                indexArr[oldIndex] -= 1;
            }
        }

        for (int i = 0; i < coordinateList.size() && i < snowList.size() ;i++){
            Snow snow = snowList.get(i);
            if(snow.pathPoint.y > 0 && snow.pathPoint.y < snow.endPoint.y){
                if(snow.timeCurrent == 0){
                    snow.timeCurrent = System.currentTimeMillis();
                    snow.timeEnd = snow.timeCurrent + snow.playDelay;
                }
                if(snow.timeCurrent < snow.timeEnd){
                    snow.delayStatus = snow.DELAY_STATUS_STOP;
                    snow.timeCurrent = System.currentTimeMillis();
                }
                else snow.delayStatus = snow.DELAY_STATUS_END;
            }
            if(snow.delayStatus == snow.DELAY_STATUS_START) {
                snow.count += 0.001;
                snow.pathPoint = calculateBezierPointForQuadratic(snow.count, snow.startPoint, snow.controlPoint, snow.endPoint);
            }else if (snow.delayStatus == snow.DELAY_STATUS_STOP){
//                Log.d(TAG, "onAnimationUpdate() snow = "+snow.toString());
            }else if(snow.delayStatus == snow.DELAY_STATUS_END){
                snow.pathPoint.y -= snow.ySpeed;
            }
            if (snow.bitmapRotate > 10){
                snow.forwardBool = false;
            }else if(snow.bitmapRotate < -10){
                snow.forwardBool = true;
            }
            if (snow.forwardBool){
                snow.bitmapRotate += 0.08;
            }else {
                snow.bitmapRotate -= 0.08;
            }

            if (snow.pathPoint.y < 0) {
                if((!"LOCAL1".equals(snow.tplType) && !"LOCAL2".equals(snow.tplType) &&
                    !"LOCAL3".equals(snow.tplType)) && snow.sid !=null){
                    Log.d(TAG, "updateSnowView: snow.tplType = "+snow.tplType);
                    SaveTaskManager.getInstance().saveSnowTask(snow);
                }
                L.d(TAG, "onAnimationUpdate~~ SnowList.remove() -> snow = "+snow.toString());
                snowList.remove(i);
                if(snowList.size() < coordinateList.size()){
                    handler.sendEmptyMessage(MSG_ADD);
                }
            }
        }
        invalidate();
    }

    public static PointF calculateBezierPointForQuadratic(float t, PointF p0, PointF p1, PointF p2) {
        PointF point = new PointF();
        float temp = 1 - t;
        point.x = temp * temp * p0.x + 2 * t * temp * p1.x + t * t * p2.x;
        point.y = temp * temp * p0.y + 2 * t * temp * p1.y + t * t * p2.y;
        return point;
    }

    public class Snow {

        final int DELAY_STATUS_START = 100;
        final int DELAY_STATUS_STOP = 200;
        final int DELAY_STATUS_END = 300;

        public String sid;
        float count = 0;
        private String name;
        private String content;
        private int playDelay = 0;
        private int index = 0;
        private final PointF startPoint = new PointF();
        private PointF pathPoint ;
        private PointF endPoint ;
        private PointF controlPoint ;
        private final float ySpeed;
        private final int bpHeight;
        private final int bpWidth;
        private final Bitmap snowBitmap;
        private Bitmap iconBitmap ;
        private long timeCurrent = 0;
        private long timeEnd = 0;
        private float bitmapRotate = 0;
        private boolean forwardBool = true;
        private int delayStatus = DELAY_STATUS_START;
        private final float BASESPEED = 100.0f;
        private String tplType;

        Snow(float ySpeed, Bitmap snowBitmap) {
            this.bpHeight = snowBitmap.getHeight() / 8;
            this.bpWidth = snowBitmap.getWidth() / 8;
            this.startPoint.x = randomX();
            this.startPoint.y = randomY(bpHeight);
            this.pathPoint = this.startPoint;
            this.endPoint = getEndPointF();
            this.index = currentIndex;
            this.controlPoint = getControlPointF(startPoint,endPoint);
            this.ySpeed = (ySpeed + ySpeed * (float) Math.random()) / BASESPEED;
            this.snowBitmap = Bitmap.createScaledBitmap(snowBitmap, bpWidth, bpHeight, true);
        }

        @Override
        public String toString() {
            return "Snow{" +
                    "sid='" + sid + '\t' +
                    ", name='" + name + '\t' +
                    ", content='" + content + '\t' +
                    ", playDelay=" + playDelay +'\t'+
                    ", index=" + index +
                    '}';
        }
    }


    private static volatile int currentIndex = 0;
    private PointF getEndPointF(){
        currentIndex++;
        if(currentIndex >= coordinateList.size()){
            currentIndex = 0;
        }
        PointF endPoint = new PointF();
        endPoint.x = coordinateList.get(currentIndex).x;
        endPoint.y = coordinateList.get(currentIndex).y + 30;
        return endPoint;
    }

    private PointF getEndPointF(int index){
       if(index > coordinateList.size()) {
           index = 0;
       }
        PointF endPoint = new PointF();
        endPoint.x = coordinateList.get(index).x;
        endPoint.y = coordinateList.get(index).y + 30;
        return endPoint;
    }

    private PointF getControlPointF(PointF startPoint , PointF endPoint){
        PointF controlPoint = new PointF();
        if (startPoint.y > endPoint.y){
            controlPoint.y = endPoint.y + (startPoint.y - endPoint.y)/2;
        }else {
            controlPoint.y =startPoint.y + (endPoint.y - startPoint.y)/2;
        }
        controlPoint.x = startPoint.x;
        controlPoint.x = xWidth / 2;
        controlPoint.y = yHeight /2;
        return controlPoint;
    }

    private final int[] widthArr = {5,6,7,8};
    private int widthIndex = 0;
    private float randomX() {
        float width = (xWidth / 12 )* widthArr[widthIndex];
        Log.d(TAG, "randomX() -> xWidth = "+width);
        widthIndex++;
        if (widthIndex>=widthArr.length) widthIndex = 0;
        return width;
    }

    private float randomY(int bpHeight) {
        float nextFloat = yRandom.nextFloat() * bpHeight;
        Log.d(TAG, "randomY() yHeight = "+(yHeight + nextFloat));
        return yHeight + nextFloat;
    }


    private List<Coordinate> getCoordinate(){
        List<Coordinate> dataList = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(JSON);
            String jsonArr = jsonObject.getString("coordinate");
            Gson gson = new Gson();
            dataList = gson.fromJson(jsonArr, new TypeToken<List<Coordinate>>(){}.getType());
            L.d(TAG, "getCoordinate() size = "+dataList.size());
        } catch (JSONException e) {
            L.e(TAG,e.toString());
        }
        return dataList;
    }

    class Coordinate {
        int x;
        int y;
    }

    private final String JSON = "{\"coordinate\":[" +
            "{\"x\":210,\"y\":380},{\"x\":850,\"y\":430},{\"x\":1280,\"y\":440},{\"x\":1650,\"y\":450}," +
            "{\"x\":950,\"y\":60},{\"x\":1180,\"y\":140},{\"x\":730,\"y\":130}," +
            "{\"x\":410,\"y\":290}," +
            "{\"x\":590,\"y\":400},{\"x\":1030,\"y\":370},{\"x\":1470,\"y\":330}]}";


}
