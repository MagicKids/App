package com.example.lenovo.imageshow;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;


import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * 图片加载帮助类
 * 使用单例模式
 * */

public class ImageLoader {
    private static ImageLoader imageLoader;
    //需要的变量
    /*
    * 核心缓存类
    * */
    private LruCache<String,Bitmap> mLruCache;
    /*
    * 线程池
    * */
    private ExecutorService mThreadPool;

    private LinkedList<Runnable> mTaskQueue;

    private Thread mPoolThread; //后台轮询线程

    private Handler mPoolThreadHandler;//后台轮询handler

    private Handler mUIhandler;

    private static final int DEFAULT_THREAD_COUNT=4;//默认线程数量

    private Semaphore mThreadSemaphore=new Semaphore(0);//利用信号量解决线程没有开始开始已经被调用的情况
    private Semaphore mTaskQueueSemaphore;
    private Type type=Type.FILO;
    public enum Type{
        FIFO,FILO;
    }

    private ImageLoader(int count ,Type type){
        //初始化变量


        //循环的一种方式,另一种是利用while(true)
        mPoolThread=new Thread(new Runnable() {
            @Override
            public void run() {
                {
                    Looper.prepare();
                    mPoolThreadHandler = new Handler() {
                        //不断循环
                        @Override
                        public void handleMessage(Message msg) {
                            super.handleMessage(msg);


                            //从线程池中获取线程执行
                            mThreadPool.execute(getTask());
                            try {
                                mTaskQueueSemaphore.acquire();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }


                        }
                    };
                    //初始化成功之后释放一个信号量
                    mThreadSemaphore.release();

                    Looper.loop();

                }
            }
        });
        mPoolThread.start();

        //线程池的初始化
        mThreadPool= Executors.newFixedThreadPool(count);
        //线程队列的初始化
        mTaskQueue=new LinkedList<>();
        //缓冲区的初始化
        int maxMemory= (int) Runtime.getRuntime().maxMemory()/8;
        mLruCache=new LruCache<String,Bitmap>(maxMemory){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes()*value.getHeight();//总大小
            }
        };

        mTaskQueueSemaphore=new Semaphore(count);
    }

    //获取线程
    private Runnable getTask() {
        if(Type.FIFO==type){
            return mTaskQueue.removeFirst();
        }else{
            return mTaskQueue.removeLast();
        }

    }

    public static ImageLoader getInstance(int count,Type type){

        if(imageLoader==null){
            synchronized (ImageLoader.class){
                if(imageLoader==null){
                    imageLoader=new ImageLoader(count,type);
                }
            }
        }
        return imageLoader;
    }


    //加载图片
    //根据path为imageview设置图片
    public void loadimage(final String path, final ImageView imageView){
        imageView.setTag(path);

        if(mUIhandler==null){
            mUIhandler=new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    //获取图片
                    //设置图片
                    BitmapHolder bitmapholder= (BitmapHolder) msg.obj;
                    String path=bitmapholder.path;
                    Bitmap bitmap=bitmapholder.bitmap;
                    ImageView imageview=bitmapholder.imageview;
                    if(imageview.getTag().toString().equals(path)){
                        imageview.setImageBitmap(bitmap);
                    }
                }
            };
        }

        //在缓存中查找
        Bitmap bm=getBitmapFromLruCache(path);
        //如果在缓存中，则可以通知UIhandler设置图片
        if(bm!=null){
            Message message=Message.obtain();
            BitmapHolder bitmapholder=new BitmapHolder();
            bitmapholder.bitmap=bm;
            bitmapholder.imageview=imageView;
            bitmapholder.path=path;
            message.obj=bitmapholder;
            mUIhandler.sendMessage(message);
        }else{
            //如果在缓存中找不到，则要创建一个线程
            //加入到线程队列中，并且通知后台轮询线程执行线程

            addTask(new Runnable(){
                @Override
                public void run() {
                    //1.获取图片的大小
                    ImageSize imageSize=getImageViewSize(imageView);
                    //2.利用options压缩图片
                    Bitmap bm=decodeBitmapFromPath(path,imageSize.width,imageSize.height);
                    //3.加载图片并且加入到缓存中
                    addBitmapToLruCache(path, bm);
                    //通知ui线程设置图片


                    Message message=Message.obtain();
                    BitmapHolder bitmapholder=new BitmapHolder();
                    bitmapholder.bitmap=bm;
                    bitmapholder.imageview=imageView;
                    bitmapholder.path=path;
                    message.obj=bitmapholder;
                    mUIhandler.sendMessage(message);

                    mTaskQueueSemaphore.release();
                }
            });
        }
    }

    //将图片加入到缓存中
    private void addBitmapToLruCache(String path, Bitmap bm) {
        if(getBitmapFromLruCache(path)==null){
            if(bm!=null){
                mLruCache.put(path,bm);
            }
        }
    }

    //压缩图片
    //利用options进行压缩
    private Bitmap decodeBitmapFromPath(String path, int width, int height) {
        //获取图片的宽和高，不把图片加到内存中
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(path,options);

        options.inSampleSize= getSampleSize(options,width,height);
        //使用获取到的inSampleSize解析图片
        options.inJustDecodeBounds=false;//将图片防止内存
        Bitmap bitmap=BitmapFactory.decodeFile(path,options);
        return bitmap;
    }

    private int getSampleSize(BitmapFactory.Options options, int reqwidth, int reqheight) {
        int width=options.outWidth;
        int height=options.outHeight;

        int inSampleSize=1;
        if(width>reqwidth || height>reqheight){
            //进行压缩
            //获取压缩比例
            int widthradio=Math.round(width*1.0f/reqwidth);
            int heightradio=Math.round(height*1.0f/reqheight);

            inSampleSize=Math.max(widthradio,heightradio);
        }
        return inSampleSize;
    }

    //将task加入到任务队列中
    private synchronized void addTask(Runnable runnable) {
        mTaskQueue.add(runnable);
        System.out.println("sendss");
        //这里可能会出现mPoolThreadHandler为空的情况
        //在这里可以利用if(mPoolThreadHandler==null)wait();然后在该线程start之后notify
        try {
            //获取一个信号量
            //解决线程没有初始化却被调用出现空指针的情况
            if(mPoolThreadHandler==null)mThreadSemaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mPoolThreadHandler.sendEmptyMessage(0x110);//通知后台轮询线程执行线程池的线程
    }

    public ImageSize getImageViewSize(ImageView imageView) {
        ImageSize imageSize=new ImageSize();

        ViewGroup.LayoutParams layoutParams=imageView.getLayoutParams();
        int width=imageView.getWidth();
        if(width<=0){
            width=layoutParams.width;//获取在layout中获取的宽度
        }
        if(width<=0){
            width=getViewFiledValue(imageView, "mMaxWidth");
        }
        if(width<=0){
            width=ScreenUtil.getScreenDisplayMetrics(imageView.getContext()).widthPixels;
        }

        int height=imageView.getHeight();
        if(height<=0){
            height=layoutParams.height;//获取在layout中获取的宽度
        }
        if(height<=0){
            height=getViewFiledValue(imageView,"mMaxHeight");
        }
        if(height<=0){
            height=ScreenUtil.getScreenDisplayMetrics(imageView.getContext()).heightPixels;
        }
        imageSize.width=width;
        imageSize.height=height;
        return imageSize;
    }
    private class ImageSize{
        int width;
        int height;
    }


    //利用反射机制获取最大宽度和高度
    private int getViewFiledValue(Object object,String fieldname){
        int value=0;
        try {
            Field field=ImageView.class.getDeclaredField(fieldname);
            field.setAccessible(true);
            int fieldvalue=field.getInt(object);
            if(fieldvalue>0 && fieldvalue<Integer.MAX_VALUE){
                value=fieldvalue;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return value;
    }
    //图片信息类
    private class BitmapHolder{
        String path;
        ImageView imageview;
        Bitmap bitmap;

    }
    public Bitmap getBitmapFromLruCache(String path) {
        return mLruCache.get(path);
    }
}
