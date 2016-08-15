package com.example.lenovo.imageshow;


import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImageAdapter extends BaseAdapter {
    private String mDirPath;
    private List<String> mDatas;
    private LayoutInflater inflater;
    private static Set<String> mSelected = new HashSet<String>();//记录按钮是否点击过

    public ImageAdapter(Context context, List<String> mDatas, String dirPath) {
        this.mDirPath = dirPath;
        this.mDatas = mDatas;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mDatas.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder viewholder;
        if (convertView == null) {
            viewholder = new ViewHolder();
            convertView = inflater.inflate(R.layout.image_scan_gridview_item, parent, false);
            viewholder.imagebutton = (ImageButton) convertView.findViewById(R.id.select_image);
            viewholder.imageview = (ImageView) convertView.findViewById(R.id.item_image);
            convertView.setTag(viewholder);
        } else {
            viewholder = (ViewHolder) convertView.getTag();
        }

        //设置初始图片
        viewholder.imageview.setImageResource(R.drawable.image_loading);
        viewholder.imageview.setColorFilter(null);
        viewholder.imagebutton.setImageResource(R.drawable.icon_unseleted);

        //得到图片的绝对路径
        final String filepath=mDirPath + "/" + mDatas.get(position);
        //设置图片的点击事件
        viewholder.imageview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //已经被选择
                if (mSelected.contains(filepath)) {
                    mSelected.remove(filepath);
                    viewholder.imagebutton.setImageResource(R.drawable.icon_unseleted);
                    viewholder.imageview.setColorFilter(null);//选中或没选中
                } else {
                    //未被选择
                    mSelected.add(filepath);
                    viewholder.imagebutton.setImageResource(R.drawable.icon_selected);
                    viewholder.imageview.setColorFilter(Color.parseColor("#77000000"));
                }
                //notifyDataSetChanged();
            }
        });
        //用于保存上一次点击的状态
        if(mSelected.contains(filepath)){
            viewholder.imagebutton.setImageResource(R.drawable.icon_selected);
            viewholder.imageview.setColorFilter(Color.parseColor("#77000000"));
        }else{
            viewholder.imagebutton.setImageResource(R.drawable.icon_unseleted);
            viewholder.imageview.setColorFilter(null);
        }
        //加载图片
        ImageLoader.getInstance(3, ImageLoader.Type.FILO).loadimage(mDirPath + "/" + mDatas.get(position), viewholder.imageview);
        return convertView;
    }

    static class ViewHolder {
        ImageView imageview;
        ImageButton imagebutton;
    }
}