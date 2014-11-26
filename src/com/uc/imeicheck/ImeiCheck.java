package com.uc.imeicheck;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;



public class ImeiCheck implements Callback {
	
	private static final String TAG = "ImeiCheck";
	
	private static String[] ImeiTable = { };
	
	//设置是否进行IMEI验证
	private static boolean IS_CHECK_IMEI = true;
	//是否只在本地验证，不进行云端验证
	private static boolean IS_CHECK_ONLY_LOCALLY = false;
	
	private Context mContext;
	private String mPlatform;
	private String mProduct;
	private String mVer;
	private String mSubver;
	private String mImei;
	
	private final int CHECK_FAIL = 0;
	private final int CHECK_SUCCESS = 1;
	private final int NOT_CHECK = 2;
	private final int CHECK_ALREADY = 3;
		
	private final int CKECK_SERVER_ERROR = -1;
	private final int NETWORK_SOCKETTIMEOUT = -2;
	private final int NETWORK_CONNECTTIMEOUT = -3;
	private final int CHECK_ONLY_LOCAL_FAIL = -4;
	
    private final String preferencesName = "ICI";
    private final String keyName = "SN";
	
	private ImeiCheckCallBack mListener;
	private ProgressDialog mImeiCheckDialog = null;
	
	private Handler mhandler = new Handler(Looper.getMainLooper(), this);
	
	private String mNewSignature;
	private final String DES_KEY = "U2FsdGVkX18tehxx";
	
	public ImeiCheck(Context context, String platform, String product, String ver, String subver, 
			String imei, ImeiCheckCallBack listener){
		this.mContext = context;
		this.mPlatform = platform;
		this.mProduct = product;
		this.mVer = ver;
		this.mSubver = subver;
		this.mImei = imei;
		this.mListener = listener;
	}
		
	public void startCheck(){
		
		//判断是否进行IMEI验证
		if(!IS_CHECK_IMEI){
			if(DebugUtil.isDebug){
				Log.d(TAG, "not check imei");
			}			
			this.mhandler.sendEmptyMessage(NOT_CHECK);
			return;
		}
		
		//imei为空时
		if(this.mImei == null){
			if(DebugUtil.isDebug){
				Log.e(TAG, "IMEI is null");
			}
			this.mhandler.sendEmptyMessage(CHECK_FAIL);
		}
		

		SharedPreferences preferences = this.mContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE);
				
		//本地验证只存储md5的sign，下次本地验证直接md5验证就行了
		String oldSignature = preferences.getString(keyName, "");
		ImeiEncrypt encryptObj = new ImeiEncrypt(DES_KEY);
		try {
            this.mNewSignature = encryptObj.encrypt(this.mVer + this.mSubver);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            if(DebugUtil.isDebug){
                Log.e(TAG, "encrypt version fail, check the des encrypt");
            }
            this.mhandler.sendEmptyMessage(CHECK_FAIL);
        }
		
		if(oldSignature.equals(this.mNewSignature)){
			//此时不需要验证，之前已经验证通过，直接返回
			if(DebugUtil.isDebug){
				Log.d(TAG, "have check before");
			}
			this.mhandler.sendEmptyMessage(CHECK_ALREADY);
			return ;
		}else {
			//此时需要验证,弹出progressDialog
			this.mImeiCheckDialog = showDialog(this.mContext, this.mContext.getString(R.string.loading_toast));
			
			if(localCheck()){		//本地验证	通过
				this.mhandler.sendEmptyMessage(CHECK_SUCCESS);
				return ;
			}else if(!IS_CHECK_ONLY_LOCALLY){  //本地验证不通过，进行云端验证
				ImeiData checkData = ImeiData.create().setPlatform(this.mPlatform)
														.setProduct(this.mProduct)
														.setVer(this.mVer)
														.setSubver(this.mSubver)
														.setImei(this.mImei);
														
				new ImeiCheckProcess(checkData, this.mContext, this.mhandler).start();
				
			}else{  //本地验证不通过，而且不进行云端验证
				this.mhandler.sendEmptyMessage(CHECK_ONLY_LOCAL_FAIL);
				return;
			}
		}		
		
	}
	
	private boolean localCheck(){
		
		for(int i=0; i<ImeiTable.length; i++){			
			if(this.mImei.equals(ImeiTable[i]))
				return true;
		}
		
		return false;
	}

	@Override
	public boolean handleMessage(Message msg) {
		
		switch(msg.what){
		case NOT_CHECK:
			if(DebugUtil.isDebug){
				Log.d(TAG, "not check imei");
			}
			this.mListener.handleSuccess();
			break;
		case CHECK_ALREADY:
			if(DebugUtil.isDebug){
				Log.d(TAG, "have already check before");
			}
			this.mListener.handleSuccess();
			break;
		case CHECK_ONLY_LOCAL_FAIL:
			Toast.makeText(this.mContext, this.mContext.getString(R.string.local_check_fail), Toast.LENGTH_SHORT).show();
			this.mListener.handleFail();
			break;		
		case CHECK_SUCCESS:
			dismissDialog(this.mImeiCheckDialog);
			Toast.makeText(this.mContext, this.mContext.getString(R.string.check_success), Toast.LENGTH_SHORT).show();
						
			SharedPreferences preferences = this.mContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString(keyName, this.mNewSignature);
			editor.commit();
			
			this.mListener.handleSuccess();
			break;
		case CHECK_FAIL:
			dismissDialog(this.mImeiCheckDialog);
			Toast.makeText(this.mContext, this.mContext.getString(R.string.check_fail), Toast.LENGTH_SHORT).show();
			this.mListener.handleFail();
			break;		
		case CKECK_SERVER_ERROR:
			dismissDialog(this.mImeiCheckDialog);
			Toast.makeText(this.mContext, this.mContext.getString(R.string.server_error), Toast.LENGTH_SHORT).show();
			this.mListener.handleFail();
			break;
		case NETWORK_SOCKETTIMEOUT:
			//此时属于建立连接时超时，连不上服务器
			dismissDialog(this.mImeiCheckDialog);
			Toast.makeText(this.mContext, this.mContext.getString(R.string.socket_timeout), Toast.LENGTH_SHORT).show();
			this.mListener.handleFail();
			break;
		case NETWORK_CONNECTTIMEOUT:
			//此时属于网络差，连上服务器又断掉了，即发送请求道服务器，然后接收不到返回
			dismissDialog(this.mImeiCheckDialog);
			Toast.makeText(this.mContext, this.mContext.getString(R.string.connection_timeout), Toast.LENGTH_SHORT).show();
			this.mListener.handleFail();
			break;
		}
		
		//True if no further handling is desired, else false
		return true;
	}
	
	 private ProgressDialog showDialog(Context context,String strContent){
        if (context != null){
        	
            ProgressDialog infoDialog = new ProgressDialog(context);
            infoDialog.setMessage(strContent);
            infoDialog.show();
            return infoDialog;
        }
        return null;
	}
	 
    private void dismissDialog(ProgressDialog infoDialog){
    	
        if (infoDialog != null){
        	
            infoDialog.dismiss();
        }
        infoDialog = null;
    }
}
