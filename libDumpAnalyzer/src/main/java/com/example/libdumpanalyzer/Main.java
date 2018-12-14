package com.example.libdumpanalyzer;

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
import java.util.Iterator;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("haha");
        try {
            anylyzerDump("/Users/jinlongfeng/Desktop/GeekPlay/Chapter04/heap.dump");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void anylyzerDump(String filePath) throws IOException {

        File heapDumpFile = new File(filePath);
        HprofBuffer hprofBuffer = new MemoryMappedFileBuffer(heapDumpFile);
        HprofParser parser = new HprofParser(hprofBuffer);
        Snapshot snapshot = parser.parse();

        ClassObj bitmapClass = snapshot.findClass("android.graphics.Bitmap");


        Iterator<Heap> iterator = snapshot.getHeaps().iterator();

        while (iterator.hasNext()) {

            Heap heap = iterator.next();
            String heapName =  heap.getName();

            // 从heap中获得所有的Bitmap实例
            List<Instance> bitmapInstances = bitmapClass.getHeapInstances(heap.getId());

            for (Instance instance : bitmapInstances){



               Heap heap1 =  instance.getHeap();


              String heap1Name =  heap1.getName();

//               ClassObj bytes = snapshot

//               ArrayInstance buffer = HahaHelper.fieldValue(((ClassInstance) bitmapInstance).getValues(), "mBuffer");

            }

            // 从Bitmap实例中获得buffer数组

//            ArrayInstance buffer = HahaHelper.fieldValue(((ClassInstance) bitmapInstance).getValues(), "mBuffer");

        }

    }
}
