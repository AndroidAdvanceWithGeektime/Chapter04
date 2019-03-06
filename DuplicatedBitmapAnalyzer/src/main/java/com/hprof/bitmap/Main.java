package com.hprof.bitmap;

import com.google.gson.Gson;
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
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args != null && args.length >= 1) {
            File heapDumpFile = new File(args[0]);
            HprofBuffer buffer = new MemoryMappedFileBuffer(heapDumpFile);
            HprofParser parser = new HprofParser(buffer);
            Snapshot snapshot = parser.parse();
            snapshot.computeDominators();

            ClassObj bitmapClass = snapshot.findClass("android.graphics.Bitmap");
            Heap heap = snapshot.getHeap("app");
            if (heap != null) {
                Map<String, List<Instance>> bufferMd5Map = new HashMap<>();
                List<Instance> bitmapInstances = bitmapClass.getHeapInstances(heap.getId());
                for (Instance instance : bitmapInstances) {
                    if (instance != null) {
                        ArrayInstance bitmapBuffer = HahaHelper.fieldValue(((ClassInstance) instance).getValues(),
                                "mBuffer");
                        if (bitmapBuffer != null) {
                            String md5 = getMD5(bitmapBuffer.getValues());
                            List<Instance> list = bufferMd5Map.get(md5);
                            if (list == null) {
                                list = new ArrayList<>();
                                bufferMd5Map.put(md5, list);
                            }
                            list.add(instance);
                        }
                    }
                }
                List<OutputModule> outputModules = new ArrayList<>();
                for (Map.Entry<String, List<Instance>> entry : bufferMd5Map.entrySet()) {
                    if (entry.getValue() != null && entry.getValue().size() >= 2) {
                        OutputModule outputModule = new OutputModule();
                        boolean hasSetBaseInfo = false;
                        for (Instance instance : entry.getValue()) {
                            if (!hasSetBaseInfo) {
                                hasSetBaseInfo = true;
                                outputModule.setWidth(HahaHelper.fieldValue(((ClassInstance) instance).getValues(), "mWidth"));
                                outputModule.setHeight(HahaHelper.fieldValue(((ClassInstance) instance).getValues(), "mHeight"));
                                outputModule.setBufferHash(entry.getKey());
                                outputModule.setDuplicateCount(entry.getValue().size());
                                ArrayInstance bitmapBuffer = HahaHelper.fieldValue(((ClassInstance) instance).getValues(),
                                        "mBuffer");
                                outputModule.setBufferSize(bitmapBuffer.getSize());
                                outputModule.setStacks(new ArrayList<>());
                            }

                            List<String> stack = new ArrayList<>();
                            do {
                                stack.add(instance.toString());
                                instance = instance.getNextInstanceToGcRoot();
                            } while (instance != null);
                            outputModule.getStacks().add(stack);
                        }
                        outputModules.add(outputModule);
                    }
                }
                System.out.println(new Gson().toJson(outputModules));
            }

        }
    }

    private static String getMD5(Object[] source) {
        if (source == null || source.length <= 0) {
            return null;
        }
        byte[] bytes = new byte[source.length];
        int i = 0;
        for (Object object : source) {
            if (object instanceof Byte) {
                bytes[i++] = (byte) object;
            }
        }
        StringBuilder sb = new StringBuilder();
        java.security.MessageDigest md5 = null;
        try {
            md5 = java.security.MessageDigest.getInstance("MD5");
            md5.update(bytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if (md5 != null) {
            for (byte b : md5.digest()) {
                sb.append(String.format("%02X", b));
            }
        }
        return sb.toString();
    }
}
