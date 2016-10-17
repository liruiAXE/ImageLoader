package com.example.imageloader;

import java.io.FileDescriptor;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

public class ImageResizer {
   private static final String TAG="ImageResizer"; 
   public ImageResizer(){
	   
   }
   public Bitmap decodeSampleBitmapFromResource(Resources res,int resId,int reqWidth,int reqHeight){
	   final BitmapFactory.Options options=new BitmapFactory.Options(); 
	   options.inJustDecodeBounds=true; 
	   BitmapFactory.decodeResource(res, resId,options); 
	   options.inSampleSize=calculateInSampleSize(options,reqWidth,reqHeight); 
	   options.inJustDecodeBounds=false; 
	   return BitmapFactory.decodeResource(res, resId,options); 
	   
   }
   public Bitmap decodeSampleBitmapFromFileDescriptor(FileDescriptor fd,int reqWidth,int reqHeight){
	   final BitmapFactory.Options options=new BitmapFactory.Options();
	   options.inJustDecodeBounds=true; 
	   BitmapFactory.decodeFileDescriptor(fd, null, options); 
	   options.inSampleSize=calculateInSampleSize(options,reqWidth,reqHeight); 
	   options.inJustDecodeBounds=false; 
	   return BitmapFactory.decodeFileDescriptor(fd, null, options); 
	   
	   
   }
   
   public int calculateInSampleSize(BitmapFactory.Options options,int reqWidth,int reqHeight){
	  int sampleSize=1; 
	  int outWidth=options.outWidth; 
	  int outHeight=options.outHeight; 
	  
	  if (reqWidth==0 || reqHeight==0){
		  return 1; 
	  }
	  
	  while(outWidth>reqWidth && outHeight>reqHeight){
		  sampleSize*=2; 
		  reqWidth*=sampleSize;
		  reqHeight*=sampleSize; 
	  } 
	  return sampleSize; 
   }
}
