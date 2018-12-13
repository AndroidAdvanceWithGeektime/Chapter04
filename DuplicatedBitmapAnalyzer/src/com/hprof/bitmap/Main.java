package com.hprof.bitmap;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) throws IOException {
        File heapDumpFile = getFileInLocal();

        //打开hprof文件
        HprofBuffer buffer = new MemoryMappedFileBuffer(heapDumpFile);
        HprofParser parser = new HprofParser(buffer);
        //解析获得快照
        Snapshot snapshot = parser.parse();
        snapshot.computeDominators();

        print(Bitmap.class.getName());
        //获得Bitmap Class
        Collection<ClassObj> bitmapClasses = snapshot.findClasses(Bitmap.class.getName());
        //获取堆数据,这里包括项目app、系统、default heap的信息，需要进行过滤
        Collection<Heap> heaps = snapshot.getHeaps();
        print("bitmapClasses size = " + bitmapClasses.size());
        print("all heaps size in snapshot = " + heaps.size());

        List<ArrayInstance> instanceList = new ArrayList<>();
        //这里有一个坑,其实snapshot也是从每个heap上获取他的ClassObj列表的,但是可能出现这个heap上的
        //ClassObj对象出现在了另一个heap中的情况,因此我们不能直接获取heap的ClassObj列表,
        //需要直接从snapshot总获取ClassObj列表.
        computerDurationTime(ComputerState.START);
        for (Heap heap : heaps) {
            //只关注app的heap
            if (!heap.getName().equals("app")) {
                continue;
            }
            for (ClassObj clazz : bitmapClasses) {
                //从heap中获得所有的Bitmap实例
                List<Instance> instances = clazz.getHeapInstances(heap.getId());
                print("instances size of class by heapId = " + instances.size());

                for (int i = 0; i < instances.size(); i++) {
                    //从GcRoot开始遍历搜索，Integer.MAX_VALUE代表无法被搜索到，说明对象没被引用可以被回收
                    if (instances.get(i).getDistanceToGcRoot() == Integer.MAX_VALUE) {
                        continue;
                    }
                    int curHashCode = getHashCodeByInstance(instances.get(i));
                    for (int j = i + 1; j < instances.size(); j++) {
                        int nextHashCode = getHashCodeByInstance(instances.get(j));
                        if (curHashCode == nextHashCode) {
                            print("* stacks info");
                            AnalyzerResult result = getAnalyzerResult(instances.get(i));
                            print(result.toString());
                            getStackInfo(instances.get(i));
                            print("=======================================================================");
                            if (i == instances.size() - 2 && j == instances.size() - 1) {
                                print("* stacks info");
                                result = getAnalyzerResult(instances.get(i));
                                print(result.toString());
                                getStackInfo(instances.get(j));
                                print("=======================================================================");
                            }
                            break;
                        }
                    }
                }
            }
        }
        computerDurationTime(ComputerState.END);
    }


    private void getStackInfo(Instance instance) {
        if (instance.getDistanceToGcRoot() != 0 && instance.getDistanceToGcRoot() != Integer.MAX_VALUE) {
            getStackInfo(instance.getNextInstanceToGcRoot());
        }
        if (instance.getNextInstanceToGcRoot() != null) {
            print("" + instance.getNextInstanceToGcRoot());
        }
    }

    private AnalyzerResult getAnalyzerResult(Instance instance) {
        List<ClassInstance.FieldValue> classInstanceValues = ((ClassInstance) instance).getValues();
        ArrayInstance bitmapBuffer = fieldValue(classInstanceValues, "mBuffer");
        int bitmapHeight = fieldValue(classInstanceValues, "mHeight");
        int bitmapWidth = fieldValue(classInstanceValues, "mWidth");
        AnalyzerResult result = new AnalyzerResult();
        result.setHashCode(Arrays.hashCode(bitmapBuffer.getValues()));
        result.setClassInstance(bitmapBuffer.toString());
        result.setBufferSize(bitmapBuffer.getValues().length);
        result.setWidth(bitmapWidth);
        result.setHeight(bitmapHeight);
        return result;
    }

    private int getHashCodeByInstance(Instance instance) {
        List<ClassInstance.FieldValue> classInstanceValues = ((ClassInstance) instance).getValues();
        ArrayInstance curBitmapBuffer = fieldValue(classInstanceValues, "mBuffer");
        return Arrays.hashCode(curBitmapBuffer.getValues());
    }

    class AnalyzerResult {
        int hashCode;
        String classInstance;
        int width;
        int height;
        int bufferSize;

        @Override
        public String toString() {
            return "bufferHashCode:" + this.hashCode + "\n"
                    + "width:" + this.width + "\n"
                    + "height:" + this.height + "\n"
                    + "bufferSize:" + this.bufferSize;
        }

        public int getBufferSize() {
            return bufferSize;
        }

        public void setBufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
        }

        public int getHashCode() {
            return hashCode;
        }

        public void setHashCode(int hashCode) {
            this.hashCode = hashCode;
        }

        public String getClassInstance() {
            return classInstance;
        }

        public void setClassInstance(String classInstance) {
            this.classInstance = classInstance;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }
    }


    private <T> T fieldValue(List<ClassInstance.FieldValue> values, String fieldName) {
        for (ClassInstance.FieldValue fieldValue : values) {
            if (fieldValue.getField().getName().equals(fieldName)) {
                return (T) fieldValue.getValue();
            }
        }
        throw new IllegalArgumentException("Field " + fieldName + " does not exists");
    }

    private File getFileInLocal() {
        //android profiler导出的hprof文件
        File file = new File("D://origin.hprof");
        return file;
    }

    private static void print(String content) {
        System.out.println(content);
    }

    enum ComputerState {
        START, END;
    }

    static long startTime;
    static long endTime;

    static private void computerDurationTime(ComputerState state) {
        if (ComputerState.START == state) {
            startTime = System.currentTimeMillis();
            print("记录开始时间 -------- " + startTime);
        } else {
            endTime = System.currentTimeMillis();
            print("记录结束时间 -------- " + endTime);
            if (endTime < startTime) {
                print("计算异常......");
                return;
            }
            print("处理耗时时长(MS) -------- " + (endTime - startTime));
        }
    }
}
