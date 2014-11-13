package com.example.wave3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import android.util.Log;

public class WaveAnalyzer implements Runnable {
	
	byte[] waveform1000ms;
	int waveform1000ms_index;
	int[] bpms;
	int bpms_index;
	int samplingRate;
	
	public WaveAnalyzer(int samplingRate) {
		// TODO 自動生成されたコンストラクター・スタブ
		this.samplingRate = samplingRate;
		
		waveform1000ms = null;
		waveform1000ms_index = -1;
		bpms = null;
		bpms_index = -1;
	}
	
    // ウェーブレット変換    
    private byte[] invoke(byte[] input, byte[] outputs, byte[] outputw){
		for (int j = 0; j < input.length / 2 - 1; j++) {
			int average = (input[j * 2] + input[j * 2 + 1]) / 2;
			outputs[j] = (byte)average;
			int difference = (input[j * 2] - input[j * 2 + 1]);
			outputw[j] = (byte)difference;
		}
    	return outputs;
    }
    
    // 最頻値を求める
    private int mode(int[] input){
    	HashMap<Integer, Integer> counter = new HashMap<Integer, Integer>();
		int mode_count = -1;
		int mode = 0;
		for (int i = 0; i < input.length; i++) {
			Integer count = counter.get(input[i]);
			if(count != null){
				count++;
			}else{
				count = 1;
			}
			counter.put(input[i], count);
			if(mode_count < count){
				mode_count = count;
				mode = input[i];
			}
		}
		if(mode_count == 1){
			mode = 0;//average(input);
		}
		// デバッグ用、ヒストグラムぽいものをLogCatに表示します
//		Integer[] keys = (Integer[]) new TreeSet(counter.keySet()).toArray(new Integer[0]);
//		Log.d("", "--------------------");
//		for (int i = 0; i < keys.length; i++) {
//			Integer count = counter.get(keys[i]);
//			if(count > 1){
//			String count_str = "";
//			for (int j = 0; j < count; j++) {
//				count_str += "*";
//			}
//			Log.d("", keys[i] + "" + count_str);
//			}
//		}
//		Log.d("", "--------------------");
		return mode;
    }
    
    public void updateWaveform(byte[] waveform){
//    	if(isupdate){
//        	this.waveform = waveform;
        	if(waveform1000ms_index >= 0 && waveform1000ms_index < waveform1000ms.length){
            	for (int i = 0; i < waveform.length && waveform1000ms_index + i < waveform1000ms.length; i++) {
        			waveform1000ms[waveform1000ms_index + i] = waveform[i];
        		}
            	waveform1000ms_index += waveform.length;
        	}else{
        		waveform1000ms = new byte[samplingRate];
        		waveform1000ms_index = 0;
        	}
//    	}
    }
    
    private void updateBPM(int[] bpm){
		if(bpms_index >= 0 && bpms_index < bpms.length){
			for (int i = 0; i < bpm.length && bpms_index + i < bpms.length; i++) {
				bpms[bpms_index + i] = bpm[i];
			}
			bpms_index += bpm.length;
		}else{
			bpms = new int[50];
			bpms_index = 0;
		}
    }
    
    @Override
	public void run() {
		// TODO 自動生成されたメソッド・スタブ
		// ウェーブレット解析結果生成
		byte[] wavelet_w1 = new byte[waveform1000ms.length / 2];
		byte[] wavelet_s1 = new byte[waveform1000ms.length / 2];
		invoke(waveform1000ms, wavelet_s1, wavelet_w1);

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
				if(beatmin_indexes.isEmpty() || min_indexes[i] - beatmin_indexes.get(beatmin_indexes.size() - 1) >= minimum_duration){
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
		double msper1sample = 1085.9 / samplingRate;	// 1085.9 は環境依存要素かもしれない。1000msを表している
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
//		int[] beatmin_indexes_int = new int[beatmin_indexes.size()];
//		for (int i = 0; i < beatmin_indexes.size(); i++) {
//			beatmin_indexes_int[i] = beatmin_indexes.get(i);
//		}
		updateBPM(bpmmin);
//		int[] beatmax_indexes_int = new int[beatmax_indexes.size()];
//		for (int i = 0; i < beatmax_indexes.size(); i++) {
//			beatmax_indexes_int[i] = beatmax_indexes.get(i);
//		}
		updateBPM(bpmmax);
	}

}
