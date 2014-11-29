package com.example.wave3;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.content.Context;
import android.util.Log;

public class FileWriter {
	public static String readPublicFile(Context context, String filename){
		String text;
		try{					
			File file = new File(context.getExternalFilesDir(null), filename);
			FileInputStream in = new FileInputStream(file);
			BufferedInputStream stream = new BufferedInputStream(in);
			text = "";
			byte[] temp = new byte[stream.available()];
			while(stream.read(temp) != -1){
				text += new String(temp, "UTF-8");
			}
			stream.close();
		}catch(Exception exception){
			text = null;
			Log.e("", exception.toString() + " on reading");
		}
		return text;
	}

	public static String writePublicFile(Context context, String filename, String text){
		try{
			
					File file = new File(context.getExternalFilesDir(null), filename);
			FileOutputStream output = new FileOutputStream(file);
			BufferedOutputStream stream = new BufferedOutputStream(output);
			stream.write(text.getBytes("UTF-8"));
			stream.close();
			
		}catch(Exception exception){
			text = null;
			Log.e("", exception.toString() + "on writing");
		}				
		return text;
	}

	public static String readPrivateFile(Context context, String filename){
		String text;
		try{					
			File file = new File(context.getExternalFilesDir(null), filename);
			FileInputStream in = context.openFileInput(file.getName());
			BufferedInputStream stream = new BufferedInputStream(in);
			text = "";
			byte[] temp = new byte[stream.available()];
			while(stream.read(temp) != -1){
				text += new String(temp, "UTF-8");
			}
			stream.close();
		}catch(Exception exception){
			text = null;
			Log.e("", exception.toString() + " on reading");
		}
		return text;
	}

	public static String writePrivateFile(Context context, String filename, String text){
		try{
			
			File file = new File(context.getExternalFilesDir(null), filename);
			FileOutputStream output = context.openFileOutput(file.getName(), context.MODE_PRIVATE);
			BufferedOutputStream stream = new BufferedOutputStream(output);
			stream.write(text.getBytes("UTF-8"));
			stream.close();
			
		}catch(Exception exception){
			text = null;
			Log.e("", exception.toString() + "on writing");
		}
		return text;
	}


}
