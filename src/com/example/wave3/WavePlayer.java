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

	//��������Ԗ؂̒ǉ����ϐ�
	byte[] fft; //fft�f�[�^�i�[�p�o�C�g�^�ϐ�
	boolean Scale = false;//�X�P�[�������o�ł��Ă��邩�A���Ȃ����̔���
	int max=0; //�ő�l���W�i�[�p�ϐ�
	int cmj=0,cma=0,dmj=0,dma=0,emj=0,ema=0,fmj=0,fma=0,gmj=0,gma=0,amj=0,bmj=0,bma=0;//�e�X�P�[���䗦���_�����p�ϐ�,mj�̓��W���[,ma�̓}�C�i�[
	boolean a=false,b=false,c=false,d=false,e=false,f=false,g=false;//�e���K����p�ϐ�
	boolean as=false,cs=false,ds=false,fs=false,gs=false;//(�ǉ���)���K���V���[�v�̎��p�̃t���O�A�h,��,�t�@,�\,����5���̂݃V���[�v����
	boolean majar=false,mainare=false; //���W���[�A�}�C�i�[�̃t���O�A���W���[�Ȃ疾�邢�A�}�C�i�[�Ȃ�Â��ȂƂ��ĔF��A�e�X�P�[���Ɏg�p�������������邩���ׂĂ݂�B
	//�����܂ŐԖ؂̒ǉ����ϐ�
	
	public WavePlayer(String uri) throws IOException{
		// TODO �����������ꂽ�R���X�g���N�^�[�E�X�^�u
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
				// TODO �����������ꂽ���\�b�h�E�X�^�u
				
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
		// �E�F�[�u���b�g��͌��ʐ���
		byte[] wavelet_w1 = new byte[waveform1000ms.length / 2];
		byte[] wavelet_s1 = new byte[waveform1000ms.length / 2];
		for (int j = 0; j < waveform1000ms.length / 2 - 1; j++) {
			int average = (waveform1000ms[j * 2] + waveform1000ms[j * 2 + 1]) / 2;
			wavelet_s1[j] = (byte)average;
			int difference = (waveform1000ms[j * 2] - waveform1000ms[j * 2 + 1]);
			wavelet_w1[j] = (byte)difference;
		}
		// �g�`�̍�����devision�ɕ������čő�l�A�ŏ��l������
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
		// ����ꂽ�ő�l�A�ŏ��l�̂����������l�𒴂����L�v�Ȃ��̂𒊏o
		double threshold = 0.4;
		double min_threshold = values_min * threshold;
		double max_threshold = values_max * threshold;
		int minimum_duration = wavelet_w1.length / division;
		ArrayList<Integer> beatmin_indexes = new ArrayList<Integer>();
		ArrayList<Integer> beatmax_indexes = new ArrayList<Integer>();
		for (int i = 0; i < min_indexes.length; i++) {
			if(min_values[i] < min_threshold){
				if(beatmin_indexes.isEmpty() || min_indexes[i] - beatmin_indexes.get(beatmin_indexes.size() - 1) >= minimum_duration){	//	���x����̗]�n���肩��
					beatmin_indexes.add(min_indexes[i]);								
				}
			}
			if(max_values[i] > max_threshold){
				if(beatmax_indexes.isEmpty() || max_indexes[i] - beatmax_indexes.get(beatmax_indexes.size() - 1) >= minimum_duration){
					beatmax_indexes.add(max_indexes[i]);
				}
			}
		}
		// �L�v�ȍő�l�A�ŏ��l�̊Ԋu������BPM�����߂�
		if(beatmin_indexes.size() == 0){
			beatmin_indexes.add(0);
		}
		if(beatmax_indexes.size() == 0){
			beatmax_indexes.add(0);
		}
		int[] bpmmin = new int[beatmin_indexes.size() - 1];
		int[] bpmmax = new int[beatmax_indexes.size() - 1];
		int maximum_bpm = 185;
		double msper1sample = 1000.0 / (format.getInteger(format.KEY_SAMPLE_RATE) / 2);	// 1085.9 �͊��ˑ��v�f��������Ȃ��B1000ms��\���Ă���
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
		// ���܂���BPM�����O���[�o���ϐ��֑���
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
	
	//�Ԗؒǉ���,�������̏����������ɃK���K���Ə����Ă����܂�
	
	public void updateFFT()
	{
		if(waveform.length < 256){
			return;
		}
		
		// 1�b���Ƃ̔g�`�̍ŏ���256�T���v������������A�Ƃ肠����
		byte[] data = new byte[256];
		for (int i = 0; i < data.length; i++) {
			data[i] = waveform[i];
		}
		
		//���}������̃T���v�����
long time_b = System.currentTimeMillis();
		double [] real = new double[data.length]; // ������
		double [] imaginary = new double[data.length]; // ������
		
		for (int n = 0; n < data.length; n++) {
			double ReF = 0.0, ImF = 0.0;
			for (int k = 0; k < data.length; k++) {
				ReF += data[k]*Math.cos(2*Math.PI*k*n/(data.length + 1));
				ImF += -data[k]*Math.sin(2*Math.PI*k*n/(data.length + 1));
			}
			real[n] = ReF;	// ������
			imaginary[n] = ImF;	// ������
 		}
long time_a = System.currentTimeMillis();
Log.d("", (time_a - time_b) + "ms");
// double �� 64516 �� �� 27ms ���炢
		
		// Android�d�l�ɍ��킹��
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
		
		//�ő�l���o����
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
			/*�~�̔���A�~�̎��g���͂��̔ԍ��Ɋi�[����Ă���͂��B*/
			/*max��14�u�~�v���u�~�v��127�̋����������Ă����ꍇ�A�܂��O��̎��g��������50�ȉ��̏ꍇ�A�u�~�v�Ɣ��f����*/
			while(max > 23){	// �{���␳�����Ă݂���i�G �ǋL(�Ԗ�):�O�{���Ȃǂɂ��Ή��ł���悤�ɂ��Ă݂܂���
				max = max / (max/23+1) ;
			}
			// �������݃e�X�g�ł��A�e�X�g
							// fft[max] > max_value * 0.5 �ōő�l50%�݂����ɂ��Ă݂���
			
			if(max==12 && ((double)fft[max-2]/(double)fft[max])<0.5 && ((double)fft[max+2]/(double)fft[max])<=0.3 && ((double)fft[max+4]/(double)fft[max])<0.2 && cs==false)//�h#�̌��o
			{
				Log.d("���K","�h#");cs=true;dmj++;emj++;fma++;amj++;bmj++;bma++;
			}
			if(max==12 && ((double)fft[max-2]/(double)fft[max])<0.5 && ((double)fft[max+2]/(double)fft[max])>=0.5 && ((double)fft[max+2]/(double)fft[max])<=0.75 && ((double)fft[max+4]/(double)fft[max])>=0.2 && d==false)//���̌��o
			{
				Log.d("���K","��");d=true;cmj++;cma++;dmj++;ema++;fmj++;gmj++;gma++;bma++;
			}
			if(max==12 && ((double)fft[max-2]/(double)fft[max])>0.39 && ((double)fft[max+2]/(double)fft[max])>=0.9 && ((double)fft[max+4]/(double)fft[max])>0.35 && ds==false )//��#�̌��o
			{
				Log.d("���K","��#");ds=true;cma++;emj++;fma++;gma++;bmj++;
			}
			if(max==14 && ((double)fft[max-2]/(double)fft[max])<=0.15 && e==false)//�~�̌��o
			{
				Log.d("���K","�~");e=true;cmj++;dmj++;dma++;emj++;ema++;fmj++;gmj++;amj++;bmj++;bma++;
			}
			if(max==16 && ((double)fft[max-2]/(double)fft[max])>=0.7 && ((double)fft[max+2]/(double)fft[max]) >= 0.40 && f==false)//�t�@�̌��o
			{
				Log.d("���K","�t�@");f=true;cmj++;cma++;dma++;fmj++;fma++;gma++;
			}
			if(max==16 && ((double)fft[max-2]/(double)fft[max])<=0.5 && ((double)fft[max+2]/(double)fft[max])<=0.5 && fs==false)//�t�@#�̌��o
			{
				Log.d("���K","�t�@#");fs=true;dmj++;emj++;ema++;gmj++;amj++;bmj++;bma++;
			}
			if(max==18 && ((double)fft[max-2]/(double)fft[max])>=0.50 && ((double)fft[max+2]/(double)fft[max]) <= 0.50 && g==false)//�\�̌��o
			{
				Log.d("���K","�\");g=true;cmj++;cma++;dmj++;dma++;ema++;fmj++;fma++;gmj++;gma++;bma++;
			}
			if(max==18 && ((double)fft[max+2]/(double)fft[max])<=0.30 && ((double)fft[max+2]/(double)fft[max])<0.5 && gs==false)//�\#�̌��o
			{
				Log.d("���K","�\#");gs=true;cma++;emj++;fma++;amj++;bmj++;
			}
			if(max==20 && ((double)fft[max-2]/(double)fft[max])>=0.5 && ((double)fft[max+2]/(double)fft[max])<0.5 && a==false)//���̌��o
			{
				Log.d("���K","��");a=true;cmj++;dmj++;dma++;emj++;ema++;fmj++;gmj++;gma++;amj++;bma++;
			}
			if(max==20 && ((double)fft[max-2]/(double)fft[max])<0.5 && ((double)fft[max+2]/(double)fft[max])<0.5 && as==false )//��#�̌��o
			{
				Log.d("���K","��#");as=true;cma++;dma++;emj++;ema++;fmj++;gma++;bmj++;
			}
			if(max==22 && ((double)fft[max-2]/(double)fft[max])>=0.5 && ((double)fft[max+2]/(double)fft[max])<=0.5 && b==false )//�V�̌��o
			{
				Log.d("���K","�V");b=true;cmj++;dmj++;emj++;ema++;gmj++;amj++;bmj++;bma++;
			}
			if(max==22 && ((double)fft[max-2]/(double)fft[max])<=0.3 && c==false )//�h�̌��o
			{
				Log.d("���K","�h");c=true;cmj++;cma++;dma++;ema++;fmj++;fma++;gmj++;gma++;dmj--;
			}
			// Log.d("", (c ? "C" : "-") + (d ? "D" : "-") + (e ? "E" : "-") + (f ? "F" : "-") + (g ? "G" : "-") + (a ? "A" : "-") + (b ? "B" : "-") + );

			//�e��X�P�[���̌��o���s���A�擾���ꂽ���K���Q�l�ɃX�P�[���̔�����s��
			if(Scale!=true){

				if(cmj==6)
				{
					Log.d("�X�P�[��","C���W���[�X�P�[��");
					majar = true;
					Scale=true;
				}
				if(cma==6)
				{
					Log.d("�X�P�[��","C�}�C�i�[�X�P�[��");
					mainare=true;
					Scale=true;
				}
				//�����܂�C�X�P�[��
				//��������D�X�P�[��
				if(dmj==6)
				{
					Log.d("�X�P�[��","D���W���[�X�P�[��");
					majar = true;
					Scale = true;
				}
				if(dma==6)
				{
					Log.d("�X�P�[��","D�}�C�i�[�X�P�[��");
					mainare = true;
					Scale = true;
				}
				//�����܂�D�X�P�[��
				//��������E�X�P�[��
				if(emj==6)
				{
					Log.d("�X�P�[��","D���W���[�X�P�[��");
					majar = true;
					Scale = true;
				}
				if(ema==6)
				{
					Log.d("�X�P�[��","D�}�C�i�[�X�P�[��");
					mainare = true;
					Scale = true;
				}
				//�����܂�E�X�P�[��
				//��������F�X�P�[��
				if(fmj==6)
				{
					Log.d("�X�P�[��","D���W���[�X�P�[��");
					majar = true;
					Scale = true;
				}
				if(fma==6)
				{
					Log.d("�X�P�[��","D�}�C�i�[�X�P�[��");
					mainare = true;
					Scale = true;
				}
				//��������G�X�P�[��
				if(gmj==6)
				{
					Log.d("�X�P�[��","D���W���[�X�P�[��");
					majar = true;
					Scale = true;
				}
				if(gma==6)
				{
					Log.d("�X�P�[��","D�}�C�i�[�X�P�[��");
					mainare = true;
					Scale = true;
				}
				//��������a�X�P�[��
				if(amj==6)
				{
					Log.d("�X�P�[��","D���W���[�X�P�[��");
					majar = true;
					Scale = true;
				}
				//��������b�X�P�[��
				if(bmj==6)
				{
					Log.d("�X�P�[��","D���W���[�X�P�[��");
					majar = true;
					Scale = true;
				}
				if(bma==6)
				{
					Log.d("�X�P�[��","D�}�C�i�[�X�P�[��");
					mainare = true;
					Scale = true;
				}
			}
		}
	}
	
	//�ǉ����A�����ŏI��
	
	public String getCode(){
		// �����ɂ���āF����ŊO������A�N�Z�X�ł���Ɗy����
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
