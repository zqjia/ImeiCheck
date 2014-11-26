package com.uc.imeicheck;

public class ImeiData {

	private String mPlatform;
	private String mProduct;
	private String mVer;
	private String mSubver;
	private String mImei;
	
	public ImeiData(){
		
	}
	
	public ImeiData(String platform, String product, String ver, String subver, String imei){
		this.mPlatform = platform;
		this.mProduct = product;
		this.mVer = ver;
		this.mSubver = subver;
		this.mImei = imei;
	}
	 
	 public static ImeiData create(){
		 return new ImeiData();
	 }
	 
	 public ImeiData setPlatform(String platform){
		 this.mPlatform = platform;
		 return this;
	 }
	 
	 public ImeiData setProduct(String product){
	 	mProduct = product;
		return this;
	 }
	
	 public ImeiData setVer(String ver){
		mVer = ver;
		return this;
	 }
	
	 public ImeiData setSubver(String subver){
		mSubver = subver;
		return this;
	 }
	
	 public ImeiData setImei(String imei) {
		mImei = imei;
		return this;
	 }
	  
	 public String getPlatform(){
		 return this.mPlatform;
	 }
	 
	 public String getProduct(){
		 return this.mProduct;
	 }
	 
	 public String getVer(){
		 return this.mVer;
	 }
	 
	 public String getSubver(){
		 return this.mSubver;
	 }
	 
	 public String getImei(){
		 return this.mImei;
	 }

}
