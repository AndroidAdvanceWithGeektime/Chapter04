package com.hprof.bitmap;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    private static Map<String, Entity> map = new HashMap<>();
    private static Method method;
    static {
        try {
            method = ArrayInstance.class.getDeclaredMethod("asRawByteArray", int.class, int.class);
            method.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        HprofBuffer buffer = new MemoryMappedFileBuffer(new File(args[0]));
        HprofParser parser = new HprofParser(buffer);
        Snapshot snapshot = parser.parse();
        snapshot.computeDominators();

        if (method == null) {
            throw new IllegalStateException("array instance reflect error");
        }
        ClassObj classObj = snapshot.findClass("android.graphics.Bitmap");
        Heap heap = snapshot.getHeap("app");
        List<Instance> instances = classObj.getHeapInstances(heap.getId());
        map.clear();

        analysis(instances);

        for (Entity e : map.values()) {
            System.out.println(e);
        }
    }

    private static void analysis(List<Instance> instances) {
        for (Instance i : instances) {
            try {
                ArrayInstance buffer = fieldValue(((ClassInstance) i).getValues(), "mBuffer");
                int width = fieldValue(((ClassInstance) i).getValues(), "mWidth");
                int height = fieldValue(((ClassInstance) i).getValues(), "mHeight");
                byte[] data = (byte[]) method.invoke(buffer, 0, buffer.getSize());
                String key = getMd5(data);
                Entity entity = map.get(key);
                if (entity == null) {
                    entity = new Entity(data.length, width, height, key);
                    map.put(key, entity);
                }
                entity.addInstance(i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static <T> T fieldValue(List<ClassInstance.FieldValue> values, String fieldName) {
        for (ClassInstance.FieldValue fieldValue : values) {
            if (fieldValue.getField().getName().equals(fieldName)) {
                return (T) fieldValue.getValue();
            }
        }
        throw new IllegalArgumentException("Field " + fieldName + " does not exists");
    }

    public static String getMd5(byte[] bytes) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(bytes, 0, bytes.length);
            return byteArrayToHex(md5.digest()).toLowerCase();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static String byteArrayToHex(byte[] byteArray) {
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] resultCharArray = new char[byteArray.length * 2];
        int index = 0;
        for (byte b : byteArray) {
            resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];
            resultCharArray[index++] = hexDigits[b & 0xf];
        }
        return new String(resultCharArray);
    }

    static class Entity {
        private List<Instance> instances = new ArrayList<>();
        private String hash;
        private int len;
        private int width;
        private int height;

        public Entity(int len, int width, int height, String hash) {
            this.hash = hash;
            this.len = len;
            this.width = width;
            this.height = height;
        }

        public void addInstance(Instance i) {
            this.instances.add(i);
        }

        @Override
        public String toString() {
            JSONObject object = new JSONObject();
            object.fluentPut("duplicateCount", instances.size());
            object.fluentPut("bufferHash", hash);
            object.fluentPut("width", width);
            object.fluentPut("height", height);
            object.fluentPut("bufferSize", len);
            JSONArray array = new JSONArray();
            for (Instance i : instances) {
                JSONArray path = new JSONArray();
                List<Instance> list = getStackInfo(i);
                for (Instance p : list) {
                    path.add("" + p);
                }
                if (!path.isEmpty()) {
                    array.add(path);
                }
            }
            object.fluentPut("stacks", array);
            return object.toString();
        }

        private List<Instance> getStackInfo(Instance instance) {
            List<Instance> arrayList = new ArrayList<>();
            Instance nextInstance;
            while ((nextInstance = instance.getNextInstanceToGcRoot()) != null) {
                arrayList.add(nextInstance);
                instance = nextInstance;
            }
            return arrayList;
        }
    }
}
