package com.example.wave3;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

public class WaveActivity extends Activity {

	MediaPlayer player;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wave);
		
		Intent intent = getIntent();
		String url = intent.getStringExtra("url");
		player = MediaPlayer.create(this, Uri.parse(url));	// vocaloid.mp3 or bach.mp3 or you.mp3 or 1khz-0db-30sec.wav

		View view = new WaveView(this, player);
		FrameLayout layout = (FrameLayout)findViewById(R.id.container);
		layout.addView(view);
		
	}
	
	protected void onStart(){
		super.onStart();
		
		player.start();
	}
	
	protected void onStop(){
		super.onStop();
		
		player.pause();
	}

}
