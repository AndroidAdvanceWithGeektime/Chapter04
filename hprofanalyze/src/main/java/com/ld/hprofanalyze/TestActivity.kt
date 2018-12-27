package com.ld.hprofanalyze

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Debug
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.squareup.haha.perflib.io.MemoryMappedFileBuffer
import kotlinx.android.synthetic.main.activity_test.*
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import android.util.Log
import com.squareup.haha.perflib.*
import java.lang.StringBuilder
import java.lang.reflect.Array


private const val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 100
private const val TAG = "TestActivity"

class TestActivity : AppCompatActivity() {
    private var externalReportPath: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE)
        } else {
            initExternalReportPath()
            afterPermissionGrant()

        }

    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: kotlin.Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        initExternalReportPath()
        afterPermissionGrant()
    }

    private fun initExternalReportPath() {
        externalReportPath = File(Environment.getExternalStorageDirectory(), "hprofdir")
        if (externalReportPath?.exists() ?: false) {
            externalReportPath?.mkdirs()
        }

    }

    private fun afterPermissionGrant() {
//        1、构造内存泄漏（重复加载同一张图片）
        var bitmap1 = BitmapFactory.decodeResource(resources, R.drawable.test)
        image1.setImageBitmap(bitmap1)
        var bitmap2 = BitmapFactory.decodeResource(resources, R.drawable.test)
        image2.setImageBitmap(bitmap2)
        var bitmap3 = BitmapFactory.decodeResource(resources, R.drawable.test)
        image3.setImageBitmap(bitmap3)
        var bitmap4 = BitmapFactory.decodeResource(resources, R.drawable.test)
        image4.setImageBitmap(bitmap4)
        dump.setOnClickListener {
            Debug.dumpHprofData(externalReportPath?.absolutePath)
        }
        parse.setOnClickListener {
            //            2、Haha库使用，生成内存快照。
            var memoryMappedFileBuffer = MemoryMappedFileBuffer(externalReportPath)
            var hprofParser = HprofParser(memoryMappedFileBuffer)
            var snapshot = hprofParser.parse()
            snapshot.computeDominators()
            var heaps = snapshot.heaps
            heaps = heaps.filter {
                it.name.equals("app") || it.name.equals("default")
            }
//            3、获取bitmap相关的对象的内存快照。
            var bitmapClass = snapshot.findClass("android.graphics.Bitmap")
            var bitmaplist = arrayListOf<Instance>()
            heaps.forEach {
                bitmaplist.addAll(bitmapClass.getHeapInstances(it.id))
            }
            var bitmapMapToBuffer = mutableMapOf<Instance, ArrayInstance>()
//            4、构建map   key：bitmap内存映射对象，value：bitmap Class 的mbuffer字段值
            bitmaplist.forEach {
                (it as ClassInstance).values.forEach { buffer ->
                    if (buffer.field.name.equals("mBuffer")) {
                        bitmapMapToBuffer[it] = buffer.value as ArrayInstance
                    }
                }
            }
//            5、构建map key:步骤4中的mbuffer字段的数组的hashcode,value:拥有相同hashcode的Instance 集合。
            var result = mutableMapOf<Int, ArrayList<Instance>>()
            bitmapMapToBuffer.forEach { instance, byteBuffer ->
                var hashCode = Arrays.hashCode(byteBuffer.values)
                if (result.containsKey(hashCode)) {
                    result[hashCode]?.add(instance)
                } else {
                    result[hashCode] = arrayListOf(instance)
                }
            }
//            6、过滤步骤5中map，如果value（list）的size大于1，就表示有多个instance的mbuffer是一样的，就可以被认为是内存泄漏了。
            var filterResult = result.filter {
                it.component2().size > 1
            }
            Log.d(TAG, "发现重复bitmap 数量为 ${filterResult.size}")
//             7、获取bitmap的宽高和buffer长度
            filterResult.forEach { hashcode, instances ->
                var leak = LeakObjectDetaial()
                instances.forEach {
                    (it as ClassInstance).values.forEach { fieldValue ->
                        leak.leakCount++
                        when (fieldValue.field.name) {
                            "mWidth" -> leak.width = fieldValue.value as Int
                            "mHeight" -> leak.height = fieldValue.value as Int
                            "mBuffer" -> leak.bufferSize = (fieldValue.value as ArrayInstance).values.size
                        }
                    }
//                    8、获取bitmap的引用链。
                    var stackFrame = StringBuilder()
                    stackFrame.append(it.classObj.className).append("\n")
                    var nextInstanceToGcRoot = it.nextInstanceToGcRoot
                    while (nextInstanceToGcRoot != null) {
                        stackFrame.insert(0, nextInstanceToGcRoot.classObj.className + "\n")
                        nextInstanceToGcRoot = nextInstanceToGcRoot.nextInstanceToGcRoot
                    }
                    leak.stacks = stackFrame.toString()
                    Log.d(TAG, "width:${leak.width},height:${leak.height},buffersize:${leak.bufferSize},stack:${leak.stacks}")
                    /*var stackField = it::class.java.superclass.getDeclaredField("mStack")
                    stackField.isAccessible = true
                    var stackValue: StackTrace = stackField.get(it) as StackTrace
                    var stackFrameField = stackValue::class.java.getField("mFrames")
                    stackFrameField.isAccessible = true
                    var stackFrameArray = stackFrameField.get(stackValue)
                    var stackLength = Array.getLength(stackFrameArray)
*/
                }
            }
        }
    }

    class LeakObjectDetaial {
        var leakCount: Int = 0
        var stacks: String = ""
        var width: Int = 0
        var height: Int = 0
        var bufferSize: Int = 0
    }

}
