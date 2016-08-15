package com.example.lenovo.imageshow;

/*
* 用于记录文件中的照片的各种信息
* */
public class FolderBean {
    private String dir;
    private String filename;
    private String firstimagedir;
    private int count;//该文件中总共有多少图片

    public String getFilename() {
        return filename;
    }



    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getFirstimagedir() {
        return firstimagedir;
    }

    public void setFirstimagedir(String firstimagedir) {
        this.firstimagedir = firstimagedir;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
        int lastindexof=this.dir.lastIndexOf("/");
        this.filename=this.dir.substring(lastindexof);
    }
}
