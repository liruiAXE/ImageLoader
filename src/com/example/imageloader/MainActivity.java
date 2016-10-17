package com.example.imageloader;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.ImageView;

public class MainActivity extends Activity {
    
	GridView gview=null; 
	List<String> urls=new ArrayList<String>(); 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gview=(GridView)findViewById(R.id.gridView1);   
        init(); 
        ImageAdapter adapter=new ImageAdapter(getBaseContext(),urls); 
        gview.setAdapter(adapter); 
    }
    private void init(){
    	String[] s=new String[]{"http://static.ettoday.net/images/190/d190943.jpg",
    			"http://i.imgur.com/o5jbt.jpg",
    			"http://image.tpwang.net/image/%E7%90%B3/artist-%E7%90%B3%E5%A8%9C%C2%B7%E6%B5%B7%E8%92%82/%E7%90%B3%E5%A8%9C%C2%B7%E6%B5%B7%E8%92%82189027.jpg",
    			"http://img1.gtimg.com/ent/pics/hv1/134/26/1864/121213364.jpg",
    			"http://image.tpwang.net/image/%E7%90%B3/artist-%E7%90%B3%E5%A8%9C%C2%B7%E6%B5%B7%E8%92%82/%E7%90%B3%E5%A8%9C%C2%B7%E6%B5%B7%E8%92%8263656.jpg",
    			"http://ruby.komica.org/pix/img2468.jpg",
    			"http://static.ettoday.net/images/190/d190943.jpg"};
    	for (int i=0;i<s.length;i++){
    		urls.add(s[i]); 
    	}
    	 
    	
    }
}
