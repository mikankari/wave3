package com.example.wave3;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

public class WavePlayer{
	
	private MediaExtractor extractor;
	private MediaCodec codec;
	private Thread inputbuffer_thread;
	private Thread outputbuffer_thread;
	private boolean inputbuffer_isloop;
	private boolean outputbuffer_isloop;
	private byte[] waveform;
	
	public WavePlayer(String uri) throws IOException{
		// TODO 自動生成されたコンストラクター・スタブ
		extractor = new MediaExtractor();
		extractor.setDataSource(uri);
		extractor.selectTrack(extractor.getTrackCount() - 1);
		final MediaFormat format = extractor.getTrackFormat(extractor.getTrackCount() - 1);
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
						Log.d("", "fail to get inputbuffer");
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
		
	}
	
	private void updateWaveform(byte[] waveform){
		this.waveform = waveform;
	}
	
	public byte[] getWaveform(){
		return waveform;
	}
	
	public double getDecodePercentage(){
		MediaFormat format = extractor.getTrackFormat(extractor.getTrackCount() - 1);
//Log.d("", extractor.getSampleTime() / format.getLong(MediaFormat.KEY_DURATION));
		return extractor.getSampleTime() / (double)format.getLong(MediaFormat.KEY_DURATION);
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
