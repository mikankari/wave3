package com.example.wave3;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

public class WaveActivity extends Activity {

	WavePlayer player;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wave);
		
		Intent intent = getIntent();
		String uri = intent.getStringExtra("uri");
		try {
			player = new WavePlayer(uri); 			
		} catch (Exception e) {
			// TODO: handle exception
			Log.d("", "ERROR! " + e);
		}

		View view = new WaveView(this);
		FrameLayout layout = (FrameLayout)findViewById(R.id.container);
		layout.addView(view);
		
		
	}
	
	protected void onStart(){
		super.onStart();
		
		player.start();
	}
	
	protected void onStop(){
		super.onStop();
		
		player.yield();
	}

}
