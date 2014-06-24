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
	public static final int R_BUFFER_OK = 0;
	public static final int R_TRY_AGAIN_LATER = -1;
	public static final int R_OUTPUT_UPDATE = -2;
	public static final int R_INVALIDATE_BUFFER_SIZE = -10;
	public static final int R_UNKNOWN = -40;
	
	public static final int STATUS_INVALID = 0;	//
	public static final int STATUS_LOADED = 1;	//component loaded, but not initialized. only accept call of set/getparameter
	public static final int STATUS_IDLE = 2;	//component initialized, ready to start
	public static final int STATUS_EXEC = 3;	//after start, it is processing data
	public static final int STATUS_WAIT = 5;	//waiting for resouces
	
	public static final String KEY_COLORFORMAT = "key_colorformat";
	
	public static final int DEFAULT_AVC_BUF_SIZE = 1024*1024;	//1M bytes
	
	private MediaCodec mMC = null;
	private String MIME_TYPE = "video/avc";
	private AvcEncoderSink mSink = null;
	private MediaFormat mMF = null;
	private ByteBuffer[] mInputBuffers = null;
	private ByteBuffer[] mOutputBuffers = null;
	private final int BUFFER_TIMEOUT = 0; //microseconds
	private BufferInfo mBI = null;
	private byte[] mOutputBytesInStore = null;
	private int mPrimeColorFormat = 0; //0 is not listed in Android doc, as MediaCodecInfo.CodecCapabilities
	private int mStatus = STATUS_INVALID;
	
	private FpsHelper mFpsHelper = null;
	
	private static MediaCodecInfo selectCodec(String mimeType) {
	     int numCodecs = MediaCodecList.getCodecCount();
	     for (int i = 0; i < numCodecs; i++) {
	         MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

	         if (!codecInfo.isEncoder()) {
	             continue;
	         }

	         String[] types = codecInfo.getSupportedTypes();
	         for (int j = 0; j < types.length; j++) {
	             if (types[j].equalsIgnoreCase(mimeType)) {
	            	 Log.d("AvcEncoder", "selectCodec OK, get "+mimeType);
	                 return codecInfo;
	             }
	         }
	     }
	     return null;
	 }
	
	public void Init(AvcEncoderSink sink)
	{
		Log.i("AvcEncoder", "Init");
		
		MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
		
		// Find a color profile that the codec supports
	    MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(MIME_TYPE);	//may freeze on Nexus4 Android4.2.2
	    for (int i = 0; i < capabilities.colorFormats.length && mPrimeColorFormat == 0; i++) {
	        int format = capabilities.colorFormats[i];
	        switch (format) {
	        case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:				/*I420 --- Nvidia Tegra 3, Samsung Exynos */
	        case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:		/*yv12 --- Qualcomm Adreno */
	        case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:			/*NV12*/
	        case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:	/*NV21 --- TI OMAP */
	        case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
	        case MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar:
	        	mPrimeColorFormat = format;
	        	Log.d("AvcEncoder", "get supportted color format " + format);
	            break;
	        default:
	            Log.d("AvcEncoder", "Skipping unsupported color format " + format);
	            break;
	        }
	    }
	    
	    //mPrimeColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar;
		
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
	}
	
	public int tryConfig(int width, int height, int framerate, int bitrate)
	{
		Log.i("AvcEncoder", "tryConfig ++");
		mMF = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
		mMF.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);  
		mMF.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);  

		if (mPrimeColorFormat != 0)
		{
			mMF.setInteger(MediaFormat.KEY_COLOR_FORMAT, mPrimeColorFormat);  
		}
		mMF.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, -1); //关键帧间隔时间 单位s
		mMF.setInteger("stride", width);
		mMF.setInteger("slice-height", height);
		
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
		
		return false;
	}

	public void start()
	{
		Log.i("AvcEncoder", "start");
		if (mStatus == STATUS_EXEC)
		{
			Log.d("AvcEncoder", "wrong status:"+mStatus);
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
			mMC.flush();
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
	
	public int InputRawBuffer(/*in*/byte[] bytes, /*in*/int len, long timestamp)
	{
		//Log.i("AvcEncoder", "InputRawBuffer ++");
		
		if (true == mFpsHelper.ShouldBeDropped(timestamp))
		{
			return R_BUFFER_OK;
		}
		
		int inputbufferindex = mMC.dequeueInputBuffer(BUFFER_TIMEOUT);
		if (inputbufferindex >= 0)
		{
			ByteBuffer inputBuffer = mInputBuffers[inputbufferindex];
			inputBuffer.clear();
			int capacity = inputBuffer.capacity();
			
			if (capacity < len)
			{
				mMC.queueInputBuffer(inputbufferindex, 0, 0, 0, 0); 	//return the buffer to OMX quickly
				Log.e("AvcEncoder", "InputRawBuffer, input size invalidate, capacity="+capacity+",len="+len);
				return R_INVALIDATE_BUFFER_SIZE;
			}
			
			inputBuffer.put(bytes, 0, len);
			mMC.queueInputBuffer(inputbufferindex, 0, len, 0, 0);
			
			Log.i("AvcEncoder", "InputRawBuffer -- OK, capacity="+capacity);
		}
		else if (inputbufferindex == MediaCodec.INFO_TRY_AGAIN_LATER)
		{
			Log.i("AvcEncoder", "InputRawBuffer -- INFO_TRY_AGAIN_LATER");
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return R_TRY_AGAIN_LATER;
		}
		else
		{
			//unexpected return value, not specified in Android doc
			return R_UNKNOWN;
		}
		return R_BUFFER_OK;
	}
	
	//usage: int[] len = new int[1];
	public int OutputAvcBuffer(/*out*/byte[] bytes, /*in, out*/int[] len)
	{
		//Log.i("AvcEncoder", "OutputAvcBuffer ++");
		if (mOutputBytesInStore != null)
		{
			if (mOutputBytesInStore.length > len[0])
			{
				Log.w("AvcEncoder", "OutputAvcBuffer, len is still too small, requre at least "+ mOutputBytesInStore.length);
				mSink.onUpdateOutputBufferSize(mOutputBytesInStore.length);
				return R_INVALIDATE_BUFFER_SIZE;
			}
			else
			{
				Log.i("AvcEncoder", "OutputAvcBuffer, play the buffer in store, len is "+ mOutputBytesInStore.length);
				System.arraycopy(mOutputBytesInStore, 0, bytes, 0, mOutputBytesInStore.length);
				len[0] = mOutputBytesInStore.length;
				mOutputBytesInStore = null;
				return R_BUFFER_OK;
			}
		}
		
		int outputbufferindex = mMC.dequeueOutputBuffer(mBI, BUFFER_TIMEOUT);
		if (outputbufferindex >= 0)
		{
			mOutputBuffers[outputbufferindex].position(mBI.offset);
			mOutputBuffers[outputbufferindex].limit(mBI.offset + mBI.size);
			
			if (mBI.size > len[0])
			{
				Log.w("AvcEncoder", "OutputAvcBuffer, len is too small, requre at least "+ mBI.size);
				mSink.onUpdateOutputBufferSize(mBI.size);
				mOutputBytesInStore = new byte[mBI.size];
				mOutputBuffers[outputbufferindex].get(mOutputBytesInStore);
				mMC.releaseOutputBuffer(outputbufferindex, false);
				return R_INVALIDATE_BUFFER_SIZE;
			}
			mOutputBuffers[outputbufferindex].get(bytes, 0, mBI.size);
			len[0] = mBI.size ;
			mMC.releaseOutputBuffer(outputbufferindex, false);
			
			Log.i("AvcEncoder", "OutputAvcBuffer -- OK at "+ outputbufferindex+", size="+len[0]);
		}
		else if (outputbufferindex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED)
		{
			mOutputBuffers = mMC.getOutputBuffers();
			Log.i("AvcEncoder", "OutputAvcBuffer -- INFO_OUTPUT_BUFFERS_CHANGED");
			return R_OUTPUT_UPDATE;
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
			return R_OUTPUT_UPDATE;
		}
		else if (outputbufferindex == MediaCodec.INFO_TRY_AGAIN_LATER)
		{
			Log.i("AvcEncoder", "OutputAvcBuffer -- INFO_TRY_AGAIN_LATER");
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return R_TRY_AGAIN_LATER;
		}
		else
		{
			//unexpected return value, not specified in Android doc
			return R_UNKNOWN;
		}
		
		
		return R_BUFFER_OK;
	}
}