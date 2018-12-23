package com.hprof.bitmap;

import android.os.Debug;
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

public class DuplicatedBitmapCheck {
    private File mFile;

    private DuplicatedBitmapCheck() {
    }

    private static class DuplicatedBitmapCheckHolder {
        private static final DuplicatedBitmapCheck sInstance = new DuplicatedBitmapCheck();
    }

    public static DuplicatedBitmapCheck getInstance() {
        return DuplicatedBitmapCheckHolder.sInstance;
    }

    public void dump(File file) {
        //1.手动出发一次GC后获取hprof文件
        triggerGc();
        try {
            //生成了Hprof文件
            Debug.dumpHprofData(file.getAbsolutePath());
            mFile = file;

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void analyze() {
        if (mFile == null || !mFile.exists()) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 打开hprof文件
                HprofBuffer bufferr = null;
                try {
                    bufferr = new MemoryMappedFileBuffer(mFile);

                    HprofParser parser = new HprofParser(bufferr);
                    Snapshot snapshot = parser.parse();
                    // 获得Bitmap Class
                    final ClassObj bitmapClass = snapshot.findClass("android.graphics.Bitmap");

                    // 获得heap, 只需要分析app和default heap即可
                    Heap heap = snapshot.getHeap("app");
                    // 从heap中获得所有的Bitmap实例
                    final List<Instance> bitmapInstances = bitmapClass.getHeapInstances(heap.getId());
                    // 从Bitmap实例中获得buffer数组
                    if (bitmapInstances == null || bitmapInstances.size() == 0) {
                        //不存在相同的bitmap
                        return;
                    }

                    int[] buffers = new int[bitmapInstances.size()];
                    for (int i = 0; i < bitmapInstances.size(); i++) {
                        ArrayInstance arrayInstance = HahaHelper.fieldValue(((ClassInstance) bitmapInstances.get(i)).getValues(), "mBuffer");
                        buffers[i] = Arrays.hashCode(arrayInstance.getValues());

                    }
                    HashMap<Integer, ArrayList<Instance>> map = new HashMap<>();
                    for (int i = 0; i < buffers.length; i++) {
                        if (!map.containsKey(buffers[i])) {
                            ArrayList<Instance> list = new ArrayList<>();
                            list.add(bitmapInstances.get(i));
                            map.put(buffers[i], list);
                        } else {
                            continue;
                        }
                        for (int j = i + 1; j < buffers.length; j++) {
                            if (buffers[i] == buffers[j]) {
                                map.get(buffers[i]).add(bitmapInstances.get(j));
                            }
                        }
                    }
                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<Integer, ArrayList<Instance>> entry : map.entrySet()) {
                        if (entry.getValue().size() > 1) {
                            sb.append("duplicateCount:" + entry.getValue().size() + "\n");
                            List<Instance> instances = entry.getValue();
                            sb.append("stacks:" + "\n");
                            String[] infos = new String[instances.size()];
                            for (int i = 0; i < instances.size(); i++) {
                                infos[i] = getTraceString(getTraceFromInstance(instances.get(i)));
                            }
                            Instance instance = instances.get(0);
                            sb.append(Arrays.toString(infos) + "\n");
                            sb.append("bufferHashcode:" + entry.getKey() + "\n");
                            int width = HahaHelper.fieldValue(((ClassInstance) instance).getValues(), "mWidth");
                            int height = HahaHelper.fieldValue(((ClassInstance) instance).getValues(), "mHeight");
                            sb.append("width:" + width + "\n");
                            sb.append("height:" + height + "\n");
                            ArrayInstance arrayInstance = HahaHelper.fieldValue(((ClassInstance) instance).getValues(), "mBuffer");

                            sb.append("bufferSize:" + arrayInstance.getValues().length);
                        }
                    }
                    if (TextUtils.isEmpty(sb.toString())) {
                        sb.append("no duplicatedBitmap" + "\n");
                    }
                    if (mDuplicatedBitmapListener != null) {
                        mDuplicatedBitmapListener.bitmapInfo(sb.toString());
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public String getTraceString(ArrayList<Instance> trace) {
        StringBuilder stringBuilder = new StringBuilder();
        if (trace != null && trace.size() > 0) {
            for (Instance instance : trace) {
                stringBuilder.append(instance.getClassObj().getClassName());
                stringBuilder.append("\n");
            }
        }
        return stringBuilder.toString();
    }

    private static ArrayList<Instance> getTraceFromInstance(Instance instance) {
        ArrayList<Instance> arrayList = new ArrayList<>();
        Instance nextInstance = null;
        while ((nextInstance = instance.getNextInstanceToGcRoot()) != null) {
            arrayList.add(nextInstance);
            instance = nextInstance;
        }
        return arrayList;
    }


    private void triggerGc() {
        Runtime.getRuntime().gc();
        enqueueReferences();
        System.runFinalization();
    }

    private void enqueueReferences() {
        // Hack. We don't have a programmatic way to wait for the reference queue daemon to move
        // references to the appropriate queues.
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new AssertionError();
        }
    }

    public DuplicatedBitmapListener mDuplicatedBitmapListener;

    public void setDuplicatedBitmapListener(DuplicatedBitmapListener listener) {
        mDuplicatedBitmapListener = listener;
    }

    public interface DuplicatedBitmapListener {
        void bitmapInfo(String info);
    }
}
