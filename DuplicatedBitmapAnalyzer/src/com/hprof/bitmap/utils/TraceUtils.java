package com.hprof.bitmap.utils;

import com.squareup.haha.perflib.Instance;

import java.util.ArrayList;

/**
 * @author Zengshaoyi
 * @version 1.0 <p><strong>Features draft description.主要功能介绍</strong></p>
 * @since 2018/12/26 15:41
 */
public class TraceUtils {

    /**
     * 获取 trace
     *
     * @param instance
     * @return
     */
    public static ArrayList<Instance> getTraceFromInstance(Instance instance) {
        ArrayList<Instance> arrayList = new ArrayList<>();
        Instance nextInstance = null;
        while ((nextInstance = instance.getNextInstanceToGcRoot()) != null) {
            arrayList.add(nextInstance);
            instance = nextInstance;
        }
        return arrayList;
    }

}