package com.example.imageloader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.jakewharton.disklrucache.DiskLruCache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

public class ImageLoader {
   private static final String TAG="ImageLoader"; 
   public static final int MESSAGE_POST_RESULT=1; 
   private static final int CPU_COUNT=Runtime.getRuntime().availableProcessors(); 
   private static final int CORE_POOL_SIZE=CPU_COUNT+1;
   private static final int MAXIMUM_POOL_SIZE=CPU_COUNT*2+1; 
   private static final long KEEP_ALIVE=10L; 
   
    
   private static final long DISK_CACHE_SIZE=1024*1024*50; 
   private static final int IO_BUFFER_SIZE=80*1024; 
   private static final int DISK_CACHE_INDEX=0; 
   private boolean mIsDiskLruCacheCreated=false; 
   
   private static final ThreadFactory sThreadFactory=new ThreadFactory(){
	   private final AtomicInteger mCount=new AtomicInteger(1); 
	   public Thread newThread(Runnable r){
		   return new Thread(r,"ImageLoader#"+mCount.getAndIncrement()); //这里会被并行访问到吗
	   }
	 
   }; 
   public static final Executor THREAD_POOL_EXECUTOR=new ThreadPoolExecutor(CORE_POOL_SIZE,MAXIMUM_POOL_SIZE,KEEP_ALIVE,TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>(),sThreadFactory); 
   private Handler mMainHandler=new Handler(Looper.getMainLooper()){
	 public void handleMessage(Message msg){
		 LoaderResult result=(LoaderResult)msg.obj; 
		 ImageView imageView=result.imageview; 
		 String uri=(String)imageView.getTag(R.id.TAG_KEY_URI);
		 if (uri.equals(result.uri)){
			 imageView.setImageBitmap(result.bitmap); 
		 } else {
			 Log.w(TAG,"set image bitmap,but url has changed,ignored!");
		 }
	 }
   }; 
   private Context mContext; 
   private ImageResizer mImageResizer=new ImageResizer(); 
   private LruCache<String,Bitmap> mMemoryCache; 
   private DiskLruCache mDiskLruCache; 
   
   private ImageLoader(Context context){
	   mContext=context.getApplicationContext(); 
	   int maxMemory=(int)(Runtime.getRuntime().maxMemory()/1024); 
	   int chacheSize=maxMemory/8; 
	   mMemoryCache=new LruCache<String,Bitmap>(chacheSize){
		   protected int sizeOf(String key,Bitmap bitmap){
			   return bitmap.getRowBytes()*bitmap.getHeight()/1024; 
		   }
	   }; 
	   File diskCacheDir=getDiskCacheDir(mContext,"bitmap"); 
	   if (!diskCacheDir.exists()){
		   diskCacheDir.mkdirs(); 
	   }
	   if (getUsableSpace(diskCacheDir)>DISK_CACHE_SIZE){
		   try {
			   mDiskLruCache=DiskLruCache.open(diskCacheDir, 1, 1, DISK_CACHE_SIZE); 
			   mIsDiskLruCacheCreated=true; 
		   } catch (IOException e){
			   e.printStackTrace(); 
		   }
	   }
	   
   } 
   public static ImageLoader build(Context context){
	   return new ImageLoader(context); 
   }
   private void addBitmapToMemoryCache(String key,Bitmap bitmap){
	   if (getBitmapFromMenCache(key)==null){
		   mMemoryCache.put(key, bitmap); 
	   }
   }
   private Bitmap getBitmapFromMenCache(String key){
	   return mMemoryCache.get(key); 
   }
   public void bindBitmap(final String uri,final ImageView imageView){
	   imageView.setTag(R.id.TAG_KEY_URI,uri); 
	   Bitmap bitmap=loadBitmapFromMemCache(uri); 
	   if (bitmap!=null){
		   imageView.setImageBitmap(bitmap); 
		   return;
	   } 
	   Runnable loadBitmapTask=new Runnable(){ 
		   public void run(){
			   Bitmap bitmap=loadBitmap(uri,Integer.MAX_VALUE/2,Integer.MAX_VALUE/2); 
			   if (bitmap!=null){ 
				   
				   LoaderResult result=new LoaderResult(imageView,uri,bitmap); 
				   mMainHandler.obtainMessage(MESSAGE_POST_RESULT,result).sendToTarget(); 
			   }
		   }
	     
	   }; 
	   THREAD_POOL_EXECUTOR.execute(loadBitmapTask); 
   }
   public void bindBitmap(final String uri,final ImageView imageView,final int reqWidth,final int reqHeight){ 
	   Log.i("liruiim","################################################");
	   imageView.setTag(R.id.TAG_KEY_URI, uri); 
	   Bitmap bitmap=loadBitmapFromMemCache(uri); 
	   if (bitmap!=null){
		   imageView.setImageBitmap(bitmap); 
		   return; 
	   }
	   Runnable loadBitmapTask=new Runnable(){
		   public void run(){
			  Bitmap bitmap=loadBitmap(uri,reqWidth,reqHeight);    
			  if (bitmap!=null){
				  Log.i("liruiim"," Runnable bitmap!=null"); 
				  LoaderResult result=new LoaderResult(imageView,uri,bitmap);
				  mMainHandler.obtainMessage(MESSAGE_POST_RESULT,result).sendToTarget(); 
			  }
		   }
	   }; 
	   THREAD_POOL_EXECUTOR.execute(loadBitmapTask); 
   } 
   public Bitmap loadBitmap(String uri,int reqWidth,int reqHeight){
	   Bitmap bitmap=loadBitmapFromMemCache(uri); 
	   if (bitmap!=null){
		   return bitmap; 
	   }
	   try{
		   bitmap=loadBitmapFromDiskCache(uri,reqWidth,reqHeight); 
		   if (bitmap!=null) return bitmap;
		   bitmap=loadBitmapFromHttp(uri,reqWidth,reqHeight); 
		   Log.d(TAG,"loadBitmapFromHttp,url: "+uri); 
	   } catch(IOException e){
		   e.printStackTrace(); 
	   } 
	   if (bitmap==null && !mIsDiskLruCacheCreated){
		   Log.w(TAG,"encouter error,DiskLruCache is not created."); 
		   bitmap=downloadBitmapFromUrl(uri); 
	   }
	   return bitmap; 
	   
   }  
   private Bitmap downloadBitmapFromUrl(String urlString){
	   Bitmap bitmap=null; 
	   HttpURLConnection urlConnection=null; 
	   BufferedInputStream in=null; 
	   try{
		   final URL url=new URL(urlString); 
		   urlConnection=(HttpURLConnection)url.openConnection(); 
		   in=new BufferedInputStream(urlConnection.getInputStream(),IO_BUFFER_SIZE); 
		   bitmap=BitmapFactory.decodeStream(in); 
	   } catch(final IOException e){
		   Log.e(TAG,"Error in downloadBitmap: "+e); 
	   } finally {
		   if (urlConnection!=null){
			   urlConnection.disconnect(); 
		   }
		   try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	   }
	   return bitmap;
   }
   private Bitmap loadBitmapFromHttp(String url,int reqWidth,int reqHeight) throws IOException{ 
	   Log.i("liruiim","loadBitmapFromHttp() called!"); 
	   if (Looper.myLooper()==Looper.getMainLooper()){
		   Log.w(TAG,"can not visit network from UI Thread."); 
	   }
	   if(mDiskLruCache==null){
		   return null; 
	   }
	   String key=hashKeyFromUrl(url); 
	   DiskLruCache.Editor editor=mDiskLruCache.edit(key); 
	   if (editor!=null){
		   OutputStream outputStream=editor.newOutputStream(DISK_CACHE_INDEX); 
		   if (downloadUrlToStream(url,outputStream)){
			   editor.commit();  
			   Log.i("liruiim","editor.commit()"); 
		   } else {
			   editor.abort(); 
		   }
		   mDiskLruCache.flush(); 
	   }
	   return loadBitmapFromDiskCache(url,reqWidth,reqHeight); 
   }
   public boolean downloadUrlToStream(String urlString,OutputStream outputStream){ 
	   Log.i("liruiim","downloadtoStream() called"); 
	   Log.i("liruiim","Thread : "+Thread.currentThread().getId()); 
	   HttpURLConnection urlConnection=null; 
	   BufferedOutputStream out=null; 
	   BufferedInputStream in=null; 
	   try{
		   final URL url=new URL(urlString); 
		   urlConnection = (HttpURLConnection)url.openConnection(); 
		   in=new BufferedInputStream(urlConnection.getInputStream(),IO_BUFFER_SIZE); 
		   out=new BufferedOutputStream(outputStream,IO_BUFFER_SIZE); 
		   int b; 
		   while ((b=in.read())!=-1){
			   out.write(b); 
		   }  
		   Log.i("liruiim","urlconnection true"); 
		   return true; 
	   } catch(IOException e){
		   Log.e(TAG,"downloadBitmap failed."+e); 
	   } finally{
		   if (urlConnection!=null){
			   urlConnection.disconnect(); 
		   } 
		   try{
			    if (in!=null)
			    in.close(); 
			    if (out!=null)
		        out.close(); 
		   } catch(IOException e){
			   e.printStackTrace(); 
		   }
		  
	   }
	   return false; 
   }
   private Bitmap loadBitmapFromDiskCache(String url,int reqWidth,int reqHeight) throws IOException{
	   if (Looper.myLooper()==Looper.getMainLooper()){
		   Log.w(TAG,"load bitmap from UI Thread,its not recommended!"); 
	   }
	   if (mDiskLruCache==null){
		   return null; 
	   }
	   Bitmap bitmap=null; 
	   String key=hashKeyFromUrl(url);  
	   Log.i("liruiim","MD5 : "+key); 
	   DiskLruCache.Snapshot snapShot=mDiskLruCache.get(key); 
	   if (snapShot!=null){
		   FileInputStream fileInputStream=(FileInputStream)snapShot.getInputStream(DISK_CACHE_INDEX); 
		   FileDescriptor fileDescriptor=fileInputStream.getFD();  
		   Log.i("liruiim","decode from FD"); 
		   bitmap=mImageResizer.decodeSampleBitmapFromFileDescriptor(fileDescriptor, reqWidth, reqHeight); 
		   if (bitmap!=null){
			   addBitmapToMemoryCache(key,bitmap); 
		   }
		   
	   }  
	   if (bitmap!=null)
	   Log.i("liruiim","bitmap hashcode() : "+ bitmap.hashCode()); 
	   return bitmap; 
   }
   private Bitmap loadBitmapFromMemCache(String uri){
	   final String key=hashKeyFromUrl(uri); 
	   return getBitmapFromMenCache(key); 
   }
   private String hashKeyFromUrl(String url){
	   String cacheKey; 
	   try{
		   final MessageDigest mDigest=MessageDigest.getInstance("MD5"); 
		   mDigest.update(url.getBytes()); 
		   cacheKey=bytesToHexString(mDigest.digest()); 
	   } catch(NoSuchAlgorithmException e){
		   cacheKey=String.valueOf(url.hashCode()); 
	   }
	   return cacheKey; 
   } 
   private String bytesToHexString(byte[] bytes){
	   StringBuilder sb=new StringBuilder(); 
	   for (int i=0;i<bytes.length;i++){
		   String hex=Integer.toHexString(0xFF&bytes[i]); 
		   if (hex.length()==1){
			   sb.append('0'); 
		   } 
		   sb.append(hex); 
	   }
	   return sb.toString(); 
   }
   private long getUsableSpace(File path){
	   if (Build.VERSION.SDK_INT>=VERSION_CODES.GINGERBREAD){
		   return path.getUsableSpace(); 
	   } 
	   final StatFs stats=new StatFs(path.getPath()); 
	   return stats.getBlockCountLong()*stats.getAvailableBlocksLong(); 
   }
   public File getDiskCacheDir(Context context,String uniquename){
	   boolean externalStorageAvailable=Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED); 
	   final String cachePath; 
	   if (externalStorageAvailable){
		   cachePath=context.getExternalCacheDir().getParent(); 
	   } else {
		   cachePath=context.getCacheDir().getPath(); 
	   }
	   return new File(cachePath+File.separator+uniquename); 
   }
   private static class LoaderResult{
	   public ImageView imageview; 
	   public String uri; 
	   public Bitmap bitmap; 
	   public LoaderResult(ImageView imageView,String uri,Bitmap bitmap){
		   this.imageview=imageView; 
		   this.uri=uri; 
		   this.bitmap=bitmap; 
	   }
   }
}
