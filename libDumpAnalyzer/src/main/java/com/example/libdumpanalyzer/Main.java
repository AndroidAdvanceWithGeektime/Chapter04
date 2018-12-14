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
import com.sun.org.apache.xerces.internal.impl.xpath.XPath;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class Main {

   public static   Set<String> mStringSet = new TreeSet<>();


    public static void main(String[] args) {
        System.out.println("haha");
        try {
            mStringSet.clear();
            anylyzerDump("C:\\Users\\fengjl\\Desktop\\geekand\\Chapter04\\heap1.hprof");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void anylyzerDump(String filePath) throws IOException {

        File heapDumpFile = new File(filePath);
        HprofBuffer hprofBuffer = new MemoryMappedFileBuffer(heapDumpFile);
        HprofParser parser = new HprofParser(hprofBuffer);
        Snapshot snapshot = parser.parse();

        String className = "android.graphics.Bitmap";

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
            BASE64Encoder base64Encoder = new BASE64Encoder();
            String str = base64Encoder.encode(temp);

            String hash = md5(str);

            // 不存在，则加入
            if (!mStringSet.contains(hash)){
                mStringSet.add(hash);
            }else {
                // 存在则输出信息
                System.out.println("buffer hash = " + hash);
                generateImage(str,"C:\\Users\\fengjl\\Desktop\\geekand\\Chapter04\\heap" + index + ".png");
                index++;
            }
        }

    }


    public static String md5(String string) {

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


    public static void byte2image(byte[] data, String path) {
        if (data.length < 3 || path.equals("")) return;
        try {
            FileImageOutputStream imageOutput = new FileImageOutputStream(new File(path));
            imageOutput.write(data, 0, data.length);
            imageOutput.close();
            System.out.println("Make Picture success,Please find image in " + path);
        } catch (Exception ex) {
            System.out.println("Exception: " + ex);
            ex.printStackTrace();
        }
    }


    public static byte[] image2byte(String path) {
        byte[] data = null;
        FileImageInputStream input = null;
        try {
            input = new FileImageInputStream(new File(path));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int numBytesRead = 0;
            while ((numBytesRead = input.read(buf)) != -1) {
                output.write(buf, 0, numBytesRead);
            }
            data = output.toByteArray();
            output.close();
            input.close();
        } catch (FileNotFoundException ex1) {
            ex1.printStackTrace();
        } catch (IOException ex1) {
            ex1.printStackTrace();
        }
        return data;
    }

    public static boolean generateImage(String data,String filePath){
        BASE64Decoder decoder = new BASE64Decoder();
        try{
            byte[] bytes = decoder.decodeBuffer(data);
            for (int i = 0; i < bytes.length ; i++) {
                if (bytes[i] < 0){
                    bytes[i] += 256;
                }
            }
            OutputStream outputStream = new FileOutputStream(filePath);
            outputStream.write(bytes);
            outputStream.flush();
            outputStream.close();
            return true;
        }catch (Exception e){

            return false;
        }

    }

}
