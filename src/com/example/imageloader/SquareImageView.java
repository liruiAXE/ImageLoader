package com.example.imageloader;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class SquareImageView extends ImageView{
	public SquareImageView(Context context){
		super(context); 
	}
	public SquareImageView(Context context,AttributeSet attrs){
		super(context,attrs); 
	}
    protected void onMeasure(int widthMeasureSpec,int hegihtMeasureSpec){
    	super.onMeasure(widthMeasureSpec, widthMeasureSpec); 
    }
}
