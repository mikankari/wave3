package com.example.wave3;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MusicListActivity extends Activity {

	private MediaPlayer mp;
	int playing_position;
	boolean isuserseek;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_music_list);
		
		final ListView listview1 = (ListView)findViewById(R.id.listview1);
//		final MediaPlayer mp = MediaPlayer.create(this, R.raw.dummy);
		listview1.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO 自動生成されたメソッド・スタブ
				Data item = (Data)parent.getItemAtPosition(position);
				
//				Toast.makeText(getApplicationContext(), item.data, Toast.LENGTH_SHORT);
								
//				startItemNew(item.data, position);
//				mp = MediaPlayer.create(getApplicationContext(), Uri.parse(item.data));
//				mp.start();
				
				Intent intent = new Intent(MusicListActivity.this, WaveActivity.class);
				intent.putExtra("url", item.data);
				startActivity(intent);

			}
			
		});

		mp = null;
		playing_position = 0;
		isuserseek = false;
		executeFindMusicFiles(listview1);
		
		new Timer().schedule(new TimerTask() {
			
			@Override
			public void run() {
				// TODO 自動生成されたメソッド・スタブ
				seekUpdate();
			}
		}, (long)0, (long)1000);
	}
	
	protected void onDestroy(){
		super.onDestroy();
		
//		Log.d("test", "ondestroy");
		
		stopItem();
	}

	/**
	 * ここに音楽ファイルを探す処理を書いていきます
	 * メソッド名はなんでも良いです
	 */
	private void executeFindMusicFiles(ListView listview){
		
//		ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1);
		ArrayAdapter<Data> adapter = new ArrayAdapter<Data>(this, android.R.layout.simple_list_item_1);
	
		ContentResolver cr = getContentResolver();
		// URI、射影、選択、引数、
		Cursor cursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
		
		// 1行目があるか
		if(cursor.moveToFirst()){
			
			// タイトルのカラムが何番目か
			int columnIndex_title = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
			int columnIndex_artist = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
			
			do{
				
				// タイトルを取得
				String title = cursor.getString(columnIndex_title);
//				String other = cursor.getString(columnIndex_artist) + "\n";
//				other += cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)) + "\n";
//				other += cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)) + "\n";
				String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
//				String other = "";
				
				// デバッグ用
//				Log.d("test", "title=" + title);
				
				Data item = new Data();
				item.title = title;
				item.data = data;
				adapter.add(item);
//				adapter.add(title + "\n" + other);
				
			}while(cursor.moveToNext());
			
			listview.setAdapter(adapter);
			
		}
		
		
	}
	
	private int getPosition(){
		return playing_position;
	}
	
	private void seekUpdate(){
		if(!isuserseek){
			SeekBar seekbar1 = (SeekBar)findViewById(R.id.seekbar1);
			if(mp != null){
				seekbar1.setProgress((int)((float)mp.getCurrentPosition() / (float)mp.getDuration() * 100.0));
			}else{
				seekbar1.setProgress(0);
			}
		}
	}
	
	private void startItemNew(String data, int position){
		if(mp != null){
			mp.stop();
		}
		mp = MediaPlayer.create(this, Uri.parse(data));
		mp.setOnCompletionListener(new OnCompletionListener() {
			
			@Override
			public void onCompletion(MediaPlayer mp) {
				// TODO 自動生成されたメソッド・スタブ
				ListView listview1 = (ListView)findViewById(R.id.listview1);
				int position = getPosition() + 1;
				if(position >= listview1.getCount()){
					position = listview1.getCount() - 1;
				}
				Data item = (Data)listview1.getItemAtPosition(position);
				
				startItemNew(item.data, position);				
			}
		});
		playing_position = position;
		startItem();
		
//Log.d("test", data);
	}
	
	private void startItem(){
		if(mp == null){
			return;
		}
		mp.start();
		Button button = (Button)findViewById(R.id.button_play);
		button.setText("||");
	}
	
	private void stopItem(){
		if(mp == null){
			return;
		}
		mp.pause();
		Button button = (Button)findViewById(R.id.button_play);
		button.setText(">");
	}

	private static class Data{
		String title;
		String data;
		
		// ここがリストビューに表示される
		public String toString(){
			return title;
		}
	}

}
