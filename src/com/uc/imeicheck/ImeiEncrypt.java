package com.uc.imeicheck;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import android.annotation.SuppressLint;
import android.util.Base64;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;

public class ImeiEncrypt {

	    private byte[] desKey;

	    public ImeiEncrypt(String desKey) {
	        this.desKey = desKey.getBytes();
	    }

	    @SuppressLint("TrulyRandom") 
	    public byte[] desEncrypt(byte[] plainText) throws Exception {
	        SecureRandom sr = new SecureRandom();
	        byte rawKeyData[] = desKey;
	        DESKeySpec dks = new DESKeySpec(rawKeyData);
	        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
	        SecretKey key = keyFactory.generateSecret(dks);
	        Cipher cipher = Cipher.getInstance("DES");
	        cipher.init(Cipher.ENCRYPT_MODE, key, sr);
	        byte data[] = plainText;
	        byte encryptedData[] = cipher.doFinal(data);
	        return encryptedData;
	    }

	    public byte[] desDecrypt(byte[] encryptText) throws Exception {
	        SecureRandom sr = new SecureRandom();
	        byte rawKeyData[] = desKey;
	        DESKeySpec dks = new DESKeySpec(rawKeyData);
	        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
	        SecretKey key = keyFactory.generateSecret(dks);
	        Cipher cipher = Cipher.getInstance("DES");
	        cipher.init(Cipher.DECRYPT_MODE, key, sr);
	        byte encryptedData[] = encryptText;
	        byte decryptedData[] = cipher.doFinal(encryptedData);
	        return decryptedData;
	    }

	    public String encrypt(String input) throws Exception {
	        return base64Encode(desEncrypt(input.getBytes()));
	    }

	    public String decrypt(String input) throws Exception {
	        byte[] result = base64Decode(input);
	        return new String(desDecrypt(result));
	    }

	    public static String base64Encode(byte[] s) throws UnsupportedEncodingException {
	        if (s == null)
	            return null;
	        
	        return new String(Base64.encode(s, Base64.DEFAULT), "utf-8");
	    }

	    public static byte[] base64Decode(String s) throws IOException {
	        if (s == null)
	            return null;
	        
	        byte[] b = Base64.decode(s, Base64.DEFAULT);
	        return b;
	    }
	
}
