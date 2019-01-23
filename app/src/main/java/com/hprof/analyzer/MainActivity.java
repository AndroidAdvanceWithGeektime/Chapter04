package com.hprof.analyzer;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
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
import com.squareup.leakcanary.HahaHelper;
import com.squareup.leakcanary.HeapAnalyzer;
import com.squareup.leakcanary.LeakTrace;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

	private static final String TAG = "MainActivity";

	public static final String SD_PATH = Environment
			.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/";

	public static final String DUMP_PATH = SD_PATH + "com.hprof.analyzer/file/";

	public static final String DUMP_NAME = "dump";

	/**
	 * 读写权限请求码
	 */
	private static final int REQUEST_EXTERNAL_STORAGE = 1;

	/**
	 * 读写权限
	 */
	private static String[] PERMISSIONS_STORAGE = {
			"android.permission.READ_EXTERNAL_STORAGE",
			"android.permission.WRITE_EXTERNAL_STORAGE"};

	private Button btnRequestBitmap;

	private ImageView iv1;
	private ImageView iv2;
	private ImageView iv3;

	private Bitmap bitmap1;
	private Bitmap bitmap2;
	private Bitmap bitmap3;
	private Bitmap bitmap4;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// 绑定控件
		initView();
		// 重复图片
		duplcateBitmap();
		// 权限请求
		requestPermissions();
	}

	/**
	 * 开始分析
	 */
	private void startAnalyze() {

		Snapshot snapshot = createDump();

		// 获得Bitmap Class
		final ClassObj bitmapClass = snapshot.findClass(Bitmap.class.getName());

		// 获得heap, 只需要分析app和default heap即可
		Heap appHeap = snapshot.getHeap("app");
		Heap defaultHeap = snapshot.getHeap("default");

		// 分析heap
		analzHeap(bitmapClass, appHeap, snapshot);
		analzHeap(bitmapClass, defaultHeap, snapshot);

	}

	/**
	 * 请求读写权限
	 */
	private void requestPermissions() {

		try {
			int permission = ActivityCompat.checkSelfPermission(this,
					"android.permission.WRITE_EXTERNAL_STORAGE");
			if (permission != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
			}
		} catch (Exception e) {
			Log.e(TAG, "verifyStoragePermissions: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void initView() {
		iv1 = (ImageView) findViewById(R.id.iv1);
		iv2 = (ImageView) findViewById(R.id.iv2);
		iv3 = (ImageView) findViewById(R.id.iv3);
		btnRequestBitmap = (Button) findViewById(R.id.btnRequestBitmap);
		btnRequestBitmap.setOnClickListener(this);
	}

	private void analzHeap(ClassObj bitmapClass, Heap heap, Snapshot snapshot) {

		Map<Integer, ArrayList<Instance>> instanceMap = new HashMap<>();

		// 从heap中获得所有的Bitmap实例
		final List<Instance> bitmapInstances = bitmapClass.getHeapInstances(heap.getId());
		// 从Bitmap实例中获得buffer数组
		for (int i = 0; i < bitmapInstances.size(); i++) {

			Instance instance = bitmapInstances.get(i);
			ArrayInstance buffer = HahaHelper.fieldValue(((ClassInstance) instance).getValues(), "mBuffer");

			int hashCode = Arrays.hashCode(buffer.getValues());

			ArrayList<Instance> instanceList;
			if (instanceMap.containsKey(hashCode)) {
				instanceList = instanceMap.get(hashCode);
				instanceList.add(instance);
			} else {
				instanceList = new ArrayList<>();
				instanceList.add(instance);
			}
			instanceMap.put(hashCode, instanceList);

		}

		HeapAnalyzer heapAnalyzer = new HeapAnalyzer();

		for (int key : instanceMap.keySet()) {

			ArrayList<Instance> instanceList = instanceMap.get(key);

			if (instanceList.size() > 1) {

				Integer height = HahaHelper.fieldValue(((ClassInstance) instanceList.get(0)).getValues(), "mHeight");
				Integer width = HahaHelper.fieldValue(((ClassInstance) instanceList.get(0)).getValues(), "mWidth");

				Log.e(TAG, "图片重复个数 = " + instanceList.size());
				Log.e(TAG, "hashcode: " + key);
				Log.e(TAG, "height: " + height);
				Log.e(TAG, "width: " + width);

				for (Instance instance : instanceList) {
					LeakTrace leakTrace = heapAnalyzer.findLeakTrace(snapshot, instance);
					Log.e(TAG, "引用链: " + leakTrace.toString());
				}

			}

		}

	}


	/**
	 * 生成dump文件
	 * <p>
	 * 这里属于测试方法，dump文件只会生成一次，下次不会生成
	 */
	public static Snapshot createDump() {

		File filedir = new File(DUMP_PATH);
		if (!filedir.exists()) {
			filedir.mkdirs();
		}

		File file = new File(filedir, DUMP_NAME);
		Snapshot snapshot = null;
		try {
			if (!file.exists()) Debug.dumpHprofData(file.getAbsolutePath());
			HprofBuffer buffer = new MemoryMappedFileBuffer(file);
			HprofParser parser = new HprofParser(buffer);
			// 获得snapshot
			snapshot = parser.parse();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return snapshot;

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btnRequestBitmap:
				startAnalyze();
				break;
		}
	}

	// 重复图片
	private void duplcateBitmap() {

		bitmap1 = BitmapFactory.decodeResource(getResources(), R.mipmap.bitmap);
		bitmap2 = BitmapFactory.decodeResource(getResources(), R.mipmap.bitmap);
		bitmap3 = BitmapFactory.decodeResource(getResources(), R.mipmap.bitmap);
		bitmap4 = BitmapFactory.decodeResource(getResources(), R.mipmap.bitmap);
		iv1.setImageBitmap(bitmap1);
		iv2.setImageBitmap(bitmap2);
		iv3.setImageBitmap(bitmap3);
	}

}
