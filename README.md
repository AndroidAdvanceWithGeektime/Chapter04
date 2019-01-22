# Chapter04
终于迎来了第一个课后作业，今天我们尝试通过分析内存文件hprof快速判断内存中是否存在重复的图片，并且将这些重复图片的PNG、堆栈等信息输出。

hprof文件格式可以参考doc西面的manual.html文件

**需要注意的是需要使用8.0以下的机器，因为8.0以后Bitmap中的buffer已经放到native内存中了**

下面是一个结果的样例：
```
"duplcateCount":2,
"stacks":[
     [
         "static com.sample.launcher.controller.LauncherController mLauncherView(0x1349c400)",
         "com.sample.launcher.view.LauncherView mHeaderView(0x1349bc00)",
         "com.sample.homepage.header.HomePageHeaderView mSearchAndAddressBar(0x13118c00)",
         "android.widget.ImageView mDrawable(0x131f9400)",
         "android.graphics.drawable.BitmapDrawable mBitmapState(0x13799b80)",
         "android.graphics.drawable.BitmapDrawable$BitmapState mBitmap(0x13b0f4d8)",
         "android.graphics.Bitmap instance"
     ],
     [
         "static com.sample.resources.ResourceCache cachePool(0x12d32380)",
         "com.sample.resources.ResourceCache table(0x12c30250)",
         "array java.util.HashMap$HashMapEntry[] [130](0x14724000)",
         "java.util.LinkedHashMap$LinkedHashMapEntry after(0x130ed420)",
         "java.util.LinkedHashMap$LinkedHashMapEntry value(0x1361f780)",
         "com.sample.resources.ResourceCache$CacheSoftReference T(0x1361f760)",
         "android.graphics.drawable.BitmapDrawable mBitmapState(0x136a2d30)",
         "android.graphics.drawable.BitmapDrawable$BitmapState mBitmap(0x13971708)",
         "android.graphics.Bitmap instance"
     ]
],
"bufferHash":"4e2b2a183d1b48bcf2b12af45afdbc12",
"width":60,
"height":60,
"bufferSize":14400
```

bufferHash对应的重复图片：

![](doc/4e2b2a183d1b48bcf2b12af45afdbc12.png)


实现提示
====
hprof文件分析我们有两个实现方法：

1. 参考 Android Profiler 中[perflib](https://android.googlesource.com/platform/tools/base/+/studio-master-dev/perflib/src/main/java/com/android/tools/perflib)的分析源码，其中重复Bitmap的代码是[DuplicatedBitmapAnalyzerTask](https://android.googlesource.com/platform/tools/base/+/studio-master-dev/perflib/src/main/java/com/android/tools/perflib/heap/memoryanalyzer/DuplicatedBitmapAnalyzerTask.java)。
2. 我们也可以使用 sqaure 的[HAHA](https://github.com/square/haha)库，其中核心方法为：

```
// 打开hprof文件
final HeapSnapshot heapSnapshot = new HeapSnapshot(hprofFile);
// 获得snapshot
final Snapshot snapshot = heapSnapshot.getSnapshot();
// 获得Bitmap Class
final ClassObj bitmapClass = snapshot.findClass("android.graphics.Bitmap");
// 获得heap, 只需要分析app和default heap即可
Heap heap = snapshot.getHeaps();
// 从heap中获得所有的Bitmap实例
final List<Instance> bitmapInstances = bitmapClass.getHeapInstances(heap.getId());
// 从Bitmap实例中获得buffer数组
ArrayInstance buffer = HahaHelper.fieldValue(((ClassInstance) bitmapInstance).getValues(), "mBuffer");
```

整个思路就是通过"mBuffer"的hash值判断哪些Bitmap是重复的。

提交方法
====
整个提交方法如下：

1. 完善DuplicatedBitmapAnalyzer项目
2. 注明极客时间的账号 + 实现原理与心得体会
3. 发送pull request 到本repo

奖励
===
根据项目质量和提交pull request的时间，抽取部分同学送上经典书籍。

最终结果也会在极客时间和repo中公布，欢迎大家积极参与！