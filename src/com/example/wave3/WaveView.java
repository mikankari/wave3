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
	byte[] wavelethaar_s1;	// �n�[���̃E�F�[�u���b�g�ϊ��A����
	byte[] wavelethaar_w1;	// �n�[���̃E�F�[�u���b�g�ϊ��A����
	byte[] waveletdaubechie_s1;	// �h�x�V�B�̃E�F�[�u���b�g�ϊ��A����
	byte[] waveletdaubechie_w1;	// �h�x�V�B�̃E�F�[�u���b�g�ϊ��A����
	int bpm;
	ArrayList<Integer> beatmin_indexes;
	ArrayList<Integer> beatmax_indexes;
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
				// �g�`�̌`�ɂ��Ă���
				for (int i = 0; i < waveform.length; i++) {
					waveform[i] += 128;
				}
				updateWaveform(waveform);

				// �E�F�[�u���b�g��͌��ʐ���
				if(waveform1000ms_index >= waveform1000ms.length){
					wavelethaar_w1 = new byte[waveform1000ms.length / 2];
					wavelethaar_s1 = invoke1(waveform1000ms, wavelethaar_w1);
					waveletdaubechie_w1 = new byte[waveform1000ms.length / 2];
					waveletdaubechie_s1 = invoke2(waveform1000ms, waveletdaubechie_w1);
					
					// �g�`�̍�����devision�ɕ������čő�l�A�ŏ��l������
					int division = 20;
					int range = wavelethaar_w1.length / division;
					int[] min_indexes = new int[division];
					byte[] min_values = new byte[division];
					int[] max_indexes = new int[division];
					byte[] max_values = new byte[division];
					byte values_min = Byte.MAX_VALUE;
					byte values_max = Byte.MIN_VALUE;
					for (int i = 0; i < wavelethaar_w1.length; i += range) {
						byte min = Byte.MAX_VALUE;
						int min_index = 0;
						byte max = Byte.MIN_VALUE;
						int max_index = 0;
						for (int j = 0; j < range; j++) {
							int index = i + j;
							byte value = wavelethaar_w1[index];
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
					// ����ꂽ�ő�l�A�ŏ��l�̂����������l�𒴂����L�v�Ȃ��̂𒊏o
					double threshold = 0.8;
					double min_threshold = values_min * 0.8;
					double max_threshold = values_max * 0.8;
					/*ArrayList<Integer> */beatmin_indexes = new ArrayList<Integer>();
					/*ArrayList<Integer> */beatmax_indexes = new ArrayList<Integer>();
					for (int i = 0; i < min_indexes.length; i++) {
						if(min_values[i] < min_threshold){
							beatmin_indexes.add(min_indexes[i]);
						}
						if(max_values[i] > max_threshold){
							beatmax_indexes.add(max_indexes[i]);
						}
					}
					// �L�v�ȍő�l�A�ŏ��l�̊Ԋu������BPM�����߂�
					int[] bpmmin = new int[beatmin_indexes.size() - 1];
					int[] bpmmax = new int[beatmax_indexes.size() - 1];
					double msper1sample = 1000.0 / (visualizer.getSamplingRate() / 2 / 1000);
					for (int i = 0; i < beatmin_indexes.size() - 1; i++) {
						int duration_sample = beatmin_indexes.get(i + 1) - beatmin_indexes.get(i);
						bpmmin[i] = (int)(60000 / (duration_sample * msper1sample));
					}
					for (int i = 0; i < beatmax_indexes.size() - 1; i++) {
						int duration_sample = beatmax_indexes.get(i + 1) - beatmax_indexes.get(i);
						bpmmax[i] = (int)(60000 / (duration_sample * msper1sample));						
					}
String test = "";
for (int i = 0; i < bpmmax.length; i++) {
	test += bpmmax[i] + ", ";
}
Log.d("test", test);
				}
			}
			
			@Override
			public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
				// fft[0]:			���������̒l�i�\�\�A���Ȃ킿0Hz�j
				// fft[1]:			�T���v�����O���g���̔����̎���
				// fft[2 * i]:		�𗬐����̎����isin�݂����ȁ`�`�j
				// fft[2 * i + 1]:	�𗬐����̋����icos�݂����ȁ`�`�j
				// �����ł͎����Ƌ������v�Z�ς݂̒l�ɂ��Ă���
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
    	wavelethaar_s1 = null;
    	wavelethaar_w1 = null;
    	waveletdaubechie_s1 = null;
    	waveletdaubechie_w1 = null;
    	beatmin_indexes = null;
    	beatmax_indexes = null;
    	bpm = -1;
    	isupdate = true;
    }

    // �E�F�[�u���b�g�ϊ�    
    private byte[] invoke1(byte[] input, byte[] outputw){
    	// Haar Wevelet
		byte[] outputs = new byte[input.length / 2];
		for (int j = 0; j < input.length / 2 - 1; j++) {
			int average = (input[j * 2] + input[j * 2 + 1]) / 2;
			outputs[j] = (byte)average;
			int difference = (input[j * 2] - input[j * 2 + 1]);
			outputw[j] = (byte)difference;
		}
    	return outputs;
    }
    
    private byte[] invoke2(byte[] input, byte[] outputw){
		// Daubechie Wavelet
		double[] daubechiep = {0.707106781, 0.707106781};	// N=1
//		double[] daubechiep = {0.230377813, 0.714846570, 0.630880767, -0.027983769,
//								-0.187034811, 0.030841381, 0.032883011, -0.010597401};	// N=4
//		double[] daubechiep = {0.026670057, 0.188176800, 0.527201188, 0.688459039, 0.281172343,
//								-0.249846424, -0.195946274, 0.127369340, 0.093057364, -0.071394147,
//								-0.029457536, 0.033212674, 0.003606553, -0.010733175, 0.001395351,
//								0.001992405, -0.000685856, -0.000116466, 0.000093588, -0.000013264};	// N=10
		double[] daubechieq = {0.707106781, -0.707106781};	// N=1
//		double[] daubechieq = {0.010597401, -0.032883011, 0.030841381, -0.187034811,
//								0.027983769, -0.630880767, 0.714846570, -0.230377813};	// N=4
//		double[] daubechieq = {-0.000013264, -0.000093588, -0.000116466, 0.000685856, 0.001992405,
//								-0.001395351, -0.010733175, -0.003606553, 0.033212674, 0.029457536,
//								-0.071394147, -0.093057364, 0.127369340, 0.195946274, -0.249846424,
//								-0.281172343, 0.688459039, -0.527201188, 0.188176800, -0.026670057};	// N=10
		byte[] outputs = new byte[input.length / 2];
		for (int i = 0; i < input.length / 2; i++) {
			outputs[i] = 0;
			outputw[i] = 0;
			for (int j = 0; j < daubechiep.length; j++) {
				int index = (j + 2 * i) % input.length;
				outputs[i] += daubechiep[j] * input[index];
				outputw[i] += daubechieq[j] * input[index];
			}
		}
		return outputs;
    }

    public void onDraw(Canvas canvas){
		Paint paint = new Paint();
		if(!isupdate){
			canvas.drawText("update pausing", 0, 20, paint);
		}
		drawArray(canvas, "waveform1000ms", waveform1000ms, 1, (int)(getHeight() * 0.17));
		drawArray(canvas, "haar wavelet average", wavelethaar_s1, 1, (int)(getHeight() * 0.34));
		drawArray(canvas, "daubechie wavelet average", waveletdaubechie_s1, 1, (int)(getHeight() * 0.51));
		drawArray(canvas, "haar wavelet diff", wavelethaar_w1, 1, (int)(getHeight() * 0.68));
		drawArray(canvas, "daubechie wavelet diff", waveletdaubechie_w1, 1, (int)(getHeight() * 0.85));
if(beatmin_indexes != null){
	paint.setColor(Color.BLUE);
	for (int i = 0; i < beatmin_indexes.size(); i++) {
		int startX = 480 * beatmin_indexes.get(i) / 44100;
		int startY = (int)(getHeight() * 0.68);
		int stopX = startX;
		int stopY = (int)(getHeight() * 0.68) + 64;
		canvas.drawLine(startX, startY, stopX, stopY, paint);	
	}
}
if(beatmax_indexes != null){
	paint.setColor(Color.RED);
	for (int i = 0; i < beatmax_indexes.size(); i++) {
		int startX = 480 * beatmax_indexes.get(i) / 44100;
		int startY = (int)(getHeight() * 0.68);
		int stopX = startX;
		int stopY = (int)(getHeight() * 0.68) - 64;
		canvas.drawLine(startX, startY, stopX, stopY, paint);	
	}	
}
		drawParam(canvas, waveform1000ms_index + " / ", waveform1000ms != null ? waveform1000ms.length : 0, (int)(getHeight() * 0.1));
		drawParam(canvas, "bpm", bpm, (int)(getHeight() * 0.9));
    
    	// �A�����ĕ`�悷��
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
    
}


