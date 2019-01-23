package com.hprof.bitmap;

import java.util.List;

public class DuplicateResult {
   private int  duplcateCount;
   private List<String> stack;
   private int bufferHash;
   private int width;
   private int height;
   private long bufferSize;

    public int getDuplcateCount() {
        return duplcateCount;
    }

    public void setDuplcateCount(int duplcateCount) {
        this.duplcateCount = duplcateCount;
    }

    public List<String> getStack() {
        return stack;
    }

    public void setStack(List<String> stack) {
        this.stack = stack;
    }

    public int getBufferHash() {
        return bufferHash;
    }

    public void setBufferHash(int bufferHash) {
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

    public long getBufferSize(int size) {
        return bufferSize;
    }

    public void setBufferSize(long bufferSize) {
        this.bufferSize = bufferSize;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
