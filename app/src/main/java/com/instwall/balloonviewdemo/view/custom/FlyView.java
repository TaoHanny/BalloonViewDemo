package com.instwall.balloonviewdemo.view.custom;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.instwall.balloonviewdemo.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FlyView extends View {
    private final int msgWhat = 0x01;
    public static final long DEFAULTDURATION = 300L;
    /**
     * the distance of snow start falling to the view top
     */
    private int initToTop;
    /**
     * the distance of snow start falling to the view left
     */
    private int initToLeft;
    /**
     * the distance of snow start falling to the view bottom
     */
    private int initToBottom;
    /**
     * the distance of snow start falling to the view right
     */
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
    /**
     * the range of snow start falling in the x direction
     */
    private float xWidth;
    /**
     * the range of snow start falling in the y direction
     */
    private float yHeight;

    private final String TAG = "FlyView";
    public FlyView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlyView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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
            mtx.postTranslate(snow.bpWidth / 2 + snow.x, snow.bpHeight / 2 + snow.y);

            canvas.drawBitmap(snow.snowBitmap, mtx, null);
        }
    }

    private int[] imageArr = {R.drawable.snowflake,R.drawable.heart1,R.drawable.heart2,R.drawable.heart5};

    /**
     * init snowList
     */
    private void initSnows() {
        if (null == snowBitmap) return;
        snowList.clear();
        Random mRandom = new Random();
        for (int i = 0; i < snowCount; i++) {
            int index = mRandom.nextInt(imageArr.length);
            BitmapDrawable bitmapDrawable = (BitmapDrawable) getResources().getDrawable(imageArr[index]);
            Bitmap bitmap = bitmapDrawable.getBitmap();
            Snow snow = new Snow(xSpeed, ySpeed, bitmap);
            snowList.add(snow);
        }
    }

    /**
     * stop animation dely
     */
    public void stopAnimationDely() {
        removeMessages();
        this.isDelyStop = true;
    }

    /**
     * stop animation and clear snowList
     */
    public void stopAnimationNow() {
        removeMessages();
        snowList.clear();
        invalidate();
        animator.cancel();
    }

    /**
     * start animation
     */
    public void startAnimation() {
        this.isDelyStop = false;
        if (animator.isRunning())
            animator.cancel();
        if (sendMsgable) {
            removeMessages();
            handler.sendEmptyMessageDelayed(msgWhat, snowDuration);
        }
        initSnows();
        animator.start();
    }

    /**
     * set the duration of animation
     */
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

    @Override
    protected void onDetachedFromWindow() {
        removeMessages();
        if (animator.isRunning())
            animator.cancel();
        super.onDetachedFromWindow();

    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == msgWhat)
                isDelyStop = true;
        }
    };

    private void removeMessages() {
        if (handler.hasMessages(msgWhat))
            handler.removeMessages(msgWhat);
    }


    private class animatorUpdateListenerImp implements ValueAnimator.AnimatorUpdateListener {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            for (int i = 0; i < snowList.size(); i++) {
                Snow snow = snowList.get(i);
                snow.x += snow.xSpeed;
                if(snow.y > 0 && snow.y < 50){
                    if(snow.timeCurrent == 0){
                        snow.timeCurrent = System.currentTimeMillis();
                        snow.timeEnd = snow.timeCurrent + 5000;
                    }
                    if(snow.timeCurrent < snow.timeEnd){
                        snow.delayBool = true;
                        snow.timeCurrent = System.currentTimeMillis();
                    }
                    else snow.delayBool = false;
                }
                if(!snow.delayBool){
                    snow.y -= snow.ySpeed;
                }
                Random mRandom = new Random();
                int index = mRandom.nextInt(imageArr.length);
                snow.snowBitmap = BitmapFactory.decodeResource(getContext().getResources(), imageArr[index]);
                Log.d(TAG, "onAnimationUpdate() snow = "+snow.toString());
                if (snow.x < -snow.bpWidth || snow.x > getWidth()) {
                    /**
                     *  the snow falling to the sides
                     */
                    if (isDelyStop)
                        snowList.remove(i);
                    else {
                        snow.x = randomX(snow.bpWidth);
                        snow.y = randomY(snow.bpHeight);
                    }
                }
                else if (snow.y < 0) {
                    /**
                     * the snow falling to the Top
                     */
                    if (isDelyStop)
                        snowList.remove(i);
                    else {
                        snow.x = randomX(snow.bpWidth);
                        snow.y = yHeight - snow.bpHeight;
                        snow.delayBool = false;
                        snow.timeCurrent = 0;
                        snow.timeEnd = 0;
                    }
                }
            }
            /**
             * to prevent the animator running empty
             */
            if (snowList.size() <= 0 && animator.isRunning())
                animator.cancel();
            invalidate();
        }
    }

    class Snow {
        private float x;
        private float y;
        private float xSpeed;
        private float ySpeed;
        private int bpHeight;
        private int bpWidth;
        private Bitmap snowBitmap;
        private long timeCurrent = 0;
        private long timeEnd = 0;
        private boolean delayBool = false;
        private float BASESPEED = 100.0f;

        Snow(float xSpeed, float ySpeed, Bitmap snowBitmap) {
            float tempScale = minScale + (float) (Math.random() * (maxScale - minScale));
            this.bpHeight = (int) (snowBitmap.getHeight() * tempScale);
            this.bpWidth = (int) (snowBitmap.getWidth() * tempScale);
            this.x = randomX(bpWidth);
            this.y = randomY(bpHeight);
            Log.d(TAG, "Snow() x = "+x + " , y = "+y);
            /**
             * xDirection > 0 right falling
             * xDirection < 0 left falling
             * xDirection = 0 vertical falling
             */
            float xDirection = 1.0f - (float) (Math.random() * 2.0f);
            this.xSpeed = xSpeed * xDirection / BASESPEED;
            this.ySpeed = (ySpeed + ySpeed * (float) Math.random()) / BASESPEED;
            this.snowBitmap = Bitmap.createScaledBitmap(snowBitmap, bpWidth, bpHeight, true);
        }

        @Override
        public String toString() {
            return "Snow{" +
                    "x=" + x +
                    ", y=" + y +
                    ", xSpeed=" + xSpeed +
                    ", ySpeed=" + ySpeed +
                    ", bpHeight=" + bpHeight +
                    ", bpWidth=" + bpWidth +
                    ", snowBitmap=" + snowBitmap +
                    ", BASESPEED=" + BASESPEED +
                    '}';
        }
    }

    /**
     * the x coordinate
     */
    private float randomX(int bpWidth) {
        return xWidth / 2 + xRandom.nextFloat() * (xWidth / 6) - (xWidth / 6);
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
