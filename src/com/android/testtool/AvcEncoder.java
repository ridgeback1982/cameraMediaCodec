package com.android.testtool;

import java.nio.ByteBuffer;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;

interface AvcEncoderSink
{
	int onUpdateOutputBufferSize(/*in*/int size);
}

public class AvcEncoder
{
	public static final int STATUS_INVALID = 0;	//
	public static final int STATUS_LOADED = 1;	//component loaded, but not initialized. only accept call of set/getparameter
	public static final int STATUS_IDLE = 2;	//component initialized, ready to start
	public static final int STATUS_EXEC = 3;	//after start, it is processing data
	public static final int STATUS_WAIT = 5;	//waiting for resouces
	
	public static final String KEY_COLORFORMAT = "key_colorformat";
	public static final String KEY_WIDTH = "key_width";
	public static final String KEY_HEIGHT = "key_height";
	public static final String KEY_IDR_INTERVAL = "key-idrinterval";
	
	public static final int DEFAULT_AVC_BUF_SIZE = 1024*1024;	//1M bytes
	
	private MediaCodec mMC = null;
	//private String MIME_TYPE = "video/x-vnd.on2.vp8";
	private String MIME_TYPE = "video/avc";
	private AvcEncoderSink mSink = null;
	private MediaFormat mMF = null;
	private ByteBuffer[] mInputBuffers = null;
	private ByteBuffer[] mOutputBuffers = null;
	private final int BUFFER_TIMEOUT = 0; //microseconds
	private BufferInfo mBI = null;
	private byte[] mOutputBytesInStore = null;
	private long mOutputBytesInStore_timestamp = 0;
	private int mPrimeColorFormat = 0; //0 is not listed in Android doc, as MediaCodecInfo.CodecCapabilities
	private int mWidth = 0;
	private int mHeight = 0;
	private int mStatus = STATUS_INVALID;
	
	private FpsHelper mFpsHelper = null;
	
	//device related:
	//1. Galaxy S4, it is OK to set to -1, means no following IDR except the first one
	//2. Nexus 5, if set to -1, every frame is IDR, no P frames!!!
	private int mIDRInterval = 60;		//60 seconds to generate an IDR
	
	public void Init(int colorformat, AvcEncoderSink sink/*null as default*/)
	{
		Log.i("AvcEncoder", "Init");
		
	    mPrimeColorFormat = colorformat;
		
		mMC = MediaCodec.createEncoderByType(MIME_TYPE);
		
		mBI = new BufferInfo();
		
		mSink = sink;
		
		mFpsHelper = new FpsHelper();
		mFpsHelper.SetEnableDrop(true);
		
		mStatus = STATUS_LOADED;
	}
	
	public void Uninit()
	{
		Log.i("AvcEncoder", "Uninit");
		mMC.release();
		mMC = null;
		mSink = null;
		mBI = null;
		mFpsHelper = null;
		mOutputBytesInStore = null;
	}
	
	public int tryConfig(int width, int height, int framerate, int bitrate)
	{
		Log.i("AvcEncoder", "tryConfig ++, w="+width+",h="+height);
		mMF = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
		mMF.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);  
		mMF.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);  

		if (mPrimeColorFormat != 0)
		{
			mMF.setInteger(MediaFormat.KEY_COLOR_FORMAT, mPrimeColorFormat);  
		}
		mMF.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mIDRInterval); //关键帧间隔时间 单位s
		mMF.setInteger("stride", width);
		mMF.setInteger("slice-height", height);
		mWidth = width;
		mHeight = height;
		
		try {
			mMC.configure(mMF, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		}catch (Exception ex)
		{
			ex.printStackTrace();
			return -1;
		}
		
		mFpsHelper.SetFrameRateControlTarget(framerate);
		mStatus = STATUS_IDLE;
		
		Log.i("AvcEncoder", "tryConfig --");
		return 0;
	}
	
	//usage: int[] v = new int[1];
	public boolean queryInt(String key, int[] value)
	{
		if (key.equals(KEY_COLORFORMAT))
		{
			value[0] = mPrimeColorFormat;
			return true;
		}
		if (key.equals(KEY_WIDTH))
		{
			value[0] = mWidth;
			return true;
		}
		if (key.equals(KEY_HEIGHT))
		{
			value[0] = mHeight;
			return true;
		}
		
		return false;
	}
	
	public boolean setInt(String key, int value)
	{
		if (key.equals(KEY_IDR_INTERVAL))
		{
			if (mStatus != STATUS_LOADED)
				return false;
			mIDRInterval = value;
			return true;
		}
		return false;
	}
	
	public int status()
	{
		return mStatus;
	}

	public void start()
	{
		Log.i("AvcEncoder", "start");
		
		if (mStatus != STATUS_IDLE)
		{
			Log.e("AvcEncoder", "wrong status:"+mStatus);
			return;
		}
		
		if (mMC != null)
		{
			mMC.start();
			mInputBuffers = mMC.getInputBuffers();
			mOutputBuffers = mMC.getOutputBuffers();
			mStatus = STATUS_EXEC;
		}
	}
	
	public void stop()
	{
		Log.i("AvcEncoder", "stop");
		if (mStatus != STATUS_EXEC)
		{
			Log.d("AvcEncoder", "wrong status:"+mStatus);
			return;
		}
		
		if (mMC != null)
		{
			//mMC.signalEndOfInputStream();  //it can only be used with Surface input
			//mMC.flush();
			mMC.stop();
			mStatus = STATUS_IDLE;
		}
	}
	
	public void flush()
	{
		Log.i("AvcEncoder", "flush");
		if (mStatus != STATUS_EXEC)
		{
			Log.d("AvcEncoder", "wrong status:"+mStatus);
			return;
		}
		
		if (mMC != null)
		{
			mMC.flush();
		}
	}
	
	public void SetBitrateOnFly(int bps)
	{
		Log.i("AvcEncoder", "SetBitrateOnFly");
		Bundle b = new Bundle();
		b.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bps);
		if (mMC != null)
			mMC.setParameters(b);
	}
	
	public void RequestKeyFrameSoon()
	{
		Log.i("AvcEncoder", "RequestKeyFrameSoon");
		Bundle b = new Bundle();
		b.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
		if (mMC != null)
			mMC.setParameters(b);
	}
	
	public int InputRawBuffer(/*in*/byte[] bytes, /*in*/int len, /*in*/long timestamp, /*flag*/int flag)
	{
		//Log.i("AvcEncoder", "InputRawBuffer ++");
		
		if (mStatus != STATUS_EXEC)
		{
			//Log.d("AvcEncoder", "wrong status:"+mStatus);
			return AvcUtils.R_TRY_AGAIN_LATER;
		}
		
		if (true == mFpsHelper.ShouldBeDropped(timestamp))
		{
			return AvcUtils.R_BUFFER_OK;
		}
		
		int inputbufferindex = 0;
		try{
			inputbufferindex = mMC.dequeueInputBuffer(BUFFER_TIMEOUT);
		}
		catch (IllegalStateException ex)
		{
			Log.e("AvcEncoder", "dequeueInputBuffer throw IllegalStateException");
			return AvcUtils.R_INVALID_STATE;
		}
		if (inputbufferindex >= 0)
		{
			ByteBuffer inputBuffer = mInputBuffers[inputbufferindex];
			inputBuffer.clear();
			int capacity = inputBuffer.capacity();
			
			if (capacity < len)
			{
				mMC.queueInputBuffer(inputbufferindex, 0, 0, timestamp, flag); 	//return the buffer to OMX quickly
				Log.e("AvcEncoder", "InputRawBuffer, input size invalidate, capacity="+capacity+",len="+len);
				return AvcUtils.R_INVALIDATE_BUFFER_SIZE;
			}
			
			inputBuffer.put(bytes, 0, len);
			mMC.queueInputBuffer(inputbufferindex, 0, len, timestamp, flag);
			
			//Log.i("AvcEncoder", "InputRawBuffer -- OK, capacity="+capacity);
		}
		else if (inputbufferindex == MediaCodec.INFO_TRY_AGAIN_LATER)
		{
			Log.i("AvcEncoder", "InputRawBuffer -- INFO_TRY_AGAIN_LATER");
//			try {
//				Thread.sleep(1);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			
			return  AvcUtils.R_TRY_AGAIN_LATER;
		}
		else
		{
			//unexpected return value, not specified in Android doc
			return AvcUtils.R_UNKNOWN;
		}
		return AvcUtils.R_BUFFER_OK;
	}
	
	//usage: int[] len = new int[1];
	//long[] ts = new long[1];
	//int[] flag = new int[1];
	public int OutputAvcBuffer(/*in*/byte[] bytes, /*in, out*/int[] len, /*out*/long[] timestamp, /*out*/int[] flags)
	{
		//Log.i("AvcEncoder", "OutputAvcBuffer ++");
		if (mStatus != STATUS_EXEC)
		{
			//Log.d("AvcEncoder", "wrong status:"+mStatus);
			return AvcUtils.R_TRY_AGAIN_LATER;
		}
		
		if (mOutputBytesInStore != null)
		{
			if (mOutputBytesInStore.length > len[0])
			{
				Log.w("AvcEncoder", "OutputAvcBuffer, len is still too small, requre at least "+ mOutputBytesInStore.length);
				if (mSink != null)
					mSink.onUpdateOutputBufferSize(mOutputBytesInStore.length);
				return AvcUtils.R_INVALIDATE_BUFFER_SIZE;
			}
			else
			{
				Log.i("AvcEncoder", "OutputAvcBuffer, play the buffer in store, len is "+ mOutputBytesInStore.length);
				System.arraycopy(mOutputBytesInStore, 0, bytes, 0, mOutputBytesInStore.length);
				len[0] = mOutputBytesInStore.length;
				timestamp[0] = mOutputBytesInStore_timestamp;
				
				mOutputBytesInStore_timestamp = 0;
				mOutputBytesInStore = null;
				return AvcUtils.R_BUFFER_OK;
			}
		}
		
		int outputbufferindex = 0;
		try {
			outputbufferindex = mMC.dequeueOutputBuffer(mBI, BUFFER_TIMEOUT);
		}
		catch (IllegalStateException ex)
		{
			Log.e("AvcEncoder", "dequeueOutputBuffer throw IllegalStateException");
			return AvcUtils.R_INVALID_STATE;
		}
		if (outputbufferindex >= 0)
		{
			mOutputBuffers[outputbufferindex].position(mBI.offset);
			mOutputBuffers[outputbufferindex].limit(mBI.offset + mBI.size);
			
			if (mBI.size > len[0])
			{
				Log.w("AvcEncoder", "OutputAvcBuffer, len is too small, requre at least "+ mBI.size);
				if (mSink != null)
					mSink.onUpdateOutputBufferSize(mBI.size);
				mOutputBytesInStore = new byte[mBI.size];
				mOutputBytesInStore_timestamp = mBI.presentationTimeUs;
				mOutputBuffers[outputbufferindex].get(mOutputBytesInStore);
				mMC.releaseOutputBuffer(outputbufferindex, false);
				return AvcUtils.R_INVALIDATE_BUFFER_SIZE;
			}
			mOutputBuffers[outputbufferindex].get(bytes, 0, mBI.size);
			len[0] = mBI.size ;
			timestamp[0] = mBI.presentationTimeUs;
			flags[0] = mBI.flags;
			
			if (mBI.flags == MediaCodec.BUFFER_FLAG_SYNC_FRAME)
				Log.d("AvcEncoder", "OutputAvcBuffer SYNC frame, "+mWidth+"x "+mHeight);
			
			mMC.releaseOutputBuffer(outputbufferindex, false);
			
			//Log.i("AvcEncoder", "OutputAvcBuffer -- OK at "+ outputbufferindex+", size="+len[0]);
		}
		else if (outputbufferindex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED)
		{
			mOutputBuffers = mMC.getOutputBuffers();
			Log.i("AvcEncoder", "OutputAvcBuffer -- INFO_OUTPUT_BUFFERS_CHANGED");
			return AvcUtils.R_OUTPUT_UPDATE;
		}
		else if (outputbufferindex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
		{
			mMF = mMC.getOutputFormat();
			//it is so wired, the "MediaFormat" is little usage. They says "There is no way to query the encoder for the MediaFormat"
			
			
			//int new_width = mMF.getInteger(MediaFormat.KEY_WIDTH);
			//int new_height = mMF.getInteger(MediaFormat.KEY_HEIGHT);
			//int new_bps = mMF.getInteger(MediaFormat.KEY_BIT_RATE);
			//int new_cf = mMF.getInteger(MediaFormat.KEY_COLOR_FORMAT);
			//int new_fps = mMF.getInteger(MediaFormat.KEY_FRAME_RATE);
			
			//Log.i("AvcEncoder", "OutputAvcBuffer -- INFO_OUTPUT_FORMAT_CHANGED: "+new_width+"x"+new_height+"@"+new_fps+/*",in "+new_bps+"bps"+*/", cf="+new_cf);
			return AvcUtils.R_OUTPUT_UPDATE;
		}
		else if (outputbufferindex == MediaCodec.INFO_TRY_AGAIN_LATER)
		{
			//Log.i("AvcEncoder", "OutputAvcBuffer -- INFO_TRY_AGAIN_LATER");
//			try {
//				Thread.sleep(10);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			
			return AvcUtils.R_TRY_AGAIN_LATER;
		}
		else
		{
			//unexpected return value, not specified in Android doc
			return AvcUtils.R_UNKNOWN;
		}
		
		
		return AvcUtils.R_BUFFER_OK;
	}
}