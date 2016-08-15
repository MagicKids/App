package com.example.lenovo.imageshow;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.List;

/*
* 自定义popupwindow
* */
public class ImagePopupWindow extends PopupWindow {
    private int width;
    private int height;
    private View v; //自定义布局
    private ListView listView;//自定义布局中的listview
    private List<FolderBean> mFolderBean;   //用于得到当中的一些数据
    private MyAdapter adapter;  //listview的适配器
    private OnDirItemClick listener;    //listview的item的点击回调接口

    public ImagePopupWindow(Context context, List<FolderBean> mDatas) {
        setWidthAndHeight(context);
        v = LayoutInflater.from(context).inflate(R.layout.popwindow_item, null, false);

        this.mFolderBean = mDatas;
        setContentView(v);      //利用该函数设置自定义view

        //设置属性
        setWidth(width);
        setHeight(height);
        setFocusable(true);

        setTouchable(true);

        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());


        //监听从外部点击popupwindow
        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //在外部点击,则关闭弹窗
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });

        initview(context);//初始化自定义布局

        initevent();
    }

    private void initevent() {
        //listview点击事件
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(listener!=null)listener.onClick(mFolderBean.get(position));
            }
        });
    }

    //初始化控件
    private void initview(Context context) {
        listView = (ListView) v.findViewById(R.id.popup_listview);
        adapter = new MyAdapter(context, mFolderBean);

        listView.setAdapter(adapter);

    }

    public void setWidthAndHeight(Context context) {
        width = ScreenUtil.getScreenDisplayMetrics(context).widthPixels;
        height = (int) (ScreenUtil.getScreenDisplayMetrics(context).heightPixels * 0.7);
    }


    //listview的另一种适配器
    private static class MyAdapter extends ArrayAdapter<FolderBean> {
        private LayoutInflater layoutInflater;
        private List<FolderBean> mDatas;

        public MyAdapter(Context context, List<FolderBean> objects) {
            super(context, 0, objects);
            layoutInflater = LayoutInflater.from(context);
            mDatas = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = layoutInflater.inflate(R.layout.popup_listview_item, parent, false);
                viewHolder.imageview = (ImageView) convertView.findViewById(R.id.imageview);
                viewHolder.text_dir = (TextView) convertView.findViewById(R.id.text_folder);
                viewHolder.text_count = (TextView) convertView.findViewById(R.id.text_count);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            //重置
            viewHolder.imageview.setImageResource(R.drawable.image_loading);

            FolderBean folderBean = mDatas.get(position);
            ImageLoader.getInstance(3, ImageLoader.Type.FILO).loadimage(folderBean.getFirstimagedir(), viewHolder.imageview);

            viewHolder.text_count.setText(String.valueOf(folderBean.getCount())+"张");
            viewHolder.text_dir.setText(folderBean.getFilename());
            return convertView;
        }

        static class ViewHolder {
            ImageView imageview;
            TextView text_dir;
            TextView text_count;
        }
    }

    public interface OnDirItemClick{
        void onClick(FolderBean folderBean);
    }
    public void setOnDirItemClick(OnDirItemClick listener){
        this.listener=listener;
    }
}
