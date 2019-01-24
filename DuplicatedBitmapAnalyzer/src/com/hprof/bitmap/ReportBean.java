package com.hprof.bitmap;

import java.util.List;

public class ReportBean {


    /**
     * duplcateCount : 2
     * stacks : [["static com.sample.launcher.controller.LauncherController mLauncherView(0x1349c400)","com.sample.launcher.view.LauncherView mHeaderView(0x1349bc00)","com.sample.homepage.header.HomePageHeaderView mSearchAndAddressBar(0x13118c00)","android.widget.ImageView mDrawable(0x131f9400)","android.graphics.drawable.BitmapDrawable mBitmapState(0x13799b80)","android.graphics.drawable.BitmapDrawable$BitmapState mBitmap(0x13b0f4d8)","android.graphics.Bitmap instance"],["static com.sample.resources.ResourceCache cachePool(0x12d32380)","com.sample.resources.ResourceCache table(0x12c30250)","array java.util.HashMap$HashMapEntry[] [130](0x14724000)","java.util.LinkedHashMap$LinkedHashMapEntry after(0x130ed420)","java.util.LinkedHashMap$LinkedHashMapEntry value(0x1361f780)","com.sample.resources.ResourceCache$CacheSoftReference T(0x1361f760)","android.graphics.drawable.BitmapDrawable mBitmapState(0x136a2d30)","android.graphics.drawable.BitmapDrawable$BitmapState mBitmap(0x13971708)","android.graphics.Bitmap instance"]]
     * bufferHash : 4e2b2a183d1b48bcf2b12af45afdbc12
     * width : 60
     * height : 60
     * bufferSize : 14400
     */

    private int duplcateCount;
    private String bufferHash;
    private int width;
    private int height;
    private int bufferSize;
    private String stacks;

    public int getDuplcateCount() {
        return duplcateCount;
    }

    public void setDuplcateCount(int duplcateCount) {
        this.duplcateCount = duplcateCount;
    }

    public String getBufferHash() {
        return bufferHash;
    }

    public void setBufferHash(String bufferHash) {
        this.bufferHash = bufferHash;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public String getStacks() {
        return stacks;
    }

    public void setStacks(String stacks) {
        this.stacks = stacks;
    }


    @Override
    public String toString() {


        return   "{" + "\n"+
                "duplcateCount :" + getDuplcateCount() + "," + "\n"+
                "bufferHash :"   + "\"" +getBufferHash() + "\"" + "," + "\n"+
                "width :"        + getWidth() + "," + "\n"+
                "height :"       + getHeight() + "," + "\n"+
                "bufferSize :"   + getBufferSize() + "," + "\n"+
                "stacks :"       + getStacks() +"\n"+
                "}";

    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
