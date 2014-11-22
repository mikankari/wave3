package com.example.wave3;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class MusicListActivity extends Activity {

	Thread analysing_thread;
	boolean analysing_isloop;
	Handler handler = new Handler();
	Context context = this;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_music_list);
		
		final ListView listview1 = (ListView)findViewById(R.id.listview1);
		listview1.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO 自動生成されたメソッド・スタブ
				if(!isAnalysing()){
					Data item = (Data)parent.getItemAtPosition(position);
					Intent intent = new Intent(MusicListActivity.this, WaveActivity.class);   
					intent.putExtra("uri", item.data);   
					startActivity(intent);					
				}
			}
		});
		
		final Button button1 = (Button)findViewById(R.id.button1);
		button1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO 自動生成されたメソッド・スタブ
				startAnalysing();
			}
		});
		final Button button2 = (Button)findViewById(R.id.button2);
		button2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO 自動生成されたメソッド・スタブ
				stopAnalysing();
			}
		});
		
		analysing_thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO 自動生成されたメソッド・スタブ
				// 解析中
				for (int i = 0; i < listview1.getCount() && analysing_isloop; i++) {					
					Data item = (Data)listview1.getItemAtPosition(i);
					
					try {
						WavePlayer player = new WavePlayer(item.data);
						player.start();
						while(player.getDecodePercentage() != 1 && analysing_isloop){
							item.info = Math.round(player.getDecodePercentage() * 100) + "% 解析中...";
							handler.post(new Runnable() {
								@Override
								public void run() {
									// TODO 自動生成されたメソッド・スタブ
									((ArrayAdapter)listview1.getAdapter()).notifyDataSetChanged();									
								}
							});
							Thread.sleep(1000);
						}
						item.info = "BPM" + player.getBPM();
						handler.post(new Runnable() {
							@Override
							public void run() {
								// TODO 自動生成されたメソッド・スタブ
								((ArrayAdapter)listview1.getAdapter()).notifyDataSetChanged();									
							}
						});
						FileWriter.writePublicFile(context, item.title, item.info);
						player.stop();
					} catch (Exception e) {
						// TODO 自動生成された catch ブロック
						Log.e("", e.toString() + " on analysing: " + i);
					}
				}
				
			}
		});

		executeFindMusicFiles(listview1);
		
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				// TODO 自動生成されたメソッド・スタブ
				// 定期実行
			}
		}, (long)0, (long)1000);
	}
	
	protected void onDestroy(){
		super.onDestroy();
	}
	
	public boolean isAnalysing(){
		return analysing_isloop;
	}
	
	public void startAnalysing(){
		analysing_isloop = true;
		analysing_thread.start();
	}
	
	public void stopAnalysing(){
		analysing_isloop = false;
		try {
			analysing_thread.join();
		} catch (InterruptedException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
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
				item.info = "";
				adapter.add(item);
//				adapter.add(title + "\n" + other);
				
			}while(cursor.moveToNext());
			
			listview.setAdapter(adapter);
			
		}
		
		
	}

	private static class Data{
		String title;
		String data;
		String info;
		
		// ここがリストビューに表示される
		public String toString(){
			return info + " " + title;
		}
	}

}
