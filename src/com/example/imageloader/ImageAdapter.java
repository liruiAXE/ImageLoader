package com.example.imageloader;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class ImageAdapter extends BaseAdapter{
    
	LayoutInflater inflater; 
	Context mContext; 
	public ImageAdapter(Context context,List<String> urllist){
		inflater=LayoutInflater.from(context);  
		mContext=context; 
		mUrlList=urllist; 
	}
	List<String> mUrlList; 
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mUrlList.size(); 
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return mUrlList.get(position); 
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position; 
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		 Holder holder=null; 
		 if (convertView==null){
			 convertView=inflater.inflate(R.layout.list_item, parent,false); 
			 holder=new Holder(); 
			 holder.image=(ImageView)convertView.findViewById(R.id.image); 
			 convertView.setTag(holder); 
		 } else {
			 holder=(Holder) convertView.getTag(); 
		 }
		 holder.image.setTag(getItem(position)); 
		 ImageLoader.build(mContext).bindBitmap(mUrlList.get(position), holder.image,holder.image.getMeasuredWidth(),holder.image.getMeasuredHeight()); 
		 return convertView; 
	}
    private class Holder{
    	public ImageView image; 
    	public Holder(){
    		
    	}
    }
}
