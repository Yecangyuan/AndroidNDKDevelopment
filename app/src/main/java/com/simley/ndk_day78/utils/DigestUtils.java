package com.simley.ndk_day78.utils;

import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public final class DigestUtils {

    private static final String TAG = DigestUtils.class.getSimpleName();

    private static final char[] hexCode = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    /**
     * MD5 加密
     *
     * @param input
     * @return
     */
    public static String md5(String input) {
        byte[] bytes = new byte[0];
        try {
            bytes = MessageDigest.getInstance("MD5").digest(input.getBytes());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "md5: " + e);
        }
        return convertToHexBinary(bytes);
    }

    public static String convertToHexBinary(byte[] data) {
        StringBuilder r = new StringBuilder(data.length * 2);
        for (byte b : data) {
            r.append(hexCode[(b >> 4) & 0xF]);
            r.append(hexCode[(b & 0xF)]);
        }
        return r.toString();
    }


    /**
     * RSA私钥解密
     *
     * @param msg    加密字符串
     * @param privateKey 私钥
     * @return铭文
     */

    public static String decrypt(String msg, String privateKey) {
        try {
            if (msg.contains(" ")) {
                msg = msg.replaceAll(" ", "+");
            }
            //base64编码的私钥
            final byte[] decoded = Base64.decode(privateKey, Base64.DEFAULT);

            //final byte[] inputByte = Base64.decodeBase64(message.getBytes(StandardCharsets.UTF_8))

            //decodeBase64(privateKey);
            RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, priKey);
            //64位解码加密后的字符串
            final byte[] inputByte = Base64.decode(msg.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);

            //  decodeBase64(message.getBytes(StandardCharsets.UTF_8));
            //密文
            final int len = inputByte.length;
            //偏移量
            int offset = 0;
            //段数
            int i = 0;
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while (len - offset > 0) {
                byte[] cache;
                if
                (len - offset > 128) {
                    cache = cipher.doFinal(inputByte, offset, 128);
                } else {
                    cache = cipher.doFinal(inputByte, offset, len - offset);
                }
                bos.write(cache);
                i++;
                offset = 128 * i;
            }
            bos.close();
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } catch (InvalidKeyException | InvalidKeySpecException | NoSuchAlgorithmException |
                 NoSuchPaddingException
                 | IllegalBlockSizeException | BadPaddingException | IOException e) {
            Log.e(TAG, String.format("decrypt: 数据解密异常 , 原始数据： %s，密钥： %s，e: %s ", msg, privateKey, e));
        }
        return null;
    }


    /**
     * RSA私钥解密
     *
     * @param message    加密字符串
     * @param privateKey 私钥
     * @return 铭文
     */

    public static String decrypt2(String message, String privateKey) {
        try {
            if (message.contains(" ")) {
                message = message.replaceAll(" ", "+");
            }
            //base64编码的私钥
            final byte[] decoded = Base64.decode(privateKey.getBytes(StandardCharsets.UTF_8), 0);


            //  final byte[] decoded = Base64Utils.decode(privateKey);
            RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, priKey);
            //64位解码加密后的字符串
            final byte[] inputByte = Base64.decode(message.getBytes(StandardCharsets.UTF_8), 0);
            //密文
            final int len = inputByte.length;
            //偏移量
            int offset = 0;
            //段数
            int i = 0;
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while (len - offset > 0) {
                byte[] cache;
                if (len - offset > 128) {
                    cache = cipher.doFinal(inputByte, offset, 128);
                } else {
                    cache = cipher.doFinal(inputByte, offset, len - offset);
                }
                bos.write(cache);
                i++;
                offset = 128 * i;
            }
            bos.close();

            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } catch (InvalidKeyException | InvalidKeySpecException | NoSuchAlgorithmException
                 | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException |
                 IOException e) {
            Log.e(TAG, String.format("decrypt: 数据解密异常 , 原始数据： %s，密钥： %s，e: %s ", message, privateKey, e));
        }
        return null;
    }


}
