package com.example.hprof;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Debug;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.example.hprof.util.HprofAnalysis;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bitmap bitmap1 = BitmapFactory.decodeResource(getResources(), R.drawable.down_faile);
        Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(), R.drawable.down_focus);
        Bitmap bitmap3 = BitmapFactory.decodeResource(getResources(), R.drawable.down_faile);
        Bitmap bitmap4 = BitmapFactory.decodeResource(getResources(), R.drawable.down_focus);

        ((ImageView) findViewById(R.id.image1)).setImageBitmap(bitmap1);
        ((ImageView) findViewById(R.id.image3)).setImageBitmap(bitmap2);
        ((ImageView) findViewById(R.id.image2)).setImageBitmap(bitmap3);
        ((ImageView) findViewById(R.id.image4)).setImageBitmap(bitmap4);

        findViewById(R.id.dump).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File file = new File(getExternalCacheDir(), "dump.hprof");
                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Debug.dumpHprofData(file.getAbsolutePath());
                            HprofAnalysis.analysis(file.getAbsolutePath());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }
}