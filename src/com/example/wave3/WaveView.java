package com.example.wave3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
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
//						// TODO Ž©“®¶¬‚³‚ê‚½ƒƒ\ƒbƒhEƒXƒ^ƒu
//						analyzer.updateWaveform(waveform2);
//					}
//				})).start();
//				
//				// ”gŒ`‚ÌŒ`‚É‚µ‚Ä‚¢‚é
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

    

    // •`‰æˆ—
    public void onDraw(Canvas canvas){
		Paint paint = new Paint();
		if(!isupdate){
			canvas.drawText("update pausing", 0, 20, paint);
		}

		WavePlayer player = ((WaveActivity)getContext()).player;
		drawArray(canvas, "waveform", player.getWaveform(), 1, (int)(getHeight() * 0.25));
		drawParam(canvas, "%", (int)(player.getDecodePercentage() * 100), (int)(getHeight() * 0.50));
		paint.setColor(Color.BLACK);
		canvas.drawLine(0, (int)(getHeight() * 0.50), (int)(player.getDecodePercentage() * getWidth()), (int)(getHeight() * 0.50), paint);
    
    	// ˜A‘±‚µ‚Ä•`‰æ‚·‚é
		invalidate();
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
        }
    }
    
    // ”gŒ`‚ª•`‚¯‚Ü‚·
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
    
    // intŒ^ƒpƒ‰ƒ[ƒ^‚ª•`‚¯‚Ü‚·
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


