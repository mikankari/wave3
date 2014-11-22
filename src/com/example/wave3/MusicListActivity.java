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
				// TODO �����������ꂽ���\�b�h�E�X�^�u
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
				// TODO �����������ꂽ���\�b�h�E�X�^�u
				startAnalysing();
			}
		});
		final Button button2 = (Button)findViewById(R.id.button2);
		button2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO �����������ꂽ���\�b�h�E�X�^�u
				stopAnalysing();
			}
		});
		
		analysing_thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO �����������ꂽ���\�b�h�E�X�^�u
				// ��͒�
				for (int i = 0; i < listview1.getCount() && analysing_isloop; i++) {					
					Data item = (Data)listview1.getItemAtPosition(i);
					
					try {
						WavePlayer player = new WavePlayer(item.data);
						player.start();
						while(player.getDecodePercentage() != 1 && analysing_isloop){
							item.info = Math.round(player.getDecodePercentage() * 100) + "% ��͒�...";
							handler.post(new Runnable() {
								@Override
								public void run() {
									// TODO �����������ꂽ���\�b�h�E�X�^�u
									((ArrayAdapter)listview1.getAdapter()).notifyDataSetChanged();									
								}
							});
							Thread.sleep(1000);
						}
						item.info = "BPM" + player.getBPM();
						handler.post(new Runnable() {
							@Override
							public void run() {
								// TODO �����������ꂽ���\�b�h�E�X�^�u
								((ArrayAdapter)listview1.getAdapter()).notifyDataSetChanged();									
							}
						});
						FileWriter.writePublicFile(context, item.title, item.info);
						player.stop();
					} catch (Exception e) {
						// TODO �����������ꂽ catch �u���b�N
						Log.e("", e.toString() + " on analysing: " + i);
					}
				}
				
			}
		});

		executeFindMusicFiles(listview1);
		
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				// TODO �����������ꂽ���\�b�h�E�X�^�u
				// ������s
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
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		}
	}

	/**
	 * �����ɉ��y�t�@�C����T�������������Ă����܂�
	 * ���\�b�h���͂Ȃ�ł��ǂ��ł�
	 */
	private void executeFindMusicFiles(ListView listview){
		
//		ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1);
		ArrayAdapter<Data> adapter = new ArrayAdapter<Data>(this, android.R.layout.simple_list_item_1);
	
		ContentResolver cr = getContentResolver();
		// URI�A�ˉe�A�I���A�����A
		Cursor cursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
		
		// 1�s�ڂ����邩
		if(cursor.moveToFirst()){
			
			// �^�C�g���̃J���������Ԗڂ�
			int columnIndex_title = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
			int columnIndex_artist = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
			
			do{
				
				// �^�C�g�����擾
				String title = cursor.getString(columnIndex_title);
//				String other = cursor.getString(columnIndex_artist) + "\n";
//				other += cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)) + "\n";
//				other += cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)) + "\n";
				String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
//				String other = "";
				
				// �f�o�b�O�p
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
		
		// ���������X�g�r���[�ɕ\�������
		public String toString(){
			return info + " " + title;
		}
	}

}
