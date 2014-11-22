package com.example.wave3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

public class WavePlayer{
	
	private MediaExtractor extractor;
	private MediaFormat format;
	private MediaCodec codec;
	private Thread inputbuffer_thread;
	private Thread outputbuffer_thread;
	private boolean inputbuffer_isloop;
	private boolean outputbuffer_isloop;
	private byte[] waveform;
	private int waveform_index;
	private HashMap<Integer, Integer> bpms;
	
	public WavePlayer(String uri) throws IOException{
		// TODO 自動生成されたコンストラクター・スタブ
		extractor = new MediaExtractor();
		extractor.setDataSource(uri);
		extractor.selectTrack(extractor.getTrackCount() - 1);
		format = extractor.getTrackFormat(extractor.getTrackCount() - 1);
		String mimetype = format.getString(MediaFormat.KEY_MIME);

		codec = MediaCodec.createDecoderByType(mimetype);
		Log.d("", "ok");
		codec.configure(format, null, null, 0);
		codec.start();
		
		final ByteBuffer[] inputbuffer = codec.getInputBuffers();
		final ByteBuffer[] outputbuffer = codec.getOutputBuffers();
				
		inputbuffer_thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO 自動生成されたメソッド・スタブ
				
				boolean isEOS = false;
//				ByteBuffer[] inputbuffer = codec.getInputBuffers();
				
				while(inputbuffer_isloop){
					int inputbuffer_index = codec.dequeueInputBuffer(1);
					if(inputbuffer_index >= 0){
						ByteBuffer buffer = inputbuffer[inputbuffer_index];
						int bufferSize = extractor.readSampleData(buffer, 0);
						long presentationTimeUs = 0;
						if(bufferSize < 0){
							bufferSize = 0;
							isEOS = true;
							Log.d("", "End of Stream");
							break;
						}else{
							presentationTimeUs = extractor.getSampleTime();
						}
						codec.queueInputBuffer(inputbuffer_index,
												0,
												bufferSize, 
												presentationTimeUs,
												!isEOS ? 0 : MediaCodec.BUFFER_FLAG_END_OF_STREAM);
						if(!isEOS){
							extractor.advance();
						}
					}else{
//						Log.d("", "fail to get inputbuffer");
						try {
							Thread.sleep(16);
						} catch (Exception e) {
							// TODO: handle exception
							Log.e("", e.toString() + " in inputbuffer");
						}
					}
				}
			}
			
		});
		
		outputbuffer_thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO 自動生成されたメソッド・スタブ
//				ByteBuffer[] outputbuffer = null;
				MediaCodec.BufferInfo bufferinfo = new MediaCodec.BufferInfo();
				AudioTrack track = null;
				byte[] chunk = null;
				boolean isplayed = false;
				
				while(outputbuffer_isloop){
					int response = codec.dequeueOutputBuffer(bufferinfo, 1);
					if(response >= 0){
						int outputbuffer_index = response;
						ByteBuffer buffer = outputbuffer[outputbuffer_index];
						if(chunk == null || chunk.length < bufferinfo.size){
							chunk = new byte[bufferinfo.size];
						}
						buffer.position(bufferinfo.offset);
						buffer.get(chunk, 0, chunk.length);
//						if(bufferinfo.size > 0){
//							int remaining = chunk.length;
//							int written = 0;
//							int written_once;
//							while(true){
//								written_once = track.write(chunk, written, remaining);
//								written += written_once;
//								remaining -= written_once;
////Log.d("", remaining + ", " + written + ", " + written_once);
//								if(!isplayed && (remaining == 0 || written_once == 0)){
//									isplayed = true;
//									track.play();
////Log.d("", isplayed + "");
//								}
//								if(remaining == 0){
//									break;
//								}
//								try {
//									Thread.sleep(16);
//								} catch (Exception e) {
//									// TODO: handle exception
//									Log.e("", e.toString() + " in outputbuffer");
//								}
//							}
//						}
						updateWaveform(chunk);
						codec.releaseOutputBuffer(outputbuffer_index, false);
						if((bufferinfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
							break;
						}
						buffer.clear();
						
					}else if(response == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
//						outputbuffer = codec.getOutputBuffers();
						
					}else if(response == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
						int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
						int[] channelConfigs = {AudioFormat.CHANNEL_OUT_MONO, AudioFormat.CHANNEL_OUT_STEREO};
						int channelConfig = channelConfigs[format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) - 1];
						int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
						int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);

						track = new AudioTrack(AudioManager.STREAM_MUSIC,
															sampleRate,
															channelConfig,
															audioFormat,
															bufferSize,
															AudioTrack.MODE_STREAM);
						Log.d("", sampleRate + "Hz " + format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) + "ch buffer:" + bufferSize + "");
					}
				}
			}
			
		});
		
		waveform = null;
		waveform_index = -1;
		bpms = new HashMap<Integer, Integer>();
		
	}
	
	private void updateWaveform(byte[] waveform_sample){
    	if(waveform_index >= 0 && waveform_index < waveform.length){ 
    		for (int i = 0; i < waveform_sample.length && waveform_index + i < waveform.length; i++) { 
    			waveform[waveform_index + i] = waveform_sample[i]; 
    		}
        	waveform_index += waveform_sample.length; 
    	}else{
    		if(waveform != null){
        		updateBPM();    			
    		}
    		waveform = new byte[format.getInteger(format.KEY_SAMPLE_RATE)]; 
    		waveform_index = 0; 
    	}
	}
	
	private void updateBPM(){
		// ウェーブレット解析結果生成
		byte[] wavelet_w1 = new byte[waveform.length / 2];
		byte[] wavelet_s1 = new byte[waveform.length / 2];
		for (int j = 0; j < waveform.length / 2 - 1; j++) {
			int average = (waveform[j * 2] + waveform[j * 2 + 1]) / 2;
			wavelet_s1[j] = (byte)average;
			int difference = (waveform[j * 2] - waveform[j * 2 + 1]);
			wavelet_w1[j] = (byte)difference;
		}
		// 波形の差分をdevision個に分割して最大値、最小値を見る
		int division = 10;
		int range = wavelet_w1.length / division;
		int[] min_indexes = new int[division];
		byte[] min_values = new byte[division];
		int[] max_indexes = new int[division];
		byte[] max_values = new byte[division];
		byte values_min = Byte.MAX_VALUE;
		byte values_max = Byte.MIN_VALUE;
		for (int i = 0; i < wavelet_w1.length - range; i += range) {
			byte min = Byte.MAX_VALUE;
			int min_index = 0;
			byte max = Byte.MIN_VALUE;
			int max_index = 0;
			for (int j = 0; j < range; j++) {
				int index = i + j;
				byte value = wavelet_w1[index];
				if(min > value){
					min = value;
					min_index = index;
				}
				if(max < value){
					max = value;
					max_index = index;
				}
			}
			int index = i / range;
			min_indexes[index] = min_index;
			min_values[index] = min;
			max_indexes[index] = max_index;
			max_values[index] = max;
			if(values_min > min){
				values_min = min;
			}
			if(values_max < max){
				values_max = max;
			}
		}
		// 得られた最大値、最小値のうちしきい値を超えた有益なものを抽出
		double threshold = 0.4;
		double min_threshold = values_min * threshold;
		double max_threshold = values_max * threshold;
		int minimum_duration = wavelet_w1.length / division;
		ArrayList<Integer> beatmin_indexes = new ArrayList<Integer>();
		ArrayList<Integer> beatmax_indexes = new ArrayList<Integer>();
		for (int i = 0; i < min_indexes.length; i++) {
			if(min_values[i] < min_threshold){
				if(beatmin_indexes.isEmpty() || min_indexes[i] - beatmin_indexes.get(beatmin_indexes.size() - 1) >= minimum_duration){	//	精度向上の余地ありかも
					beatmin_indexes.add(min_indexes[i]);								
				}
			}
			if(max_values[i] > max_threshold){
				if(beatmax_indexes.isEmpty() || max_indexes[i] - beatmax_indexes.get(beatmax_indexes.size() - 1) >= minimum_duration){
					beatmax_indexes.add(max_indexes[i]);
				}
			}
		}
		// 有益な最大値、最小値の間隔を見てBPMを求める
		if(beatmin_indexes.size() == 0){
			beatmin_indexes.add(0);
		}
		if(beatmax_indexes.size() == 0){
			beatmax_indexes.add(0);
		}
		int[] bpmmin = new int[beatmin_indexes.size() - 1];
		int[] bpmmax = new int[beatmax_indexes.size() - 1];
		int maximum_bpm = 185;
		double msper1sample = 1000 / (format.getInteger(format.KEY_SAMPLE_RATE) / 2);	// 1085.9 は環境依存要素かもしれない。1000msを表している
		for (int i = 0; i < beatmin_indexes.size() - 1; i++) {
			int duration_sample = beatmin_indexes.get(i + 1) - beatmin_indexes.get(i);
			bpmmin[i] = (int)(60000 / (duration_sample * msper1sample));
			while(bpmmin[i] > maximum_bpm){
				bpmmin[i] /= 2;
			}
		}
		for (int i = 0; i < beatmax_indexes.size() - 1; i++) {
			int duration_sample = beatmax_indexes.get(i + 1) - beatmax_indexes.get(i);
			bpmmax[i] = (int)(60000 / (duration_sample * msper1sample));						
			while(bpmmax[i] > maximum_bpm){
				bpmmax[i] /= 2;
			}
		}
		// 求まったBPM候補をグローバル変数へ送る
		for (int i = 0; i < bpmmin.length; i++) {
			int key = bpmmin[i];
			if(bpms.containsKey(key)){
				int value = bpms.get(key);
				bpms.put(key, value + 1);				
			}else{
				bpms.put(key, 0);
			}
		}
	}
	
	public byte[] getWaveform(){
		byte[] waveform_sample = new byte[4096];	// 仮値、サンプル数の求め方不明
		for (int i = 0; i < waveform_sample.length; i++) {
			waveform_sample[i] = waveform[waveform_index - 4096];
		}
		return waveform_sample;
	}
	
	public int getBPM(){
		int max = Integer.MIN_VALUE;
		int max_index = -1;
		Integer[] keys = (Integer[]) new TreeSet(bpms.keySet()).toArray(new Integer[0]);
		for (int i = 0; i < keys.length; i++) {
			Integer count = bpms.get(keys[i]);
			if(max < count){
				max = count;
				max_index = keys[i];
			}
		}
		return max_index;
	}
	
	public double getDecodePercentage(){
		double current = extractor.getSampleTime();
		double duration = format.getLong(MediaFormat.KEY_DURATION);
		if(current < 0){
			current = duration;
		}
		return current / duration;
	}
	
	public void start(){
		inputbuffer_isloop = true;
		outputbuffer_isloop = true;
		inputbuffer_thread.start();
		outputbuffer_thread.start();		
	}
	
	public void stop(){
		inputbuffer_isloop = false;
		outputbuffer_isloop = false;
		try {
			inputbuffer_thread.join();
			outputbuffer_thread.join();
		} catch (Exception e) {
			// TODO: handle exception
			Log.e("", e.toString() + " in stop");
		}
		codec.stop();
		codec.release();
	}

}
