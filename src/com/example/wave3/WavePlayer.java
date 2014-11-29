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
	private Thread buffer_thread;
	private boolean buffer_isloop;
	private byte[] waveform;
	private byte[] waveform1000ms;
	private int waveform1000ms_index;
	private HashMap<Integer, Integer> bpms;

	//ここから赤木の追加分変数
	byte[] fft; //fftデータ格納用バイト型変数
	boolean Scale = false;//スケールが検出できているか、いないかの判定
	int max=0; //最大値座標格納用変数
	int cmj=0,cma=0,dmj=0,dma=0,emj=0,ema=0,fmj=0,fma=0,gmj=0,gma=0,amj=0,bmj=0,bma=0;//各スケール比率加点方式用変数,mjはメジャー,maはマイナー
	boolean a=false,b=false,c=false,d=false,e=false,f=false,g=false;//各音階判定用変数
	boolean as=false,cs=false,ds=false,fs=false,gs=false;//(追加分)音階がシャープの時用のフラグ、ド,レ,ファ,ソ,ラの5音のみシャープあり
	boolean majar=false,mainare=false; //メジャー、マイナーのフラグ、メジャーなら明るい、マイナーなら暗い曲として認定、各スケールに使用される特徴があるか調べてみる。
	//ここまで赤木の追加分変数
	
	public WavePlayer(String uri) throws IOException{
		// TODO 自動生成されたコンストラクター・スタブ
		extractor = new MediaExtractor();
		extractor.setDataSource(uri);
		extractor.selectTrack(extractor.getTrackCount() - 1);
		format = extractor.getTrackFormat(extractor.getTrackCount() - 1);
		String mimetype = format.getString(MediaFormat.KEY_MIME);

		codec = MediaCodec.createDecoderByType(mimetype);
		codec.configure(format, null, null, 0);
		codec.start();
		
		final ByteBuffer[] inputbuffer = codec.getInputBuffers();
		final ByteBuffer[] outputbuffer = codec.getOutputBuffers();
				
		buffer_thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO 自動生成されたメソッド・スタブ
				
				boolean isEOS = false;
				MediaCodec.BufferInfo bufferinfo = new MediaCodec.BufferInfo();
				AudioTrack track = null;
				byte[] chunk = null;
				boolean isplayed = false;

				while(buffer_isloop){
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
//						try {
//							Thread.sleep(1);
//						} catch (Exception e) {
//							// TODO: handle exception
//							Log.e("", e.toString() + " in buffer");
//						}
					}
				
					int response = codec.dequeueOutputBuffer(bufferinfo, 0);
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
//								if(!isplayed && (remaining == 0 || written_once == 0)){
//									isplayed = true;
//									track.play();
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
		waveform1000ms = null;
		waveform1000ms_index = -1;
		bpms = new HashMap<Integer, Integer>();
		
	}
	
	private void updateWaveform(byte[] waveform_sample){
		waveform = waveform_sample;
		if(waveform != null){
			updateFFT();			
		}
    	if(waveform1000ms_index >= 0 && waveform1000ms_index < waveform1000ms.length){ 
    		for (int i = 0; i < waveform_sample.length && waveform1000ms_index + i < waveform1000ms.length; i++) { 
    			waveform1000ms[waveform1000ms_index + i] = waveform_sample[i]; 
    		}
        	waveform1000ms_index += waveform_sample.length; 
    	}else{
    		if(waveform1000ms != null){
        		updateBPM();
    		}
    		waveform1000ms = new byte[format.getInteger(format.KEY_SAMPLE_RATE)]; 
    		waveform1000ms_index = 0; 
    	}
	}
	
	private void updateBPM(){
		// ウェーブレット解析結果生成
		byte[] wavelet_w1 = new byte[waveform1000ms.length / 2];
		byte[] wavelet_s1 = new byte[waveform1000ms.length / 2];
		for (int j = 0; j < waveform1000ms.length / 2 - 1; j++) {
			int average = (waveform1000ms[j * 2] + waveform1000ms[j * 2 + 1]) / 2;
			wavelet_s1[j] = (byte)average;
			int difference = (waveform1000ms[j * 2] - waveform1000ms[j * 2 + 1]);
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
		double msper1sample = 1000.0 / (format.getInteger(format.KEY_SAMPLE_RATE) / 2);	// 1085.9 は環境依存要素かもしれない。1000msを表している
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
		for (int i = 0; i < bpmmax.length; i++) {
			int key = bpmmax[i];
			if(bpms.containsKey(key)){
				int value = bpms.get(key);
				bpms.put(key, value + 1);				
			}else{
				bpms.put(key, 0);
			}
		}
	}
	
	//赤木追加分,ｆｆｔの処理をここにガリガリと書いていきます
	
	public void updateFFT()
	{
		if(waveform.length < 256){
			return;
		}
		
		// 1秒ごとの波形の最初の256サンプルだけを見る、とりあえず
		byte[] data = new byte[256];
		for (int i = 0; i < data.length; i++) {
			data[i] = waveform[i];
		}
		
		//小笠原さんのサンプルより
long time_b = System.currentTimeMillis();
		double [] real = new double[data.length]; // 実数部
		double [] imaginary = new double[data.length]; // 虚数部
		
		for (int n = 0; n < data.length; n++) {
			double ReF = 0.0, ImF = 0.0;
			for (int k = 0; k < data.length; k++) {
				ReF += data[k]*Math.cos(2*Math.PI*k*n/(data.length + 1));
				ImF += -data[k]*Math.sin(2*Math.PI*k*n/(data.length + 1));
			}
			real[n] = ReF;	// 実数部
			imaginary[n] = ImF;	// 虚数部
 		}
long time_a = System.currentTimeMillis();
Log.d("", (time_a - time_b) + "ms");
// double で 64516 回 → 27ms ぐらい
		
		// Android仕様に合わせる
		fft = new byte[real.length * 2 + 2];	// 512
		fft[0] = 0;
		fft[1] = 0;
		for (int i = 1; i < fft.length / 2; i++) {	// i = 1 to 256 
			double amplitude = Math.sqrt(Math.pow(real[i - 1], 2) + Math.pow(imaginary[i - 1], 2));
			if(amplitude > Byte.MAX_VALUE){
				amplitude = Byte.MAX_VALUE;
			}
			fft[i * 2] = (byte)amplitude;
			fft[i * 2 + 1] = (byte)amplitude;
		}
		
		//最大値検出処理
		if(Scale==false){
			byte max_value;
			byte tmp=0;
			for(int loop=2;loop<51;loop+=2)
			{
				
					if(tmp<fft[loop] /*&& (_clonefft[loop2]!=0 && _clonefft[loop2-2]!=0)*/)
					{
						/*byte tmp = _clonefft[loop2];
						_clonefft[loop2]=_clonefft[loop2-2];
						_clonefft[loop2-2]=tmp;*/
						tmp = fft[loop];
						max=loop;
						max_value = tmp;
					}
				//}
			}
			/*ミの判定、ミの周波数はこの番号に格納されているはず。*/
			/*maxが14「ミ」かつ「ミ」が127の強さを持っていた場合、また前後の周波数成分が50以下の場合、「ミ」と判断する*/
			while(max > 23){	// 倍音補正かけてみたり（雑 追記(赤木):三倍音などにも対応できるようにしてみました
				max = max / (max/23+1) ;
			}
			// 書き込みテストです、テスト
							// fft[max] > max_value * 0.5 で最大値50%みたいにしてみたり
			
			if(max==12 && ((double)fft[max-2]/(double)fft[max])<0.5 && ((double)fft[max+2]/(double)fft[max])<=0.3 && ((double)fft[max+4]/(double)fft[max])<0.2 && cs==false)//ド#の検出
			{
				Log.d("音階","ド#");cs=true;dmj++;emj++;fma++;amj++;bmj++;bma++;
			}
			if(max==12 && ((double)fft[max-2]/(double)fft[max])<0.5 && ((double)fft[max+2]/(double)fft[max])>=0.5 && ((double)fft[max+2]/(double)fft[max])<=0.75 && ((double)fft[max+4]/(double)fft[max])>=0.2 && d==false)//レの検出
			{
				Log.d("音階","レ");d=true;cmj++;cma++;dmj++;ema++;fmj++;gmj++;gma++;bma++;
			}
			if(max==12 && ((double)fft[max-2]/(double)fft[max])>0.39 && ((double)fft[max+2]/(double)fft[max])>=0.9 && ((double)fft[max+4]/(double)fft[max])>0.35 && ds==false )//レ#の検出
			{
				Log.d("音階","レ#");ds=true;cma++;emj++;fma++;gma++;bmj++;
			}
			if(max==14 && ((double)fft[max-2]/(double)fft[max])<=0.15 && e==false)//ミの検出
			{
				Log.d("音階","ミ");e=true;cmj++;dmj++;dma++;emj++;ema++;fmj++;gmj++;amj++;bmj++;bma++;
			}
			if(max==16 && ((double)fft[max-2]/(double)fft[max])>=0.7 && ((double)fft[max+2]/(double)fft[max]) >= 0.40 && f==false)//ファの検出
			{
				Log.d("音階","ファ");f=true;cmj++;cma++;dma++;fmj++;fma++;gma++;
			}
			if(max==16 && ((double)fft[max-2]/(double)fft[max])<=0.5 && ((double)fft[max+2]/(double)fft[max])<=0.5 && fs==false)//ファ#の検出
			{
				Log.d("音階","ファ#");fs=true;dmj++;emj++;ema++;gmj++;amj++;bmj++;bma++;
			}
			if(max==18 && ((double)fft[max-2]/(double)fft[max])>=0.50 && ((double)fft[max+2]/(double)fft[max]) <= 0.50 && g==false)//ソの検出
			{
				Log.d("音階","ソ");g=true;cmj++;cma++;dmj++;dma++;ema++;fmj++;fma++;gmj++;gma++;bma++;
			}
			if(max==18 && ((double)fft[max+2]/(double)fft[max])<=0.30 && ((double)fft[max+2]/(double)fft[max])<0.5 && gs==false)//ソ#の検出
			{
				Log.d("音階","ソ#");gs=true;cma++;emj++;fma++;amj++;bmj++;
			}
			if(max==20 && ((double)fft[max-2]/(double)fft[max])>=0.5 && ((double)fft[max+2]/(double)fft[max])<0.5 && a==false)//ラの検出
			{
				Log.d("音階","ラ");a=true;cmj++;dmj++;dma++;emj++;ema++;fmj++;gmj++;gma++;amj++;bma++;
			}
			if(max==20 && ((double)fft[max-2]/(double)fft[max])<0.5 && ((double)fft[max+2]/(double)fft[max])<0.5 && as==false )//ラ#の検出
			{
				Log.d("音階","ラ#");as=true;cma++;dma++;emj++;ema++;fmj++;gma++;bmj++;
			}
			if(max==22 && ((double)fft[max-2]/(double)fft[max])>=0.5 && ((double)fft[max+2]/(double)fft[max])<=0.5 && b==false )//シの検出
			{
				Log.d("音階","シ");b=true;cmj++;dmj++;emj++;ema++;gmj++;amj++;bmj++;bma++;
			}
			if(max==22 && ((double)fft[max-2]/(double)fft[max])<=0.3 && c==false )//ドの検出
			{
				Log.d("音階","ド");c=true;cmj++;cma++;dma++;ema++;fmj++;fma++;gmj++;gma++;dmj--;
			}
			// Log.d("", (c ? "C" : "-") + (d ? "D" : "-") + (e ? "E" : "-") + (f ? "F" : "-") + (g ? "G" : "-") + (a ? "A" : "-") + (b ? "B" : "-") + );

			//各種スケールの検出を行う、取得された音階を参考にスケールの判定を行う
			if(Scale!=true){

				if(cmj==6)
				{
					Log.d("スケール","Cメジャースケール");
					majar = true;
					Scale=true;
				}
				if(cma==6)
				{
					Log.d("スケール","Cマイナースケール");
					mainare=true;
					Scale=true;
				}
				//ここまでCスケール
				//ここからDスケール
				if(dmj==6)
				{
					Log.d("スケール","Dメジャースケール");
					majar = true;
					Scale = true;
				}
				if(dma==6)
				{
					Log.d("スケール","Dマイナースケール");
					mainare = true;
					Scale = true;
				}
				//ここまでDスケール
				//ここからEスケール
				if(emj==6)
				{
					Log.d("スケール","Dメジャースケール");
					majar = true;
					Scale = true;
				}
				if(ema==6)
				{
					Log.d("スケール","Dマイナースケール");
					mainare = true;
					Scale = true;
				}
				//ここまでEスケール
				//ここからFスケール
				if(fmj==6)
				{
					Log.d("スケール","Dメジャースケール");
					majar = true;
					Scale = true;
				}
				if(fma==6)
				{
					Log.d("スケール","Dマイナースケール");
					mainare = true;
					Scale = true;
				}
				//ここからGスケール
				if(gmj==6)
				{
					Log.d("スケール","Dメジャースケール");
					majar = true;
					Scale = true;
				}
				if(gma==6)
				{
					Log.d("スケール","Dマイナースケール");
					mainare = true;
					Scale = true;
				}
				//ここからaスケール
				if(amj==6)
				{
					Log.d("スケール","Dメジャースケール");
					majar = true;
					Scale = true;
				}
				//ここからbスケール
				if(bmj==6)
				{
					Log.d("スケール","Dメジャースケール");
					majar = true;
					Scale = true;
				}
				if(bma==6)
				{
					Log.d("スケール","Dマイナースケール");
					mainare = true;
					Scale = true;
				}
			}
		}
	}
	
	//追加分、ここで終了
	
	public String getCode(){
		// 遠藤による提案：これで外部からアクセスできると楽しい
		return "";
	}
	
	public byte[] getWaveform(){
		byte[] waveform_sample = waveform;
		return waveform_sample;
	}
	
	public byte[] getFFT(){
		return fft;
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
		buffer_isloop = true;
		buffer_thread.start();
	}
	
	public void stop(){
		buffer_isloop = false;
		try {
			buffer_thread.join();
		} catch (Exception e) {
			// TODO: handle exception
			Log.e("", e.toString() + " in stop");
		}
		codec.stop();
		codec.release();
	}

}
