package com.example.sample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.hprof.bitmap.DuplicatedBitmapCheck;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
   private int count=0;
    private DuplicatedBitmapCheck mDuplicatedBitmapCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final ImageView iv1=findViewById(R.id.iv1);
        final ImageView iv2=findViewById(R.id.iv2);
        final ImageView iv3=findViewById(R.id.iv3);
        Button btn=findViewById(R.id.btn);
        Button btn2=findViewById(R.id.btn2);
        Bitmap bitmap1=BitmapFactory.decodeResource(getResources(),R.mipmap.dict);
        Bitmap bitmap2=BitmapFactory.decodeResource(getResources(),R.mipmap.dict);
        Bitmap bitmap3=BitmapFactory.decodeResource(getResources(),R.mipmap.dict);
        iv1.setImageBitmap(bitmap1);
        iv2.setImageBitmap(bitmap2);
        iv3.setImageBitmap(bitmap3);
        mDuplicatedBitmapCheck = DuplicatedBitmapCheck.getInstance();
        mDuplicatedBitmapCheck.setDuplicatedBitmapListener(new DuplicatedBitmapCheck.DuplicatedBitmapListener() {
            @Override
            public void bitmapInfo(String info) {
                System.out.println(info);
            }
        });
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file=new File(getExternalCacheDir(),"dump");
                if(!file.exists()){
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                mDuplicatedBitmapCheck.dump(file);

            }
        });
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDuplicatedBitmapCheck.analyze();
            }
        });
    }
}
