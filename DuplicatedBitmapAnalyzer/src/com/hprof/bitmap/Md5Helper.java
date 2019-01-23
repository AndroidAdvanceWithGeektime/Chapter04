package com.hprof.bitmap;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by shengmingxu on 2018/12/11.
 */
public class Md5Helper {
    // 计算字节流的md5值
    public static String getMd5(byte[] bytes) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(bytes, 0, bytes.length);
        return byteArrayToHex(md5.digest()).toLowerCase();
    }

    private static String byteArrayToHex(byte[] byteArray) {
        char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        char[] resultCharArray = new char[byteArray.length * 2];
        int index = 0;
        for (byte b : byteArray) {
            resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];
            resultCharArray[index++] = hexDigits[b & 0xf];
        }
        return new String(resultCharArray);
    }

}
