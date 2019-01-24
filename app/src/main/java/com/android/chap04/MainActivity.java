package com.android.chap04;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author zhoujun
 * @date 19-1-17
 * @email jun_zhou1@hnair.com
 */
public class MainActivity extends Activity {

    public final static String TAG = "hproftest";
    private static String NAME_DUMP = "hprof_dump.hprof";
    private String mPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initListener();
        File storageFile = this.getExternalFilesDir("");
        if(storageFile != null) {
            mPath = storageFile.getAbsolutePath() + File.separator + NAME_DUMP;
        }
    }

    private void initView() {
        ImageView iv01 = findViewById(R.id.iv_01);
        ImageView iv02 = findViewById(R.id.iv_02);
        ImageView iv03 = findViewById(R.id.iv_03);
        ImageView iv04 = findViewById(R.id.iv_04);

        Bitmap bitmap01 = BitmapFactory.decodeResource(getResources(), R.drawable.iv02);
        Bitmap bitmap02 = BitmapFactory.decodeResource(getResources(), R.drawable.iv02);
        Bitmap bitmap03 = BitmapFactory.decodeResource(getResources(), R.drawable.iv02);
//        Bitmap bitmap04 = BitmapFactory.decodeResource(getResources(), R.drawable.iv02);

        iv01.setImageBitmap(bitmap01);
        iv02.setImageBitmap(bitmap02);
        iv03.setImageBitmap(bitmap03);
//        iv04.setImageBitmap(bitmap04);
    }

    private void initListener() {
        Button btnDump = findViewById(R.id.btn_dump);
        Button btnAnalyze = findViewById(R.id.btn_analyze);
        btnDump.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "mPath = " + mPath);
                if(TextUtils.isEmpty(mPath)) {
                    return ;
                }
                try {
                    android.os.Debug.dumpHprofData(mPath);
                    Log.d(TAG, "create dumpHprofData is ok..");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG, "create dumpHprofData is failed......");
                }

            }
        });

        btnAnalyze.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "ready to analyze file path = " + mPath);
                if(TextUtils.isEmpty(mPath)) {
                    return ;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            HprofBuffer buffer = new MemoryMappedFileBuffer(new File(mPath));
                            HprofParser parser = new HprofParser(buffer);
                            Snapshot snapShot = parser.parse();
                            ClassObj bitmapCls = snapShot.findClass("android.graphics.Bitmap");
                            Collection<Heap> heapList = snapShot.getHeaps();
                            Log.d(TAG, "size = " + heapList.size());
                            Iterator<Heap> it = heapList.iterator();
                            while(it.hasNext()) {
                                Heap heap = it.next();
                                String name = heap.getName();
                                int id = heap.getId();
                                Log.d(TAG, "the itemId = " + id + "; name = " + name);
                                // 从heap中获得所有的bitmap实例, 然后只是分析app和default即可.
                                if("app".equals(name) || "default".equals(name)) {
                                    analyzeHeap(id, bitmapCls, snapShot);
                                }

                            }

                            Log.d(TAG, "==>>>>Ok. done..");

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

            }
        });
    }

    private void analyzeHeap(int id, ClassObj bitmapCls, Snapshot snapshot) {
        Map<Integer, ArrayList<Instance>> instanceMap = new HashMap<>();

        final List<Instance> bitmapInstances = bitmapCls.getHeapInstances(id);
        for(Instance instance : bitmapInstances) {
            Log.d(TAG, "bitmapInstance = " + instance.toString());
            // 从bitmap实例中获得buffer数组
            ArrayInstance buffer = HahaHelper.fieldValue(((ClassInstance)instance).getValues(), "mBuffer");
            if(buffer == null) {
                continue;
            }
            int hashCode = Arrays.hashCode(buffer.getValues());
            Log.d(TAG, "===>>>instance hashCode = " + hashCode);
            ArrayList<Instance> instanceList;
            if(instanceMap.containsKey(hashCode)) {
                instanceList = instanceMap.get(hashCode);
                instanceList.add(instance);
            } else {
                instanceList = new ArrayList<>();
                instanceList.add(instance);
            }
            instanceMap.put(hashCode, instanceList);
        }

        HeapAnalyzer heapAnalyzer = new HeapAnalyzer();
        for(int key : instanceMap.keySet()) {
            ArrayList<Instance> instanceList = instanceMap.get(key);
            // 假如有大于1个对象
            if(instanceList.size() > 1) {
                Integer height = HahaHelper.fieldValue(((ClassInstance)instanceList.get(0)).getValues(), "mHeight");
                Integer width = HahaHelper.fieldValue(((ClassInstance)instanceList.get(0)).getValues(),"mWidth");
                Log.e(TAG, "Duplicate pics = " + instanceList.size());
                Log.e(TAG, "hashcode = " + key);
                Log.e(TAG, "height = " + height);
                Log.e(TAG, "width = " + width);

                for(Instance instance : instanceList) {
                    LeakTrace leakTrace = heapAnalyzer.findLeakTrace(snapshot, instance);
                    Log.e(TAG, "引用链: " + leakTrace.toString());
                }
            }
        }

    }

}
