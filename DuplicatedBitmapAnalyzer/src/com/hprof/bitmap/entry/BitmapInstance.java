package com.hprof.bitmap.entry;

import com.hprof.bitmap.utils.HahaHelper;
import com.hprof.bitmap.utils.TraceUtils;
import com.squareup.haha.perflib.ArrayInstance;
import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.ClassObj;
import com.squareup.haha.perflib.Instance;
import com.squareup.haha.perflib.Snapshot;
import com.squareup.leakcanary.AnalysisResult;
import com.squareup.leakcanary.AnalyzerProgressListener;
import com.squareup.leakcanary.ExcludedRefs;
import com.squareup.leakcanary.HeapAnalyzer;
import com.squareup.leakcanary.Reachability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Zengshaoyi
 * @version 1.0 <p><strong>Features draft description.主要功能介绍</strong></p>
 * @since 2018/12/26 15:35
 */
public class BitmapInstance {

    private String mHash;

    private Instance mInstance;

    private ArrayList<Instance> mTraceStack = new ArrayList<>();

    private int mWith;

    private int mHeight;

    private int size;

    private Snapshot mSnapshot;

    public BitmapInstance(Snapshot snapshot, String hash, Instance instance) {
        mSnapshot = snapshot;
        mHash = hash;
        mInstance = instance;
        mTraceStack.addAll(TraceUtils.getTraceFromInstance(mInstance));

        List<ClassInstance.FieldValue> classFieldList = HahaHelper.classInstanceValues(instance);
        mWith = HahaHelper.fieldValue(classFieldList, "mWidth");
        mHeight = HahaHelper.fieldValue(classFieldList, "mHeight");

        ArrayInstance arrayInstance = HahaHelper.fieldValue(classFieldList, "mBuffer");
        byte[] mBufferByte = HahaHelper.getByteArray(arrayInstance);
        if (mBufferByte != null) {
            size = mBufferByte.length;
        }
    }

    public String getHash() {
        return mHash;
    }

    public Instance getInstance() {
        return mInstance;
    }

    public String getTrace() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < mTraceStack.size(); i++) {
            String className;
            if (mTraceStack.get(i) instanceof ClassObj) {
                className = ((ClassObj) mTraceStack.get(i)).getClassName();
            } else {
                className = mTraceStack.get(i).getClassObj().getClassName();
            }

            builder.append("\"").append(className).append("\"");
            if (i != mTraceStack.size() - 1) {
                builder.append(",");
            }else{
                builder.append("]");
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    public String getTraceFromLeakCanary(){
        HeapAnalyzer analyzer = new HeapAnalyzer(ExcludedRefs.builder().build(),AnalyzerProgressListener.NONE,
                Collections.<Class<? extends Reachability.Inspector>>emptyList());


        AnalysisResult ar = analyzer.findLeakTrace(System.nanoTime(), mSnapshot, mInstance,true);
        return ar.leakTrace.toString();
    }

    public int getWith() {
        return mWith;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getSize() {
        return size;
    }
}
