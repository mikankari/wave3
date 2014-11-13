package com.example.wave3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.view.MotionEvent;
import android.view.View;

public class WaveView extends View{

	MediaPlayer player;
	Visualizer visualizer;
	WaveAnalyzer analyzer;
	boolean isupdate;
	
	byte[] waveform2;
	
	public WaveView(Context context){
		super(context);
		
//		this.player = player;
//    	visualizer = new Visualizer(player.getAudioSessionId());
//    	visualizer.setEnabled(false);
//    	visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
//    	visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
//			@Override
//			public void onWaveFormDataCapture(Visualizer visualizer, final byte[] waveform, int samplingRate) {
//				waveform2 = new byte[waveform.length];
//				for (int i = 0; i < waveform.length; i++) {
//					waveform2[i] = (byte)(waveform[i] + 128);
//				}
//				(new Thread(new Runnable() {
//					@Override
//					public void run() {
//						// TODO �����������ꂽ���\�b�h�E�X�^�u
//						analyzer.updateWaveform(waveform2);
//					}
//				})).start();
//				
//				// �g�`�̌`�ɂ��Ă���
////				for (int i = 0; i < waveform.length; i++) {
////					waveform[i] += 128;
////				}
////				updateWaveform(waveform);
//
////				analyzer.updateWaveform(waveform);
//			}
//			
//			@Override
//			public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
//			}
//		},
//		Visualizer.getMaxCaptureRate(),
//		true, false);	// waveform, fft
//    	visualizer.setEnabled(true);
//    	analyzer = new WaveAnalyzer(visualizer.getSamplingRate() / 1000);

    	isupdate = true;
    }

    

    // �`�揈��
    public void onDraw(Canvas canvas){
		Paint paint = new Paint();
		if(!isupdate){
			canvas.drawText("update pausing", 0, 20, paint);
		}
//		if(waveform2 != null){
//			for (int i = 0; i < waveform2.length; i++) {
//				waveform2[i] += 128;
//			}
//			
//		}
//		drawArray(canvas, "waveform", waveform2, 1, (int)(getHeight() * 0.25));
//		drawArray(canvas, "haar wavelet average", analyzer.waveform1000ms, 1, (int)(getHeight() * 0.50));
//		drawArray(canvas, "haar wavelet diff", wavelet_w1, 1, (int)(getHeight() * 0.75));
//		if(beatmin_indexes != null){
//			paint.setColor(Color.BLUE);
//			for (int i = 0; i < beatmin_indexes.length; i++) {
//				int startX = getWidth() * beatmin_indexes[i] / wavelet_w1.length;
//				int startY = (int)(getHeight() * 0.75);
//				int stopX = startX;
//				int stopY = startY + 64;
//				canvas.drawLine(startX, startY, stopX, stopY, paint);	
//			}
//			String bpmmin_join = "";
//			for (int i = 0; i < bpmmin.length; i++) {
//				bpmmin_join += bpmmin[i] + ", ";
//			}
//			canvas.drawText(bpmmin_join, 0, (int)(getHeight() * 0.95), paint);
//		}
//		if(beatmax_indexes != null){
//			paint.setColor(Color.RED);
//			for (int i = 0; i < beatmax_indexes.length; i++) {
//				int startX = getWidth() * beatmax_indexes[i] / wavelet_w1.length;
//				int startY = (int)(getHeight() * 0.75);
//				int stopX = startX;
//				int stopY = startY - 64;
//				canvas.drawLine(startX, startY, stopX, stopY, paint);	
//			}	
//			String bpmmax_join = "";
//			for (int i = 0; i < bpmmax.length; i++) {
//				bpmmax_join += bpmmax[i] + ", ";
//			}
//			canvas.drawText(bpmmax_join, 0, (int)(getHeight() * 0.900), paint);
//		}
//		drawParam(canvas, "BPM mode", bpm_mode, (int)(getHeight() * 0.925));
    
    	// �A�����ĕ`�悷��
		invalidate();
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
        }
    }
    
    // �g�`���`���܂�
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
    
    // int�^�p�����[�^���`���܂�
    private void drawParam(Canvas canvas, String label, int param, int zero_y){
    	Paint paint = new Paint();
    	String param_str;
    	if(param >= 0){
    		param_str = "" + param;
    	}else{
    		param_str = "analyzing...";
    	}
    	canvas.drawText(param_str, 0, zero_y, paint);    		
    }
    
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_UP){
        	isupdate = !isupdate;
//        	visualizer.setEnabled(isupdate);        		
        }

        return true;
    }
    
}


