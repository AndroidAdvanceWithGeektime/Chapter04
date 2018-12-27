package com.hprof.bitmap;

import com.hprof.bitmap.entry.BitmapInstance;
import com.hprof.bitmap.entry.DuplicatedCollectInfo;
import com.hprof.bitmap.utils.HahaHelper;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws IOException {
        File heapDumpFile = new File("./myhprof.hprof");

        boolean isExists = heapDumpFile.exists();
        System.out.println("heapDumpFile is exists:" + isExists);

        HprofBuffer buffer = new MemoryMappedFileBuffer(heapDumpFile);
        HprofParser parser = new HprofParser(buffer);
        Snapshot snapshot = parser.parse();
        // 必须执行这行代码
        snapshot.computeDominators();

        ClassObj bitmapClass = snapshot.findClass("android.graphics.Bitmap");

        // 只分析 default 和 app
        Heap defaultHeap = snapshot.getHeap("default");
        Heap appHeap = snapshot.getHeap("app");
        // 从 heap 中获取 bitmap instance 实例
        List<Instance> defaultBmInstance = bitmapClass.getHeapInstances(defaultHeap.getId());
        List<Instance> appBmInstance = bitmapClass.getHeapInstances(appHeap.getId());

        defaultBmInstance.addAll(appBmInstance);

        // 从 bitmap 实例中获得 buffer 数组 map
        List<DuplicatedCollectInfo> collectInfos = collectSameBitmap(snapshot, defaultBmInstance);
        for (DuplicatedCollectInfo info : collectInfos) {
            println(info.string());
        }
    }

    private static List<DuplicatedCollectInfo> collectSameBitmap(Snapshot snapshot, List<Instance> bmInstanceList) {
        Map<String, List<Instance>> collectSameMap = new HashMap<>();
        ArrayList<DuplicatedCollectInfo> duplicatedCollectInfos = new ArrayList<>();

        // 收集
        for (Instance instance : bmInstanceList) {
            List<ClassInstance.FieldValue> classFieldList = HahaHelper.classInstanceValues(instance);

            ArrayInstance arrayInstance = HahaHelper.fieldValue(classFieldList, "mBuffer");
            byte[] mBufferByte = HahaHelper.getByteArray(arrayInstance);
            int mBufferHashCode = Arrays.hashCode(mBufferByte);
            String hashKey = String.valueOf(mBufferHashCode);

            if (collectSameMap.containsKey(hashKey)) {
                collectSameMap.get(hashKey).add(instance);
            } else {
                List<Instance> bmList = new ArrayList<>();
                bmList.add(instance);
                collectSameMap.put(hashKey, bmList);
            }

        }

        // 去除只有一例的
        Iterator<Map.Entry<String, List<Instance>>> it = collectSameMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, List<Instance>> entry = it.next();
            if (entry.getValue().size() <= 1) {
                it.remove();
            }
        }

        // 留下重复的图片，创建 duplicatedCollectInfo 对象存入数组中
        for (Map.Entry<String, List<Instance>> entry : collectSameMap.entrySet()) {
            DuplicatedCollectInfo info = new DuplicatedCollectInfo(entry.getKey());
            for (Instance instance : entry.getValue()) {
                info.addBitmapInstance(new BitmapInstance(snapshot,entry.getKey(), instance));
            }
            info.internalSetValue();
            duplicatedCollectInfos.add(info);
        }

        return duplicatedCollectInfos;
    }


    private static void println(String content) {
        System.out.println(content);
    }
}
