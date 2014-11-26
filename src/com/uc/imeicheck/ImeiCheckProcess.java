package com.uc.imeicheck;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.Log;

public class ImeiCheckProcess {

    private static final String TAG = "ImeiCheckThread";
    
    private final static int CHECK_FAIL = 0;
    private final static int CHECK_SUCCESS = 1;
    private final static int CKECK_SERVER_ERROR = -1;
    private final static int NETWORK_SOCKETTIMEOUT = -2;
    private final static int NETWORK_CONNECTTIMEOUT = -3;
        
    private String SEPARATOR = "||";
    
    private final int SOCKET_TIMEOUT_WIFI = 10 * 1000;  //定义socket超时时间10s
    private final int CONNECT_TIMEOUT_WIFI = 5 * 1000;    //定义连接超时 5s
    
    private final int SOCKET_TIMEOUT_MOBILE = 20 * 1000;
    private final int CONNECT_TIMEOUT_MOBILE = 10 * 1000; 
       
    //hardcode
    private final String ClIENT_ID = "2";
    private final String CLIENT_SECRET = "abcdefgh";    
    private final String SERVER_URL = "http://10.1.45.25";
    private final String DES_KEY = "abcdefgh";   
    private final String MOBILE_PROXY_IP = "10.0.0.172";
    private final String TELECOM_PROXY_IP = "10.0.0.200";
    private final int PROXY_PORT = 80;
    

    
    private ImeiData mCheckData;
    private Handler mHandler;
    private Context mContext;
    
    public ImeiCheckProcess(ImeiData imeiData, Context context, Handler handler){
        this.mCheckData = imeiData;
        this.mHandler = handler;
        this.mContext = context;
    }
    
    private String changeInputStream(InputStream inputStream, String encode){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int len = 0;
        String result="";
        if (inputStream != null) {
            try {
                while ((len = inputStream.read(data)) != -1) {
                    outputStream.write(data,0,len);
                }
                result=new String(outputStream.toByteArray(), encode);
            } catch (IOException e){
                e.printStackTrace();
            }

        } 
        return result;
    }
    
    private String getNetType(){
        ConnectivityManager cm = (ConnectivityManager)this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm == null)
            return null;
        
        NetworkInfo info = cm.getActiveNetworkInfo();
        if(info == null)
            return null;
        
        return info.getTypeName();
        
    }
    
    private String getExtraInfo() {
        ConnectivityManager cm = (ConnectivityManager)this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm == null)
            return null;
        
        NetworkInfo info = cm.getActiveNetworkInfo();
        if(info == null)
            return null;
        
        return info.getExtraInfo();
        
    }
    
    private static HttpRequestRetryHandler requestRetryHandler = new HttpRequestRetryHandler() {
         // 自定义的恢复策略
         public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
             // 设置恢复策略，在Http请求发生异常时候将自动重试3次
             if (executionCount >= 3) {
                 // Do not retry if over max retry count
                 return false;
             }      
             if (exception instanceof NoHttpResponseException) {
                 // Retry if the server dropped connection on us
                 return true;
             }
             if (exception instanceof UnknownHostException) {
                 // Unknown host
                 return false;
             }
             
             if (exception instanceof SSLHandshakeException) {
                 // Do not retry on SSL handshake exception
                 return false;
             }
             
             HttpRequest request = (HttpRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
             boolean idempotent = (request instanceof HttpEntityEnclosingRequest);
             if (!idempotent) {
                 // Retry if the request is considered idempotent
                 return true;
             }
             return false;
         }
     };
    
    
    
    private void doImeiCheck(){
        //构建content内容，全部为小写             
        String platform = null, product = null, ver = null, subver = null, imei = null, content = null;
        Locale loc = Locale.getDefault();
        platform = this.mCheckData.getPlatform().toLowerCase(loc);
        product = this.mCheckData.getProduct().toLowerCase(loc);
        ver = this.mCheckData.getVer().toLowerCase(loc);
        subver = this.mCheckData.getSubver().toLowerCase(loc);
        imei = this.mCheckData.getImei().toLowerCase(loc);
        
        //所有字段都不为空
        if(platform != null && product != null && ver != null
                && subver != null && imei != null){
            content = platform + SEPARATOR 
                    + product + SEPARATOR
                    + ver + SEPARATOR
                    + subver + SEPARATOR
                    + imei + SEPARATOR;
        }else{
            if(DebugUtil.isDebug){
                Log.e(TAG, "params is not right");
                this.mHandler.sendEmptyMessage(CHECK_FAIL);
                return ;
            }
        }
            
        if(DebugUtil.isDebug){
            Log.d(TAG, "content :" + content);
        }
        
        
        //进行des加密
        String desContent = null;               
        ImeiEncrypt des = new ImeiEncrypt(DES_KEY);
        try {
            desContent = des.encrypt(content);
        } catch (Exception e1) {
            e1.printStackTrace();
            Log.e(TAG, "des fail");
            this.mHandler.sendEmptyMessage(CHECK_FAIL);
            return;         
        }
        
        if(DebugUtil.isDebug){
            Log.d(TAG, "des content :" + desContent);
        }
        
        
        //md5校验，得到摘要
        Map<String, String> paramMap = new HashMap<String, String>();
        String nonce = UUID.randomUUID().toString();    
        paramMap.put("content", desContent);
        paramMap.put("nonce", nonce);
        paramMap.put("clientId",ClIENT_ID);
            
        String sign = ImeiDigest.computeSign(paramMap, ClIENT_ID, CLIENT_SECRET, nonce);
        
        //构建http发送的content
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("content=").append(URLEncoder.encode(desContent, "utf-8")).append("&")
                .append("signature=").append(sign).append("&")
                .append("nonce=").append(nonce).append("&")
                .append("clientId=").append(ClIENT_ID);
        } catch (UnsupportedEncodingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            if(DebugUtil.isDebug){
                Log.e(TAG, "string build error");
                this.mHandler.sendEmptyMessage(CHECK_FAIL);
                return ;
            }
        }       
        
        String sendContent = sb.toString();
        if(DebugUtil.isDebug){
            Log.d(TAG, "send content:" + sendContent);
        }
        
        
        DefaultHttpClient httpClient = new DefaultHttpClient();
        httpClient.setHttpRequestRetryHandler(requestRetryHandler);
        //得到网络类型    
        String netType = getNetType();
        if(netType != null) {
            
            if(netType.equals("WIFI")) {
                //WIFI网络
                httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECT_TIMEOUT_WIFI);
                httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, SOCKET_TIMEOUT_WIFI);
            } else if(netType.equals("MOBILE")) {
                //移动网络
                httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECT_TIMEOUT_MOBILE);
                httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, SOCKET_TIMEOUT_MOBILE);
                
                String extraInfo = getExtraInfo();
                if(extraInfo != null){
                    if (extraInfo.equals("cmwap") || extraInfo.equals("uniwap")) {
                        HttpHost httpHost = new HttpHost(MOBILE_PROXY_IP, PROXY_PORT);
                        httpClient.getParams().setParameter(ConnRouteParams.DEFAULT_PROXY, httpHost);
                    } else if (extraInfo.equals("ctwap")) {
                        HttpHost httpHost = new HttpHost(TELECOM_PROXY_IP, PROXY_PORT);
                        httpClient.getParams().setParameter(ConnRouteParams.DEFAULT_PROXY, httpHost);
                    }
                }else {
                    if(DebugUtil.isDebug) {
                        Log.d(TAG, "no proxy");
                    }
                }              
            }       
        } 
        else {
            if(DebugUtil.isDebug) {
                Log.d(TAG, "get net type fail");
                this.mHandler.sendEmptyMessage(CHECK_FAIL);
            }
        } 
        
        
        try {
            HttpPost httpPost = new HttpPost(SERVER_URL);               
            httpPost.setEntity(new ByteArrayEntity(sendContent.getBytes("utf-8")));
            
            HttpResponse response = httpClient.execute(httpPost);
            
            if(response.getStatusLine().getStatusCode() ==  HttpStatus.SC_OK){
                InputStream inputStream=response.getEntity().getContent();
                //得到服务器响应
                String responseContent = changeInputStream(inputStream, "utf-8"); 
                
                if(DebugUtil.isDebug){
                    Log.d(TAG, responseContent);
                }
                
                if(responseContent != null) {
                    if(responseContent.startsWith("result")){
                        
                        String responseCode = responseContent.split(":")[1];
                        
                        if(responseCode.equals(CHECK_SUCCESS + "")){
                            this.mHandler.sendEmptyMessage(CHECK_SUCCESS);
                        }else if(responseCode.equals(CHECK_FAIL + "")){
                            this.mHandler.sendEmptyMessage(CHECK_FAIL);
                        }else if(responseCode.equals(CKECK_SERVER_ERROR + "")){
                            this.mHandler.sendEmptyMessage(CKECK_SERVER_ERROR);
                        }
                        
                    }
                    else {
                        this.mHandler.sendEmptyMessage(CKECK_SERVER_ERROR);
                    }
                }else{
                    if(DebugUtil.isDebug){
                        Log.e(TAG, "response is null");                      
                    }
                    this.mHandler.sendEmptyMessage(CHECK_FAIL);
                }
                
            }
            
        }catch(UnknownHostException e){
            //主机解析错误，一般是网络不通造成的异常
            e.printStackTrace();
            this.mHandler.sendEmptyMessage(NETWORK_SOCKETTIMEOUT);
            if(DebugUtil.isDebug){
                Log.e(TAG, "the network is not available , please check the network");
            }
            
        }catch(SocketTimeoutException  e){
            //socket 连接超时，此时应该是网络有问题
            this.mHandler.sendEmptyMessage(NETWORK_SOCKETTIMEOUT);
            e.printStackTrace();
            if(DebugUtil.isDebug){
                Log.e(TAG, "socket_timeout");
            }       
        } catch(ConnectTimeoutException e){
            //connect超时异常，一般是发请求到服务器接收响应超时
            this.mHandler.sendEmptyMessage(NETWORK_CONNECTTIMEOUT);
            e.printStackTrace();
            if(DebugUtil.isDebug){
                Log.e(TAG, "connection_timeout");
            }           
        }catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.e(TAG, "client protocol exception");
            this.mHandler.sendEmptyMessage(CHECK_FAIL);
            
        } catch(NoHttpResponseException e){
            e.printStackTrace();
            if(DebugUtil.isDebug){
                Log.e(TAG, "no http response");
                this.mHandler.sendEmptyMessage(NETWORK_CONNECTTIMEOUT);
            }
        }catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            if(DebugUtil.isDebug){
                Log.e(TAG, "IOException");
            }       
            this.mHandler.sendEmptyMessage(CHECK_FAIL);
        }
    
    }
 
    public void start(){
        new Thread(){

            @Override
            public void run() {
                // TODO Auto-generated method stub
                super.run();
                doImeiCheck();
            }
            
        }.start();
    }
       
}
