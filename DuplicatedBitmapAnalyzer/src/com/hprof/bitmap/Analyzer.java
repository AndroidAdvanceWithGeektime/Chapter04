package com.hprof.bitmap;

import com.alibaba.fastjson.JSON;
import com.squareup.haha.perflib.*;
import com.squareup.haha.perflib.io.HprofBuffer;
import com.squareup.haha.perflib.io.MemoryMappedFileBuffer;
import jdk.nashorn.internal.ir.WhileNode;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Analyzer {
    private HashMap<ArrayInstance, Instance> byteArrayToBitmapMap = new HashMap<>();
    Set<ArrayInstance> byteArrays = new HashSet<>();
    private Snapshot snapshot;
    ShortestPathFinder shortestPathFinder = new ShortestPathFinder(null);
    private List<DuplicateResult> duplicateResults = new ArrayList<>();

    private HashMap<ArrayInstance, Object[]> cacheValuesMap = new HashMap<>();

    public void find() throws IOException {
        System.out.println();
        File heapFile = new File(System.getProperty("user.dir") + "\\DuplicatedBitmapAnalyzer\\res\\hprof\\1.hprof");
        HprofBuffer hprofBuffer = new MemoryMappedFileBuffer(heapFile);
        HprofParser parser = new HprofParser(hprofBuffer);
        snapshot = parser.parse();
        ClassObj bitmapClass = snapshot.findClass("android.graphics.Bitmap");
        List<Instance> bitmapInstances = getBitmapInstances(snapshot, bitmapClass);
        bitmapInstances.stream().forEach(instance -> {
            ArrayInstance arrayInstance = HahaHelper.fieldValue(((ClassInstance) instance).getValues(), "mBuffer");
            byteArrayToBitmapMap.put(arrayInstance, instance);

        });
        byteArrays.addAll(byteArrayToBitmapMap.keySet());


        // 缓存Bitmap的数组
        cacheValuesMap = new HashMap<>(byteArrays.size());
        byteArrays.forEach(arrayInstance -> {
            cacheValuesMap.put(arrayInstance, arrayInstance.getValues());
        });

        // 根据Bitmap数组长度进行分类
        Map<Integer, Set<ArrayInstance>> arrayInstanceMapBySize =
                byteArrays.stream().collect(Collectors.groupingBy(ArrayInstance::getSize,
                        Collectors.mapping(Function.identity(), Collectors.toSet())));

        // bitmap数组大小不同的判定为非重复图片，移除
        arrayInstanceMapBySize.keySet().stream()
                .filter(key -> arrayInstanceMapBySize.get(key).size() > 1)
                .forEach(size -> compareBitmapIsSame(arrayInstanceMapBySize.get(size), size));
    }


    /*
     * 通过比较两个Bitmap的hashcode
     */
    private void compareBitmapIsSame(Set<ArrayInstance> bitmaps, int bitmapArrayLength) {
        Map<Object, Set<ArrayInstance>> prefixMap = new HashMap<>();

        for (int column = 0; column < bitmapArrayLength; column++) {
            prefixMap.clear();
            for (ArrayInstance arrayInstance : bitmaps) {
                Object[] bitmapArray = cacheValuesMap.get(arrayInstance);

                if (prefixMap.containsKey(bitmapArray[column])) {
                    prefixMap.get(bitmapArray[column]).add(arrayInstance);
                } else {
                    Set<ArrayInstance> prefixBitmapArrays = new HashSet<>();
                    prefixBitmapArrays.add(arrayInstance);
                    prefixMap.put(bitmapArray[column], prefixBitmapArrays);
                }
            }

            // 每次移除
            prefixMap.forEach((key, value) -> {
                if (value.size() < 2) {
                    bitmaps.remove(value.toArray()[0]);
                }
            });
        }

        prefixMap.forEach((key, arrayInstances) -> arrayInstances.forEach(arrayInstance -> {
            if (arrayInstances.size() > 1) {
                duplicateResults.add(getDuplicateResult(byteArrayToBitmapMap.get(arrayInstance), arrayInstances.size()));
            }
        }));
        System.out.println(JSON.toJSONString(duplicateResults));
    }

    /*
     * art虚拟机堆存储区域分为default、app、image、zygote, 这里只分析在app heap中的bitmap
     * 整个过程：先拿到Bitmap的类对象，根据类对象获取到堆里面的对象实例
     */
    private List<Instance> getBitmapInstances(Snapshot snapshot, ClassObj bitmapClass) {
        List<Instance> reachableInstances = new ArrayList<>();
        snapshot.getHeaps()
                .stream()
                .filter(heap -> "app".equals(heap.getName()))
                .forEach(heap -> bitmapClass
                        .getHeapInstances(heap.getId())
                        .stream()
//                        .filter(instance -> instance.getDistanceToGcRoot() != Integer.MAX_VALUE)
                        .forEach(instance -> {
                            reachableInstances.add(instance);
                        }));
        return reachableInstances;
    }

    /**
     * 打印堆栈信息
     */
    private DuplicateResult getDuplicateResult(Instance instance, int size) {
        DuplicateResult duplicateResult = new DuplicateResult();
        duplicateResult.setDuplcateCount(size);
        duplicateResult.setHeight(HahaHelper.fieldValue(((ClassInstance) instance).getValues(), "mHeight"));
        duplicateResult.setWidth(HahaHelper.fieldValue(((ClassInstance) instance).getValues(), "mWidth"));
        ShortestPathFinder.Result result = shortestPathFinder.findPath(snapshot, instance);
        LeakNode leakNode = result.leakingNode;
        List<String> stackInfo = new ArrayList<>();
        while (leakNode != null) {
            stackInfo.add(leakNode.instance.toString());
            leakNode = leakNode.parent;
        }
        duplicateResult.setStack(stackInfo);
        duplicateResult.setBufferHash(instance.hashCode());
        duplicateResult.getBufferSize(instance.getSize());
        return duplicateResult;
    }
}
