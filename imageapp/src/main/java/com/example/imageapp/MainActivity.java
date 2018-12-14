package com.example.imageapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Debug;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class MainActivity extends AppCompatActivity {

    private List<Bitmap> mBitmapList;

    String dumpFilePath;
    String baseDir ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBitmapList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            mBitmapList.add(bitmap);
        }

        baseDir = getFilesDir().getAbsolutePath();

        File heapDumpFile = new File(getFilesDir(), "heap1.dump");
        dumpFilePath = heapDumpFile.getAbsolutePath();

        try {
            Debug.dumpHprofData(dumpFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void onAnaylzar(View view) {

        new DumpParseThread().start();

    }

    private class DumpParseThread extends Thread {

        public Set<String> mStringSet = new TreeSet<>();

        @Override
        public void run() {
            super.run();
            mStringSet.clear();
            try {
                anylyzerDump(dumpFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        private void anylyzerDump(String filePath) throws IOException {

            File heapDumpFile = new File(filePath);
            HprofBuffer hprofBuffer = new MemoryMappedFileBuffer(heapDumpFile);
            HprofParser parser = new HprofParser(hprofBuffer);
            Snapshot snapshot = parser.parse();

            String className = "android.graphics.Bitmap";
            className = Bitmap.class.getName();

            ClassObj bitmapClass = snapshot.findClass(className);

            Heap heap = snapshot.getHeap("app");

            List<Instance> bitmapInstances = bitmapClass.getInstancesList();

            int index = 0;
            for (Instance instance : bitmapInstances) {
                Heap heap1 = instance.getHeap();
                String heap1Name = heap1.getName();

                if (!heap1Name.equalsIgnoreCase("app")) {
                    continue;
                }
                ArrayInstance buffer = HahaHelper.fieldValue(((ClassInstance) instance).getValues(), "mBuffer");
                Integer height = HahaHelper.fieldValue(((ClassInstance) instance).getValues(), "mHeight");
                Integer width = HahaHelper.fieldValue(((ClassInstance) instance).getValues(), "mWidth");

                System.out.println("height =" + height);
                System.out.println("width =" + width);
                Object[] data = buffer.getValues();
                byte[] temp = new byte[data.length];
                for (int i = 0; i < data.length; i++) {
                    temp[i] = (byte) data[i];
                }

                String str = Base64.encodeToString(temp, Base64.DEFAULT);
                String hash = md5(str);
                // 不存在，则加入
                if (!mStringSet.contains(hash)) {
                    mStringSet.add(hash);
                } else {
                    // 存在则输出信息
                    System.out.println("buffer hash = " + hash);

                    String pngFilePath = baseDir + File.separator + index + ".png";

                    FileOutputStream fileOutputStream = new FileOutputStream(pngFilePath);

                    Bitmap bitmap = BitmapFactory.decodeByteArray(temp,0,temp.length);

                    if (bitmap!=null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 80, fileOutputStream);

                        index++;
                    }
                }
            }

        }

        public String md5(String string) {

            MessageDigest md5 = null;
            try {
                md5 = MessageDigest.getInstance("MD5");
                byte[] bytes = md5.digest(string.getBytes());
                StringBuilder result = new StringBuilder();
                for (byte b : bytes) {
                    String temp = Integer.toHexString(b & 0xff);
                    if (temp.length() == 1) {
                        temp = "0" + temp;
                    }
                    result.append(temp);
                }
                return result.toString();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return "";
        }

        public boolean generateImage(String data, String filePath) {

            try {
                byte[] bytes = Base64.decode(data, Base64.DEFAULT);
                for (int i = 0; i < bytes.length; i++) {
                    if (bytes[i] < 0) {
                        bytes[i] += 256;
                    }
                }
                OutputStream outputStream = new FileOutputStream(filePath);
                outputStream.write(bytes);
                outputStream.flush();
                outputStream.close();
                return true;
            } catch (Exception e) {

                return false;
            }
        }
    }
}
