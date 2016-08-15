package com.example.lenovo.imageshow;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;




public class MainActivity extends AppCompatActivity {
    private GridView gridView;
    private TextView text_all;
    private TextView text_count;
    private RelativeLayout relativeLayout;

    private File mCurrentPth;
    private List<FolderBean> mFloderbean = new ArrayList<>();//存放文件夹的信息

    private ProgressDialog progressDialog;
    private int mMaxCount;
    private ImageAdapter adapter;//gridview的适配器
    private List<String> mImgs = new ArrayList<>(); //存放图片的相对路径

    private ImagePopupWindow imagePopupWindow;//弹出窗口


    //该handler用于当扫描完之后做一些事情
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            progressDialog.dismiss();
            //绑定数据到view中
            dataToView();
            //初始化popupwindow
            InitPopupWindow();
        }
    };

    //初始化popupwindow
    private void InitPopupWindow() {
        imagePopupWindow=new ImagePopupWindow(this,mFloderbean);

        imagePopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                //内容区域变量
            }
        });

        imagePopupWindow.setOnDirItemClick(new ImagePopupWindow.OnDirItemClick() {
            @Override
            public void onClick(FolderBean folderBean) {
                mCurrentPth = new File(folderBean.getDir());
                mImgs = Arrays.asList(mCurrentPth.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png"))
                            return true;
                        return false;
                    }
                }));
                adapter = new ImageAdapter(MainActivity.this, mImgs, mCurrentPth.getAbsolutePath());
                gridView.setAdapter(adapter);

                text_all.setText(folderBean.getFilename());
                text_count.setText(folderBean.getCount() + "");

                imagePopupWindow.dismiss();
            }
        });
    }

    private void dataToView() {
        if (mCurrentPth == null) {
            Toast.makeText(MainActivity.this, "未扫描到任何图片", Toast.LENGTH_SHORT).show();
            return;
        }

        mImgs = Arrays.asList(mCurrentPth.list());//获取图片的名字
        adapter = new ImageAdapter(this, mImgs, mCurrentPth.getAbsolutePath());

        gridView.setAdapter(adapter);

        text_all.setText(mCurrentPth.getName());
        text_count.setText(mMaxCount + "");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridView = (GridView) findViewById(R.id.imageview_gridview);
        text_all = (TextView) findViewById(R.id.all_image);
        text_count = (TextView) findViewById(R.id.image_count);
        relativeLayout = (RelativeLayout) findViewById(R.id.my_relativelayout);

        initdata();//从sd卡中获取图片

        initevent();
    }

    private void initevent() {
        text_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //imagePopupWindow.setAnimationStyle(R.style.PopupWindowStyle);
                imagePopupWindow.showAsDropDown(text_all, 0, 0);
            }
        });


    }

    //扫描图片
    //利用contentprovide扫描图片
    private void initdata() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(MainActivity.this, "不可用", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = ProgressDialog.show(this, null, "正在加载...");

        //启动一个线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                /*
                * 固定写法，同理可以获取音乐或视频等文件
                * */
                Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver contentResolver = MainActivity.this.getContentResolver();

                Cursor cursor = contentResolver.query(uri, null, MediaStore.Images.Media.MIME_TYPE + "=? or " + MediaStore.Images.Media.MIME_TYPE + "=?", new String[]{"image/jpeg", "image/png"}, MediaStore.Images.Media.DATE_MODIFIED);

                Set<String> mSet = new HashSet<String>();//防止重复读取文件夹

                while (cursor.moveToNext()) {

                    //获取图片路径
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    //获取父路径
                    File parentfile = new File(path).getParentFile();
                    if (parentfile == null) continue;

                    String dirpath = parentfile.getAbsolutePath();//获取文件的绝对路径

                    FolderBean folderBean = null;

                    if (mSet.contains(dirpath)) continue;
                    else {
                        mSet.add(dirpath);
                        folderBean = new FolderBean();
                        folderBean.setDir(dirpath);
                        folderBean.setFirstimagedir(path);
                    }

                    if (parentfile.list() == null) continue;

                    //获取文件夹中的图片数量
                    int picSize = parentfile.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String filename) {
                            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png"))
                                return true;
                            return false;
                        }
                    }).length;
                    folderBean.setCount(picSize);

                    mFloderbean.add(folderBean);
                    if (picSize > mMaxCount) {
                        mMaxCount = picSize;
                        mCurrentPth = parentfile;
                    }
                }
                cursor.close();
                //通知handler图片扫描
                handler.sendEmptyMessage(0x110);

            }
        }).start();


    }


}
