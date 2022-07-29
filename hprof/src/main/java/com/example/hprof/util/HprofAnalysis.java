package com.example.hprof.util;

import android.text.TextUtils;

import com.squareup.haha.perflib.ArrayInstance;
import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.ClassObj;
import com.squareup.haha.perflib.Heap;
import com.squareup.haha.perflib.HprofParser;
import com.squareup.haha.perflib.Instance;
import com.squareup.haha.perflib.Snapshot;
import com.squareup.haha.perflib.io.HprofBuffer;
import com.squareup.haha.perflib.io.MemoryMappedFileBuffer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HprofAnalysis {

    public static void analysis(String hprofPath) throws IOException {
        if (TextUtils.isEmpty(hprofPath)) {
            return;
        }
        File hprofFile = new File(hprofPath);
        if (!hprofFile.exists()) {
            return;
        }
        HprofBuffer dataBuffer = new MemoryMappedFileBuffer(hprofFile);
        HprofParser parser = new HprofParser(dataBuffer);
        final Snapshot snapshot = parser.parse();
        snapshot.computeDominators();
        final ClassObj bitmapClass = snapshot.findClass("android.graphics.Bitmap");
        Heap appHeap = snapshot.getHeap("app");
        // 拿到所有的 Bitmap 实例
        final List<Instance> bitmapInstances = bitmapClass.getHeapInstances(appHeap.getId());
        if (bitmapInstances == null || bitmapInstances.size() <= 1) {
            return;
        }

        int[] buffers = new int[bitmapInstances.size()];
        for (int i = 0; i < bitmapInstances.size(); i++) {
            Instance bitmapInstance = bitmapInstances.get(i);
            // mBuffer 是一个 byte[]
            ArrayInstance arrayInstance = HahaHelper.fieldValue(((ClassInstance) bitmapInstance).getValues(), "mBuffer");
            buffers[i] = Arrays.hashCode(arrayInstance.getValues());
        }
        HashMap<Integer, List<Instance>> map = new HashMap<>();
        for (int i = 0; i < buffers.length; i++) {
            if (!map.containsKey(buffers[i])) {
                List<Instance> list = new ArrayList<>();
                list.add(bitmapInstances.get(i));
                map.put(buffers[i], list);
            } else {
                map.get(buffers[i]).add(bitmapInstances.get(i));
            }
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, List<Instance>> entry : map.entrySet()) {
            if (entry.getValue().size() > 1) {
                sb.append("\"duplcateCount\":" + entry.getValue().size());
                sb.append("\n");
                sb.append("\"stacks\": \n");
                List<Instance> instanceList = entry.getValue();
                for (int i = 0; i < instanceList.size(); i++) {
                    sb.append("===================================================== \n");
                    sb.append(getTraceString(getTraceFromInstance(instanceList.get(i))));
                    sb.append("===================================================== \n");
                }

                sb.append("\"bufferHashcode\":").append("\"").append(entry.getKey().toString()).append("\"\n");
                int width = HahaHelper.fieldValue(((ClassInstance) entry.getValue().get(0)).getValues(), "mWidth");
                int height = HahaHelper.fieldValue(((ClassInstance) entry.getValue().get(0)).getValues(), "mHeight");
                sb.append("\"width\":" + width + "\n");
                sb.append("\"height\":" + height + "\n");
                sb.append("\"bufferSize\":" + entry.getValue().get(0).getSize() + "\n");
                sb.append("----------------------------------------------------- \n");
            }
        }
        if (!sb.toString().isEmpty()) {
            System.out.println(sb);
        }
    }

    public static ArrayList<Instance> getTraceFromInstance(Instance instance) {
        ArrayList<Instance> arrayList = new ArrayList<>();
        //Instance nextInstance = null;
        while(instance != null && instance.getDistanceToGcRoot() != 0 && instance.getDistanceToGcRoot() != Integer.MAX_VALUE) {
            arrayList.add(instance);
            instance = instance.getNextInstanceToGcRoot();
        }
        return arrayList;
    }

    public static String getTraceString(List<Instance> instances) {
        StringBuilder sb = new StringBuilder();
        if (instances.size() > 0) {
            for (Instance instance : instances) {
                sb.append(instance.getClassObj().getClassName());
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
