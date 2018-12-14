package com.example.imageapp;

import android.os.Debug;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        File heapDumpFile = new File(getFilesDir(),"heap.dump");

        try {
            Debug.dumpHprofData(heapDumpFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
