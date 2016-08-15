package com.example.lenovo.imageshow;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowManager;

public class ScreenUtil {
    private static WindowManager.LayoutParams lp;
    public static DisplayMetrics getScreenDisplayMetrics(Context context){
        WindowManager wm= (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics out=new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(out);
        return out;
    }

    public static void getShare(Context context,String username,String password,boolean left,boolean right){
        SharedPreferences sp=context.getSharedPreferences("Login",Context.MODE_PRIVATE);
        SharedPreferences.Editor ed=sp.edit();
        //根据checkbox的状态来存储内容
        if(!left)ed.putString("password",password);
        if(right)ed.putString("name",username);

        //将checkbox的状态写进去
        ed.putBoolean("leftcheckbox", left);
        ed.putBoolean("rightcheckbox",right);
        ed.apply();
    }

    public static void setScreenAlpha(Window window,float alpha){
        lp=window.getAttributes();
        lp.alpha=alpha;
        window.setAttributes(lp);
    }

    public static float getScreenAlpha(Window window){
        lp=window.getAttributes();
        return lp.alpha;
    }
}
