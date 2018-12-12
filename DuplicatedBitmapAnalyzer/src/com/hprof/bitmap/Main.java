package com.hprof.bitmap;

import com.squareup.haha.perflib.*;
import com.squareup.haha.perflib.io.HprofBuffer;
import com.squareup.haha.perflib.io.MemoryMappedFileBuffer;
import org.json.JSONArray;
import org.json.JSONObject;
import sun.security.provider.MD5;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException {
        String HPROF_PATH = "F:/convert.hprof";
        if(args.length > 0){
            HPROF_PATH = args[0];
        }
        File hprofFile = new File(HPROF_PATH);
        if(!hprofFile.exists()){
            System.out.printf("file: " + HPROF_PATH + " not exist, please check it");
            return;
        }
        Map<String, ArrayList<ObjNode>> objMap = new HashMap<String, ArrayList<ObjNode>>();
        HprofBuffer buffer = new MemoryMappedFileBuffer(hprofFile);
        HprofParser parser = new HprofParser(buffer);
        Snapshot snapshot = parser.parse();
        Heap defaultHeap = null, appHeap = null;
        Collection<Heap> heaps = snapshot.getHeaps();
        //找到 default 和 app heap
        for (Heap heap : heaps) {
            if (heap.getName().equals("default")) {
                defaultHeap = heap;
            } else if (heap.getName().equals("app")) {
                appHeap = heap;
            } else {
                //ignore
            }
        }
        ClassObj bitmapClass = snapshot.findClass("android.graphics.Bitmap");
        if(defaultHeap != null){
            analyzeHeapForSameBuffer(objMap, defaultHeap, bitmapClass);
        }
        if(appHeap != null){
            analyzeHeapForSameBuffer(objMap,appHeap , bitmapClass);
        }
        dumpSameBufferBitmapInfo(objMap);
    }

    /**
     * 处理 heap，找出有相同 buffer 的 bitmap 的 instance，存放在 map 中。
     * @param objMap 存放相同 buffer 的 instance 的map
     * @param heap 待处理的 heap
     * @param classObj 这里是  bitmap 的封装对象
     */
    private static void analyzeHeapForSameBuffer(Map<String, ArrayList<ObjNode>> objMap,
                                    Heap heap, ClassObj classObj){
        List<Instance> instances = classObj.getHeapInstances(heap.getId());
        for (Instance instance : instances){
            ArrayInstance buffer = HahaHelper.fieldValue(HahaHelper.classInstanceValues(instance), "mBuffer");
            byte[] bytes = HahaHelper.getByteArray(buffer);
            try {
                String md5String = Md5Helper.getMd5(bytes);
                if(objMap.containsKey(md5String)){
                    objMap.get(md5String).add(getObjNode(instance));
                }else {
                    ArrayList<ObjNode> objNodes = new ArrayList<>();
                    objNodes.add(getObjNode(instance));
                    objMap.put(md5String, objNodes);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    /**
     * 封装 Instance 对象，包含 Instance 对象和其 trace
     * @param instance
     * @return
     */
    private static ObjNode getObjNode(Instance instance){
        ObjNode objNode = new ObjNode();
        objNode.setInstance(instance);
        objNode.setTrace(getTraceFromInstance(instance));
        return objNode;
    }

    /**
     * 获取 trace
     * @param instance
     * @return
     */
    private static ArrayList<Instance> getTraceFromInstance(Instance instance){
        ArrayList<Instance> arrayList = new ArrayList<>();
        Instance nextInstance = null;
        while ((nextInstance = instance.getNextInstanceToGcRoot()) != null){
            arrayList.add(nextInstance);
            instance = nextInstance;
        }
        return arrayList;
    }

    /**
     * 根据 map 的内容，生成 json 格式的输出
     * @param map
     */
    private static void dumpSameBufferBitmapInfo(Map<String, ArrayList<ObjNode>> map){
        JSONArray jsonResult = new JSONArray();
        Iterator iterator = map.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry entry = (Map.Entry)iterator.next();
            if(((ArrayList)entry.getValue()).size() > 1){
                try {
                    ArrayList<ObjNode> objNodeArrayList = (ArrayList)entry.getValue();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("duplcateCount", objNodeArrayList.size());
                    jsonObject.put("bufferHash", entry.getKey());
                    jsonObject.put("width",
                            (int)HahaHelper.fieldValue(((ClassInstance)objNodeArrayList.get(0).getInstance()).getValues(), "mWidth"));
                    jsonObject.put("height",
                            (int)HahaHelper.fieldValue(((ClassInstance)objNodeArrayList.get(0).getInstance()).getValues(), "mHeight"));
                    JSONArray traceJsonArray = new JSONArray();
                    for (ObjNode objNode : objNodeArrayList){
                        traceJsonArray.put(objNode.getTraceString());
                    }
                    jsonObject.put("stacks", traceJsonArray);
                    jsonResult.put(jsonObject);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        System.out.printf("result: " + jsonResult.toString());
    }
}
