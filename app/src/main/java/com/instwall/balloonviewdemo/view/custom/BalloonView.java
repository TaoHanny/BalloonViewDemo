package com.instwall.balloonviewdemo.view.custom;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.instwall.balloonviewdemo.R;
import com.instwall.balloonviewdemo.control.SaveTaskManager;
import com.instwall.balloonviewdemo.model.ParamsData;

import org.jetbrains.annotations.NotNull;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BalloonView extends View {
    public static final long DEFAULTDURATION = 300L;

    private int initToTop;

    private int initToLeft;

    private int initToBottom;

    private int initToRight;
    private float minScale;
    private float maxScale;
    private float xSpeed;
    private float ySpeed;
    private int snowCount;
    private long snowDuration;
    private List<Snow> snowList;
    private BitmapDrawable snowBitmap;
    private Matrix mtx = new Matrix();
    private ValueAnimator animator;
    private Random xRandom = new Random();
    private Random yRandom = new Random();
    private boolean isDelyStop;
    private boolean sendMsgable;

    private float xWidth;

    private float yHeight;

    private Context context;

    private final String TAG = "BalloonView";
    public BalloonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BalloonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        initAttr(context, attrs);
        init();
    }

    private void initAttr(Context context, AttributeSet attrs) {
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.FlyView);
        int initTo = attributes.getDimensionPixelSize(R.styleable.FlyView_snow_initTo, 0);
        initToTop = attributes.getDimensionPixelSize(R.styleable.FlyView_snow_initToTop, 0);
        initToLeft = attributes.getDimensionPixelSize(R.styleable.FlyView_snow_initToLeft, 0);
        initToBottom = attributes.getDimensionPixelSize(R.styleable.FlyView_snow_initToBottom, 0);
        initToRight = attributes.getDimensionPixelSize(R.styleable.FlyView_snow_initToRight, 0);
        minScale = attributes.getFloat(R.styleable.FlyView_snow_minScale, 1.0f);
        maxScale = attributes.getFloat(R.styleable.FlyView_snow_maxScale, 1.0f);
        xSpeed = attributes.getFloat(R.styleable.FlyView_snow_xSpeed, 0.0f);
        ySpeed = attributes.getFloat(R.styleable.FlyView_snow_ySpeed, 100.0f);
        snowCount = attributes.getInt(R.styleable.FlyView_snow_count, 20);
        snowDuration = attributes.getInt(R.styleable.FlyView_snow_duration, 0);
        snowBitmap = (BitmapDrawable) attributes.getDrawable(R.styleable.FlyView_snow_bitmap);

        if (0 != initTo)
            initToTop = initToLeft = initToBottom = initToRight = initTo;
        if (minScale <= 0.0f || minScale > maxScale)
            throw new IllegalArgumentException("The minScale is illegal");
        sendMsgable = snowDuration > DEFAULTDURATION;
        attributes.recycle();
    }

    private void init() {
        /**
         * close software/hardware
         */
        setLayerType(View.LAYER_TYPE_NONE, null);
        snowList = new ArrayList<>(snowCount);
        animator = ValueAnimator.ofFloat(1.0f, 0.0f);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setDuration(DEFAULTDURATION);
        animator.addUpdateListener(new animatorUpdateListenerImp());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG, "onSizeChanged: getHeight = "+getHeight());
        xWidth = getWidth() - initToLeft - initToRight;
        yHeight = getHeight() - initToTop - initToBottom;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < snowList.size(); i++) {
            Snow snow = snowList.get(i);
            mtx.setTranslate(-snow.bpWidth / 2, -snow.bpHeight / 2);
            mtx.postTranslate(snow.bpWidth / 2 + snow.pathPoint.x, snow.bpHeight / 2 + snow.pathPoint.y);
            canvas.drawBitmap(snow.snowBitmap, mtx, null);
            canvas.save();
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);

            int width = snow.snowBitmap.getWidth();
            int height = snow.snowBitmap.getHeight();
            paint.setColor(Color.WHITE);
            paint.setTextSize(18);
            float [] posName = new float[snow.name.length() * 2];
            for (int j = 0;j < posName.length ;j+=2){
                posName[j] = snow.pathPoint.x + (width/4) * 3 + (width / 4)/4;
                if (j==0){
                    posName[j+1] = snow.pathPoint.y + height / 4;
                }else {
                    posName[j+1] = posName[j-1] + 20;
                }
            }
            canvas.drawPosText(snow.name, posName, paint);
            canvas.save();

            paint.setColor(Color.BLUE);
            paint.setTextSize(18);
            if(snow.content!=null && snow.content.length()>25){
                snow.content = snow.content.substring(0,24);
            }
            float [] posContent = new float[snow.content.length() * 2];
            for (int j = 0;j < posContent.length ;j+=2){
                float currentWidth = 0;
                if (j==0){
                    posContent[j] = snow.pathPoint.x + (width/4) * 2 + (width / 4)/4;
                    posContent[j+1] = snow.pathPoint.y + height / 6;
                }else {
                    currentWidth = posContent[j-1] + 20;
                    float maxHeight = snow.pathPoint.y + (height / 6) * 6;
                    if(currentWidth >= maxHeight){
                        posContent[j] = posContent[j-2] - width / 6;
                        posContent[j+1] = snow.pathPoint.y + height / 6;
                    }else {
                        posContent[j] = posContent[j-2] ;
                        posContent[j+1] = posContent[j-1] + 20;
                    }
                }
            }
            canvas.drawPosText(snow.content, posContent, paint);
        }
    }

    private int[] imageArr = {R.drawable.snowflake,R.drawable.heart1,R.drawable.heart2,R.drawable.heart5};


    public void startAnimation() {
        this.isDelyStop = false;
        if (animator.isRunning())
            animator.cancel();
        if (sendMsgable) {
            removeMessages();
            handler.sendEmptyMessageDelayed(MSG_STOP, snowDuration);
        }
        animator.start();
    }

    public void setSnowDuration(long snowDuration) {
        this.snowDuration = snowDuration;
        sendMsgable = snowDuration > DEFAULTDURATION;
    }

    /**
     * back the state of animation
     */
    public boolean isRunning() {
        return animator.isRunning();
    }

    public void pushSnows(List<ParamsData> list){
        Message message= new Message();
        message.what = MSG_ADD_FULL;
        message.obj = list;
        handler.sendMessage(message);
    }

    @Override
    protected void onDetachedFromWindow() {
        removeMessages();
        if (animator.isRunning())
            animator.cancel();
        super.onDetachedFromWindow();
    }

    private final int MSG_STOP = 0x01;
    private final int MSG_ADD_FULL = 0x02;
    private final int MSG_UPDATE = 0x03;
    private final int MSG_ADD = 0x04;
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_ADD_FULL:
                    List<ParamsData> dataList = (List<ParamsData>) msg.obj;
                    for (ParamsData data : dataList){
                        boolean isExist = false;
                        for (Snow snow : snowList){
                            if(data.getSid().equals(snow.sid)){
                                isExist = true;
                                break;
                            }
                        }
                        if (!isExist){
                            Snow snow2 = getSnow();
                            snow2.sid = data.getSid();
                            snow2.content = data.getShowWords();
                            snow2.name = data.getShowWords();
                            snow2.tplType = data.getTpltype();
                            Message message = new Message();
                            message.what = MSG_ADD;
                            message.obj = snow2;
                            handler.sendMessageDelayed(message,500);
                        }
                    }
                    break;
                case MSG_ADD:
                    Snow snow = (Snow) msg.obj;
                    snowList.add(snow);
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

    @NotNull
    private Snow getSnow() {
        Random mRandom = new Random();
        int index = mRandom.nextInt(imageArr.length);
        BitmapDrawable bitmapDrawable = (BitmapDrawable) getResources().getDrawable(R.drawable.snow);
        Bitmap bitmap = bitmapDrawable.getBitmap();
        return new Snow(xSpeed, ySpeed, bitmap);
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
        for (int i = 0; i< snowList.size();i++){
            Snow snow = snowList.get(i);
            if(snow.pathPoint.y > 0 && snow.pathPoint.y < snow.endPoint.y){
                if(snow.timeCurrent == 0){
                    snow.timeCurrent = System.currentTimeMillis();
                    snow.timeEnd = snow.timeCurrent + 15000;
                }
                if(snow.timeCurrent < snow.timeEnd){
                    snow.delayBool = true;
                    snow.timeCurrent = System.currentTimeMillis();
                }
                else snow.delayBool = false;
            }
            if(!snow.delayBool){
                snow.count+=0.001;
                snow.pathPoint =  calculateBezierPointForQuadratic(snow.count,snow.startPoint,snow.controlPoint,snow.endPoint);
            }
            if (snow.pathPoint.y < 0) {
                if(snow.tplType!="00" && snow.sid !=null){
                    Log.d(TAG, "onAnimationUpdate() snow = "+snow.toString());
                    SaveTaskManager.getInstance().saveSnowTask(snow);
                }
                snowList.remove(i);
            }
        }
        if (snowList.size() <= 0 && animator.isRunning()){
            Snow snow = getSnow();
            snow.tplType = "00";
            snow.content = "祝我的家人幸福安康，永远幸福！";
            snow.name = "用户的名字";
            snowList.add(snow);
        }

        invalidate();
    }

//    private void updateSnowView(){
//        for (int i = 0; i < snowList.size(); i++) {
//            Snow snow = snowList.get(i);
//
//            if(snow.y > 0 && snow.y < snow.endY){
//                snow.x += (snow.xSpeed/2);
//                if(snow.timeCurrent == 0){
//                    snow.timeCurrent = System.currentTimeMillis();
//                    snow.timeEnd = snow.timeCurrent + 15000;
//                }
//                if(snow.timeCurrent < snow.timeEnd){
//                    snow.delayBool = true;
//                    snow.timeCurrent = System.currentTimeMillis();
//                }
//                else snow.delayBool = false;
//            }
//            if(!snow.delayBool){
//                snow.y -= snow.ySpeed;
//                snow.x += snow.xSpeed;
//            }
//            snow.snowBitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.snow);
////            Log.d(TAG, "onAnimationUpdate() snow = "+snow.toString());
//            if (snow.x < -snow.bpWidth || snow.x > getWidth()){
//
//                if (isDelyStop)
//                    snowList.remove(i);
//                else {
//                    snow.x = randomX();
//                    snow.y = randomY(snow.bpHeight);
//                }
//            }
//            else if (snow.y < 0) {
//                if (isDelyStop)
//                    snowList.remove(i);
//                else {
//                    if(snow.tplType!="00" && snow.sid !=null){
//                        Log.d(TAG, "onAnimationUpdate() snow = "+snow.toString());
//                        SaveTaskManager.getInstance().saveSnowTask(snow);
//                    }
//                    snowList.remove(i);
//                }
//            }
//        }
//        /**
//         * to prevent the animator running empty
//         */
//        if (snowList.size() <= 0 && animator.isRunning()){
//            Snow snow = getSnow();
//            snow.tplType = "00";
//            snow.content = "祝我的家人幸福安康，永远幸福！";
//            snow.name = "用户的名字";
//            snowList.add(snow);
//        }
//
//        invalidate();
//    }



    public static PointF calculateBezierPointForQuadratic(float t, PointF p0, PointF p1, PointF p2) {
        PointF point = new PointF();
        float temp = 1 - t;
        point.x = temp * temp * p0.x + 2 * t * temp * p1.x + t * t * p2.x;
        point.y = temp * temp * p0.y + 2 * t * temp * p1.y + t * t * p2.y;
        return point;
    }

    public class Snow {



        public String sid;
        float count = 0;
        private String name;
        private String content;
        private PointF startPoint = new PointF();
        private PointF pathPoint = new PointF();
        private PointF endPoint = new PointF();
        private PointF controlPoint = new PointF();
        private float ySpeed;
        private int bpHeight;
        private int bpWidth;
        private Bitmap snowBitmap;
        private long timeCurrent = 0;
        private long timeEnd = 0;
        private boolean delayBool = false;
        private float BASESPEED = 100.0f;
        private String tplType;

        Snow(float xSpeed, float ySpeed, Bitmap snowBitmap) {
            float tempScale = minScale + (float) (Math.random() * (maxScale - minScale));
            this.bpHeight = (int) (snowBitmap.getHeight() * tempScale);
            this.bpWidth = (int) (snowBitmap.getWidth() * tempScale);
            this.startPoint.x = xWidth / 2;
            this.startPoint.y = randomY(bpHeight);
            this.pathPoint = this.startPoint;
            this.endPoint.x = randomEndX();
            this.endPoint.y = randomEndY();

            this.controlPoint = getControlPointF(startPoint,endPoint);
            float xDirection = 1.0f - (float) (Math.random() * 2.0f);
//            this.xSpeed = getXSpeed(x, endX ,y, endY,ySpeed);
//            this,xSpeed = 3L;
//            this.xSpeed = xSpeed * xDirection / BASESPEED;
            this.ySpeed = (ySpeed + ySpeed * (float) Math.random()) / BASESPEED;
            this.snowBitmap = Bitmap.createScaledBitmap(snowBitmap, bpWidth, bpHeight, true);
        }

    }


    private PointF getControlPointF(PointF startPoint , PointF endPoint){
        PointF controlPoint = new PointF();
        if(startPoint.x > endPoint.x){
            controlPoint.x = startPoint.x - endPoint.y;
        }else{
            controlPoint.x = endPoint.x - startPoint.y;
        }
        if (startPoint.y > endPoint.y){
            controlPoint.y = startPoint.y - endPoint.y;
        }else {
            controlPoint.y =endPoint.y - startPoint.y;
        }
        return controlPoint;
    }



    private float getXSpeed( float startX,float endX, float starty , float endy , float ySpeed){
        float y = starty - endy;
        float count = (y * 1000) / DEFAULTDURATION;
        float x = endX - startX;
        float speed = x / (count/4);
        Log.e(TAG, "getXSpeed: speed = "+speed);
        return speed;
    }

    private float randomEndX(){
        int width = (int )xWidth/3;
        float endX = xRandom.nextInt(width)+width;
        Log.e(TAG, "randomEndX: endX = "+endX);
        return endX;
    }

    private float randomEndY(){
        int height = (int) yHeight / 3;
        float endY = xRandom.nextInt(height) + height;
        return endY;
    }

    /**
     * the x coordinate
     */
    private int[] widthArr = {5,6,7,8};
    private int widthIndex = 0;
    private float randomX() {
        float width = (xWidth / 12 )* widthArr[widthIndex];
        Log.d(TAG, "randomX: width = "+width);
        widthIndex++;
        if (widthIndex>=widthArr.length) widthIndex = 0;
        return width;
    }

    /**
     * the y coordinate
     */
    private float randomY(int bpHeight) {
        float nextFloat = yRandom.nextFloat() * bpHeight;
        Log.d(TAG, "randomY: yHeight = "+yHeight + " , nextFloat = "+nextFloat);
        return yHeight - (initToBottom + nextFloat);
    }


}
