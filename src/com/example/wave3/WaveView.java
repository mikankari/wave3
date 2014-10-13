package com.example.wave3;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class WaveView extends View{

	MediaPlayer player;
	Visualizer visualizer;
	byte[] waveform;
	byte[] waveform1000ms;
	int waveform1000ms_index;
	byte[] wavelet_s1;
	byte[] wavelet_w1;
	int[] beatmin_indexes;
	int[] beatmax_indexes;
	int[] bpmmin;
	int[] bpmmax;
	boolean isupdate;

	public WaveView(Context context, MediaPlayer player){
		super(context);
		
		this.player = player;
    	visualizer = new Visualizer(player.getAudioSessionId());
    	visualizer.setEnabled(false);
    	visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
    	visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
			@Override
			public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
				// 波形の形にしている
				for (int i = 0; i < waveform.length; i++) {
					waveform[i] += 128;
				}
				updateWaveform(waveform);

				// ウェーブレット解析結果生成
				if(waveform1000ms_index >= waveform1000ms.length){
					wavelet_w1 = new byte[waveform1000ms.length / 2];
					wavelet_s1 = new byte[waveform1000ms.length / 2];
					invoke(waveform1000ms, wavelet_s1, wavelet_w1);

					// 波形の差分をdevision個に分割して最大値、最小値を見る
					int division = 20;
					int range = wavelet_w1.length / division;
					int[] min_indexes = new int[division];
					byte[] min_values = new byte[division];
					int[] max_indexes = new int[division];
					byte[] max_values = new byte[division];
					byte values_min = Byte.MAX_VALUE;
					byte values_max = Byte.MIN_VALUE;
					for (int i = 0; i < wavelet_w1.length; i += range) {
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
					double threshold = 0.5;
					double min_threshold = values_min * 0.8;
					double max_threshold = values_max * 0.8;
					ArrayList<Integer> beatmin_indexes = new ArrayList<Integer>();
					ArrayList<Integer> beatmax_indexes = new ArrayList<Integer>();
					for (int i = 0; i < min_indexes.length; i++) {
						if(min_values[i] < min_threshold){
							beatmin_indexes.add(min_indexes[i]);
						}
						if(max_values[i] > max_threshold){
							beatmax_indexes.add(max_indexes[i]);
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
					double msper1sample = 1085.9 / (visualizer.getSamplingRate() / 2 / 1000);
					for (int i = 0; i < beatmin_indexes.size() - 1; i++) {
						int duration_sample = beatmin_indexes.get(i + 1) - beatmin_indexes.get(i);
						bpmmin[i] = (int)(60000 / (duration_sample * msper1sample));
					}
					for (int i = 0; i < beatmax_indexes.size() - 1; i++) {
						int duration_sample = beatmax_indexes.get(i + 1) - beatmax_indexes.get(i);
						bpmmax[i] = (int)(60000 / (duration_sample * msper1sample));						
					}
					// グローバル変数へ送る
					int[] beatmin_indexes_int = new int[beatmin_indexes.size()];
					for (int i = 0; i < beatmin_indexes.size(); i++) {
						beatmin_indexes_int[i] = beatmin_indexes.get(i);
					}
					for (int i = 0; i < bpmmin.length; i++) {
						while(bpmmin[i] > 300){
							bpmmin[i] /= 2;
						}
					}
					updateBPM("min", beatmin_indexes_int, bpmmin);
					int[] beatmax_indexes_int = new int[beatmax_indexes.size()];
					for (int i = 0; i < beatmax_indexes.size(); i++) {
						beatmax_indexes_int[i] = beatmax_indexes.get(i);
					}
					for (int i = 0; i < bpmmax.length; i++) {
//						while(bpmmax[i] > 300){
							bpmmax[i] /= 2;
//						}
					}
					updateBPM("max", beatmax_indexes_int, bpmmax);
				}
			}
			
			@Override
			public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
				// fft[0]:			直流成分の値（――、すなわち0Hz）
				// fft[1]:			サンプリング周波数の半分の実部
				// fft[2 * i]:		交流成分の実部（sinみたいな～～）
				// fft[2 * i + 1]:	交流成分の虚部（cosみたいな～～）
				// ここでは実部と虚部を計算済みの値にしている
	    		for (int i = 1; i < fft.length / 2; i++) {
	    			double amplitude = Math.sqrt(Math.pow(fft[i * 2], 2) + Math.pow(fft[i * 2 + 1], 2));
	    			if(amplitude > Byte.MAX_VALUE){
	    				amplitude = Byte.MAX_VALUE;
	    			}
	    			fft[i * 2] = (byte)amplitude;
	    			fft[i * 2 + 1] = (byte)amplitude;
	    		}
				updateFFT(fft);
			}
		},
		Visualizer.getMaxCaptureRate(),
		true, false);	// waveform, fft
    	visualizer.setEnabled(true);
    	
    	waveform = null;
    	waveform1000ms = null;
    	waveform1000ms_index = -1;
    	wavelet_s1 = null;
    	wavelet_w1 = null;
    	beatmin_indexes = null;
    	beatmax_indexes = null;
    	bpmmin = null;
    	bpmmax = null;
    	isupdate = true;
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
    
    public void onDraw(Canvas canvas){
		Paint paint = new Paint();
		if(!isupdate){
			canvas.drawText("update pausing", 0, 20, paint);
		}
		drawArray(canvas, "waveform1000ms", waveform1000ms, 1, (int)(getHeight() * 0.25));
		drawArray(canvas, "haar wavelet average", wavelet_s1, 1, (int)(getHeight() * 0.50));
		drawArray(canvas, "haar wavelet diff", wavelet_w1, 1, (int)(getHeight() * 0.75));
		if(beatmin_indexes != null){
			paint.setColor(Color.BLUE);
			for (int i = 0; i < beatmin_indexes.length; i++) {
				int startX = 480 * beatmin_indexes[i] / 44100;
				int startY = (int)(getHeight() * 0.75);
				int stopX = startX;
				int stopY = startY + 64;
				canvas.drawLine(startX, startY, stopX, stopY, paint);	
			}
			String bpmmin_join = "";
			for (int i = 0; i < bpmmin.length; i++) {
				bpmmin_join += bpmmin[i] + ", ";
			}
			canvas.drawText(bpmmin_join, 0, (int)(getHeight() * 0.95), paint);
		}
		if(beatmax_indexes != null){
			paint.setColor(Color.RED);
			for (int i = 0; i < beatmax_indexes.length; i++) {
				int startX = 480 * beatmax_indexes[i] / 44100;
				int startY = (int)(getHeight() * 0.75);
				int stopX = startX;
				int stopY = startY - 64;
				canvas.drawLine(startX, startY, stopX, stopY, paint);	
			}	
			String bpmmax_join = "";
			for (int i = 0; i < bpmmax.length; i++) {
				bpmmax_join += bpmmax[i] + ", ";
			}
			canvas.drawText(bpmmax_join, 0, (int)(getHeight() * 0.90), paint);
		}
    
    	// 連続して描画する
		invalidate();
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
        }
    }
    
    private void drawArray(Canvas canvas, String label, byte[] array, int division, int zero_y){
    	Paint paint = new Paint();
    	String length_label = "";
        int width = getWidth();
        if(array != null){
            for (int i = division != 2 ? 0 : 1; i < array.length / division; i++) {
            	int x1 = width * i / (array.length / division);
            	int y1 = zero_y;
            	int x2 = x1;
            	int y2 = zero_y - array[i * division];
    	        canvas.drawLine(x1, y1, x2, y2, paint);				
            }
            length_label = "" + array.length / division;
        }
        canvas.drawText(label + " " + length_label, 0, zero_y - 64, paint);
        canvas.drawLine(0, zero_y, width, zero_y, paint);    	        	
    }
    
    private void drawParam(Canvas canvas, String label, int param, int zero_y){
    	Paint paint = new Paint();
    	if(param <= 0){
        	canvas.drawText("" + param, 0, zero_y, paint);    		
    	}
    }
    
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_UP){
        	isupdate = !isupdate;
        	visualizer.setEnabled(isupdate);        		
        }

        return true;
    }

    public void updateWaveform(byte[] waveform){
    	if(isupdate){
        	this.waveform = waveform;
        	if(waveform1000ms_index >= 0 && waveform1000ms_index < waveform1000ms.length){
            	for (int i = 0; i < waveform.length && waveform1000ms_index + i < waveform1000ms.length; i++) {
        			waveform1000ms[waveform1000ms_index + i] = waveform[i];
        		}
            	waveform1000ms_index += waveform.length;
        	}else{
        		waveform1000ms = new byte[visualizer.getSamplingRate() / 1000 * 2];
        		waveform1000ms_index = 0;
        	}
    	}
    }
    
    public void updateWavelet(byte[] wavelet){
//    	if(isupdate){
//        	this.wavelet = wavelet;    		
//    	}
    }
    
    public void updateFFT(byte[] fft){
//    	if(isupdate){
//        	this.fft = fft;    		
//    	}
    }
    
    public void updateBPM(String type, int[] beat_indexes, int[] bpm){
    	if(type.equals("min")){
    		this.beatmin_indexes = beat_indexes;
    		this.bpmmin = bpm;
    	}else if(type.equals("max")){
    		this.beatmax_indexes = beat_indexes;
    		this.bpmmax = bpm;
    	}
    }
    
}


