package com.hprof.bitmap;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class Main {

    // 计数器
    public static Map<String, ReportBean> counter = new HashMap<>();

    public static void main(String[] args) throws IOException {

        String dumpFilePath = "";

        if (args.length != 1) {
            System.out.println(" java -jar DuplicatedBitmapAnalyzer-1.0.jar <PATH TO DumpFile> ");
            return;
        }

        if (args.length>0) {
            dumpFilePath = args[0];
        }

//        String proPth = System.getProperty("user.dir");
//
//        dumpFilePath = proPth + File.separator + "heap1.hprof";

        try {

            counter.clear();
            System.out.println("the program is running, please wait ........");
            anylyzerDump(dumpFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void anylyzerDump(String dumpfilePath) throws Exception {
        File heapDumpFile = new File(dumpfilePath);
        HprofBuffer hprofBuffer = new MemoryMappedFileBuffer(heapDumpFile);
        HprofParser parser = new HprofParser(hprofBuffer);
        Snapshot snapshot = parser.parse();

        String className = "android.graphics.Bitmap";
        ClassObj bitmapClass = snapshot.findClass(className);

        List<Instance> bitmapInstances = bitmapClass.getInstancesList();

        for (Instance instance : bitmapInstances) {
            Heap heap1 = instance.getHeap();
            String heap1Name = heap1.getName();

            if (!heap1Name.equalsIgnoreCase("app")) {
                continue;
            }

            ArrayInstance buffer = HahaHelper.fieldValue(((ClassInstance) instance).getValues(), "mBuffer");
            Integer height = HahaHelper.fieldValue(((ClassInstance) instance).getValues(), "mHeight");
            Integer width = HahaHelper.fieldValue(((ClassInstance) instance).getValues(), "mWidth");


            Class<?> personType = buffer.getClass();
            Method method = personType.getDeclaredMethod("asRawByteArray", int.class, int.class);
            method.setAccessible(true);
            byte[] data = (byte[]) method.invoke(buffer, 0, buffer.getValues().length);


            BASE64Encoder base64Encoder = new BASE64Encoder();
            String str = base64Encoder.encode(data);
            String hash = md5(str);

            // 不存在，则加入
            if (!counter.keySet().contains(hash)) {
                ReportBean reportBean = new ReportBean();
                reportBean.setBufferHash(hash);
                reportBean.setHeight(height);
                reportBean.setWidth(width);
                reportBean.setBufferSize(buffer.getSize());
                reportBean.setDuplcateCount(0);
                reportBean.setStacks(printStack1(instance, snapshot));
                counter.put(hash, reportBean);

            } else {
                // 存在
                ReportBean reportBean = counter.get(hash);
                reportBean.setDuplcateCount(reportBean.getDuplcateCount() + 1);


                // 保存图片
                String pngfilePathDir = heapDumpFile.getParent()
                        + File.separator + "images";
                File file = new File(pngfilePathDir);
                if (!file.exists()){
                    file.mkdir();
                }
                String pngfilePath = pngfilePathDir+ File.separator+ hash + ".png";

                ARGB8888_BitmapExtractor.getImage(width, height, data, pngfilePath);
            }

        }


        List<ReportBean> reportBeanList = new ArrayList<>();
        Iterator<ReportBean> iterator = counter.values().iterator();

        while (iterator.hasNext()) {
            ReportBean reportBean = iterator.next();
            if (reportBean.getDuplcateCount() > 0) {
                reportBeanList.add(reportBean);
            }
        }

        Collections.sort(reportBeanList, new Comparator<ReportBean>() {
            @Override
            public int compare(ReportBean reportBean, ReportBean t1) {
                return t1.getDuplcateCount() - reportBean.getDuplcateCount() > 0 ? 1 : -1;
            }
        });

        for (int i = 0; i < reportBeanList.size(); i++) {
            ReportBean reportBean = reportBeanList.get(i);
            System.out.println(reportBean.toString());
        }

    }


    /**
     * 获取 Instance 的 stack
     *
     * @param instance
     * @param snapshot
     * @return
     */
    public static String printStack1(Instance instance, Snapshot snapshot) {

        String stacks = "";


        ExcludedRefs NO_EXCLUDED_REFS = ExcludedRefs.builder().build();
        HeapAnalyzer heapAnalyzer = new HeapAnalyzer(NO_EXCLUDED_REFS, AnalyzerProgressListener.NONE,
                Collections.<Class<? extends Reachability.Inspector>>emptyList());

        Class<?> heapAnalyzerClass = heapAnalyzer.getClass();

        try {


            Method method = heapAnalyzerClass.getDeclaredMethod("findLeakTrace",
                    long.class,
                    Snapshot.class,
                    Instance.class,
                    boolean.class);

            method.setAccessible(true);

            long analysisStartNanoTime = System.nanoTime();

            AnalysisResult analysisResult = (AnalysisResult) method.invoke(heapAnalyzer,
                    analysisStartNanoTime,
                    snapshot,
                    instance,
                    false);


            String string = analysisResult.leakTrace.toString();

            stacks = string;

        } catch (Exception e) {

            System.out.println("Exception =" + e.getMessage());

        }

        return stacks;

    }

    /**
     * 计算 md5
     *
     * @param string
     * @return
     */
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


    /**
     * 根据 byte[] 保存图片到文件
     * 参考
     * https://github.com/JetBrains/adt-tools-base/blob/master/ddmlib/src/main/java/com/android/ddmlib/BitmapDecoder.java
     */
    private static class ARGB8888_BitmapExtractor {

        public static void getImage(int width, int height, byte[] rgba, String pngFilePath) throws IOException {
            BufferedImage bufferedImage = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_ARGB);

            for (int y = 0; y < height; y++) {
                int stride = y * width;
                for (int x = 0; x < width; x++) {
                    int i = (stride + x) * 4;
                    long rgb = 0;
                    rgb |= ((long) rgba[i] & 0xff) << 16; // r
                    rgb |= ((long) rgba[i + 1] & 0xff) << 8;  // g
                    rgb |= ((long) rgba[i + 2] & 0xff);       // b
                    rgb |= ((long) rgba[i + 3] & 0xff) << 24; // a
                    bufferedImage.setRGB(x, y, (int) (rgb & 0xffffffffl));
                }
            }
            File outputfile = new File(pngFilePath);
            ImageIO.write(bufferedImage, "png", outputfile);

        }
    }
}
