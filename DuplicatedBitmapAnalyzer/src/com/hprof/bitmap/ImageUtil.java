package com.hprof.bitmap;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public class ImageUtil {
    public static void byteToImage(Object[] buffers) {

        try {
            File image = new File(System.getProperty("user.dir")
                    +"\\DuplicatedBitmapAnalyzer\\res\\image"+System.currentTimeMillis()+".jpg");
            byte[] bytes = new byte[buffers.length];
            for (int i = 0; i < buffers.length; i++) {
                bytes[i] = (byte) buffers[i];
            }
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ImageIO.write(ImageIO.read(bais), "jpg",image);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
