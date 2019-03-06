package com.hprof.bitmap;

import java.util.List;

/**
 * Created by WolfXu on 2019/3/5.
 *
 * @author WolfXu
 * @email wolf.xu@ximalaya.com
 * @phoneNumber 13670268092
 */
public class OutputModule {
    private int width;
    private int height;
    private int bufferSize;
    private String bufferHash;
    private int duplicateCount;
    private List<List<String>> stacks;

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

    public String getBufferHash() {
        return bufferHash;
    }

    public void setBufferHash(String bufferHash) {
        this.bufferHash = bufferHash;
    }

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(int duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    public List<List<String>> getStacks() {
        return stacks;
    }

    public void setStacks(List<List<String>> stacks) {
        this.stacks = stacks;
    }
}
