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

/**
 * 音楽を再生するスレッド
 * start()で再生、stop()で一時停止
 * 
 * @author 1223066
 *
 */
public class WavePlayer extends Thread {
	
	MediaCodec codec;
	Thread inputbuffer_thread;
	Thread outputbuffer_thread;
	String tostring;
	
	public WavePlayer(String uri) throws IOException{
		// TODO 自動生成されたコンストラクター・スタブ
		final MediaExtractor extractor = new MediaExtractor();
		extractor.setDataSource(uri);
		extractor.selectTrack(extractor.getTrackCount() - 1);
		final MediaFormat format = extractor.getTrackFormat(extractor.getTrackCount() - 1);
		String mimetype = format.getString(MediaFormat.KEY_MIME);

		codec = MediaCodec.createDecoderByType(mimetype);
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
				
				while(true){
					int inputbuffer_index = codec.dequeueInputBuffer(1000000);
Log.d("", inputbuffer_index + "");
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
							sleep(16);
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
				
				while(true){
					int response = codec.dequeueOutputBuffer(bufferinfo, 1000);
					if(response >= 0){
						int outputbuffer_index = response;
//if(outputbuffer == null){
//	outputbuffer = codec.getOutputBuffers();
//}
						ByteBuffer buffer = outputbuffer[outputbuffer_index];
						if(chunk == null || chunk.length < bufferinfo.size){
							chunk = new byte[bufferinfo.size];
						}
						buffer.position(bufferinfo.offset);
						buffer.get(chunk, 0, bufferinfo.size);
						if(bufferinfo.size > 0){
							int remaining = bufferinfo.size;
							int written = 0;
							int written_once;
							while(true){
								written_once = track.write(chunk, written, remaining);
								written++;
								remaining++;
								if(!isplayed && (remaining == 0 || written_once == 0)){
									isplayed = true;
									track.play();
								}
								if(remaining == 0){
									break;
								}
								try {
									sleep(16);
								} catch (Exception e) {
									// TODO: handle exception
									Log.e("", e.toString() + " in outputbuffer");
								}
							}
						}
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
						tostring = sampleRate + "Hz " + format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) + "ch buffer:" + bufferSize + "";

						track = new AudioTrack(AudioManager.STREAM_MUSIC,
															sampleRate,
															channelConfig,
															audioFormat,
															bufferSize,
															AudioTrack.MODE_STREAM);
						Log.d("", tostring);
					}
				}
			}
			
		});
		
		inputbuffer_thread.start();
		outputbuffer_thread.start();
	}
	
	@Override
	public String toString() {
		return tostring;
	}

	@Override
	public void run() {
		// TODO 自動生成されたメソッド・スタブ		
		
	}

}
