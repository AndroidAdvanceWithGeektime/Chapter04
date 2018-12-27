package com.hprof.bitmap.entry;

import java.util.ArrayList;

/**
 * @author Zengshaoyi
 * @version 1.0 <p><strong>Features draft description.主要功能介绍</strong></p>
 * @since 2018/12/26 15:50
 */
public class DuplicatedCollectInfo {

    // hash
    private String mHash;

    // 相同 hash 的 Bitmap
    private ArrayList<BitmapInstance> mBitmapInstances = new ArrayList<>();

    // mBitmapInstances size
    private int duplicatedCount;

    private int size;

    private int width;

    private int height;

    public DuplicatedCollectInfo(String hash) {
        this.mHash = hash;
    }

    public void addBitmapInstance(BitmapInstance bitmapInstance) {
        mBitmapInstances.add(bitmapInstance);
    }

    public void internalSetValue() {
        duplicatedCount = mBitmapInstances.size();
        if(mBitmapInstances.size() > 0){
            BitmapInstance instance = mBitmapInstances.get(0);
            this.width = instance.getWith();
            this.height = instance.getHeight();
            this.size = instance.getSize();
        }
    }

    public String string() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n")
                .append("\t\"hash\":").append(mHash).append(",\n")
                .append("\t\"size\":").append(size).append(",\n")
                .append("\t\"width\":").append(width).append(",\n")
                .append("\t\"height\":").append(height).append(",\n")
                .append("\t\"duplicatedCount\":").append(duplicatedCount).append(",\n")
                .append("\t\"stack\":").append("[\n");

        for(int i=0;i<mBitmapInstances.size();i++){
            builder.append(mBitmapInstances.get(i).getTraceFromLeakCanary());
            if(i != mBitmapInstances.size() -1 ){
                builder.append(",\n");
            }
        }

        builder.append("]\n")
                .append("}\n");
        return builder.toString();
    }
}
