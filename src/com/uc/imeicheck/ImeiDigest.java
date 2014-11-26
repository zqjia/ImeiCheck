package com.uc.imeicheck;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.util.Log;

public class ImeiDigest {	
	
	private static final String TAG = ImeiDigest.class.getName();
	
	public static String computeSign(Map<String, String> map, String clientId, 
					String clientSecret, String nonce){
		List<String> keys = new ArrayList<String>();
        for (String key :map.keySet()) {
            keys.add(key);
        }
        
        //  key 按升序排列
        Collections.sort(keys);
        List<String> items = new ArrayList<String>();
        
        for (String key : keys) {
            items.add(key + "=" + map.get(key));
        }
        
        StringBuilder sb = new StringBuilder();
        
        for (String item : items) {
            sb.append(item);
        }
        String content = sb.toString();

        // 验证摘要，算法：md5(APPID+SECRET_KEY+REQ_ID+签名内容)
        String text = clientId + clientSecret + nonce + content;
        if(DebugUtil.isDebug){
        	 Log.d(TAG, nonce);
             Log.d(TAG, "text  "+text);
        }
 
        Locale loc = Locale.getDefault();
        return md5(text).toLowerCase(loc);			
	}
	
    public static String md5(String raw){
       
    	MessageDigest messageDigest = getMD5MessageDigest();
    	if(messageDigest == null){
            return null;
        }
        // Encryption algorithm
    	messageDigest.update(raw.getBytes(), 0, raw.length());
        String md5 = new BigInteger(1, messageDigest.digest()).toString(16);
        while ( md5.length() < 32 ) {
            md5 = "0"+md5;
        }
        
        return md5;
    }
    
    private static MessageDigest getMD5MessageDigest()
    {
        try {
            return MessageDigest.getInstance("MD5");
        } 
        catch (NoSuchAlgorithmException e) 
        {
            e.printStackTrace();
            if(DebugUtil.isDebug){
            	 Log.e(TAG, "Exception while getting digest", e);
            }     
            return null;
        }
    }
}
