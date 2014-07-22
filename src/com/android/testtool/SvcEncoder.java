package com.android.testtool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

public class SvcEncoder {
	
	private final int SPACIAL_LAYER_CAPACITY = 4;
	private int mMaxSpacialLayerIndex = 0;
	private AvcEncoder[] 	mAvcEncoders = null;
	private SvcEncodeSpacialParam[]	mSvcEncodeParams = null;
	private final String MIME_TYPE = "video/avc";
	private int mPrimeColorFormat = 0; //0 is not listed in Android doc, as MediaCodecInfo.CodecCapabilities
	public static final String KEY_COLORFORMAT = "key_colorformat";
	public static final String KEY_WIDTH = "key_width";
	public static final String KEY_HEIGHT = "key_height";
	
	Queue<VideoBufferInfo>[] mRawDataQueue = null;
	private int mPeriodIDR = 60;	/*seconds*/
	
	
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
	
	public int Init()
	{
		Log.i("SvcEnc", "Init ++");
		MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
		
		// Find a color profile that the codec supports
	    MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(MIME_TYPE);	//may freeze on Nexus4 Android4.2.2
	    for (int i = 0; i < capabilities.colorFormats.length && mPrimeColorFormat == 0; i++) {
	        int format = capabilities.colorFormats[i];
	        switch (format) {
	        //primary color formats
	        case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:				/*I420 --- YUV4:2:0 --- Nvidia Tegra 3, Samsung Exynos */
	        case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:		/*yv12 --- YVU4:2:0 --- ?*/
	        case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:			/*NV12 --- Qualcomm Adreno330/320*/
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
	    
	    mAvcEncoders = new AvcEncoder[SPACIAL_LAYER_CAPACITY];
	    mSvcEncodeParams = new SvcEncodeSpacialParam[SPACIAL_LAYER_CAPACITY];
	    
	    mRawDataQueue = (LinkedList<VideoBufferInfo>[])new LinkedList<?>[SPACIAL_LAYER_CAPACITY];
	    for (int i=0;i<SPACIAL_LAYER_CAPACITY;i++)
	    {
	    	mRawDataQueue[i] = new LinkedList<VideoBufferInfo>();
	    }
	    
	    
	    
	    Log.i("SvcEnc", "Init --");
	    return 0;
	}
	
	public int Uninit()
	{
		Log.i("SvcEnc", "Uninit ++");
		if (mAvcEncoders == null)
		{
			return 1;
		}
		
		UninitAvcEncoders();
		mSvcEncodeParams = null;
		mRawDataQueue = null;
		//mMaxSpacialLayerIndex = 0;
		Log.i("SvcEnc", "Uninit --");
		return 0;
	}
	
	private void UninitAvcEncoders()
	{
		for(int i=0;i<mAvcEncoders.length;i++)
	    {
	    	if(mAvcEncoders[i] != null)
	    	{
	    		mAvcEncoders[i].Uninit();
	    		mAvcEncoders[i] = null;
	    	}
	    }
	}
	
	public int GetSpacialLayerCapacity()
	{
		return SPACIAL_LAYER_CAPACITY;
	}
	
	//usage:
	//int capa = GetSpacialLayerCapacity();
	//SvcEncodeSpacialParam[] params = new SvcEncodeSpacialParam[capa];
	//param[3].mWidth = ....
	public int Configure(SvcEncodeSpacialParam[] enc_params, int period_idr/*seconds*/)
	{
		Log.i("SvcEnc", "Configure ++");
		if (mAvcEncoders == null || enc_params.length > SPACIAL_LAYER_CAPACITY)
		{
			return 1;
		}
		
		for (int i=0;i<SPACIAL_LAYER_CAPACITY;i++)
		{
			if (i < enc_params.length)
			{
				if (enc_params[i] != null && enc_params[i].isMeValid() == true)
				{
					if (mAvcEncoders[i] == null)
					{
						mAvcEncoders[i] = new AvcEncoder();
						mAvcEncoders[i].Init(mPrimeColorFormat, null);
					}
					
					mPeriodIDR = period_idr;
					mAvcEncoders[i].setInt(AvcEncoder.KEY_IDR_INTERVAL, mPeriodIDR);
					
					mAvcEncoders[i].tryConfig(enc_params[i].mWidth, enc_params[i].mHeight, enc_params[i].mFrameRate, enc_params[i].mBitrate);
					if (mSvcEncodeParams[i] != enc_params[i])
						mSvcEncodeParams[i] = enc_params[i].clone();
					if (mMaxSpacialLayerIndex < i)
						mMaxSpacialLayerIndex = i;
				}
				else
				{
					mSvcEncodeParams[i] = null;
				}
				
			}
			else
			{
				mSvcEncodeParams[i] = null;
			}
		}

		
		Log.i("SvcEnc", "Configure --");
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
	
	
	public int Start()
	{
		Log.i("SvcEnc", "Start ++");
		if (mAvcEncoders == null)
		{
			return 1;
		}
		
		for(int i=0;i<SPACIAL_LAYER_CAPACITY;i++)
		{
			if (mSvcEncodeParams[i] != null)
			{
				if (mAvcEncoders[i] != null)
				{
					mAvcEncoders[i].start();
				}
			}
		}

		Log.i("SvcEnc", "Start --");
		return 0;
	}
	
	public int Stop()
	{
		Log.i("SvcEnc", "Stop ++");
		if (mAvcEncoders == null)
		{
			return 1;
		}

		for(int i=0;i<SPACIAL_LAYER_CAPACITY;i++)
		{
			if (mSvcEncodeParams[i] != null)
			{
				if (mAvcEncoders[i] != null)
				{
					mAvcEncoders[i].stop();
				}
			}
		}
		
		Log.i("SvcEnc", "Stop --");
		return 0;
	}
	
	public int Flush()
	{
		Log.i("SvcEnc", "Flush ++");
		if (mAvcEncoders == null)
		{
			return 1;
		}
		
		for(int i=0;i<SPACIAL_LAYER_CAPACITY;i++)
		{
			if (mSvcEncodeParams[i] != null)
			{
				if (mAvcEncoders[i] != null)
				{
					mAvcEncoders[i].flush();
				}
			}
		}
		
		Log.i("SvcEnc", "Flush --");
		return 0;
	}
	
	public int SetBitrateOnFly(int spacial_idx, int bps)
	{
		Log.i("SvcEnc", "SetBitrateOnFly, spacial_idx="+spacial_idx+",bps="+bps);
		if (mAvcEncoders == null || spacial_idx >= SPACIAL_LAYER_CAPACITY)
		{
			return 1;
		}
		
		if (mSvcEncodeParams[spacial_idx] != null)
		{
			if (mAvcEncoders[spacial_idx] != null)
			{
				mAvcEncoders[spacial_idx].SetBitrateOnFly(bps);
			}
		}
		
		return 0;
	}
	
	
	public int RequestKeyFrameSoon()
	{
		Log.i("SvcEnc", "RequestKeyFrameSoon");
		if (mAvcEncoders == null)
		{
			return 1;
		}
		
		if (Build.VERSION.SDK_INT < 19)
		{
			Stop();
			//hard code, for compatible
	    	if (Build.VERSION.SDK_INT <= 17)	//for "CP-DX80"
	    	{
	    		UninitAvcEncoders();
	    		Configure(mSvcEncodeParams, mPeriodIDR);
	    	}
	    	Start();
		}
		else {
			for(int i=0;i<SPACIAL_LAYER_CAPACITY;i++)
			{
				if (mSvcEncodeParams[i] != null)
				{
					if (mAvcEncoders[i] != null)
					{
						mAvcEncoders[i].RequestKeyFrameSoon();
					}
				}
			}
		}
		
		return 0;
	}
	
	//TODO:
	//For rookie: input one yuv frame, downsample it to the fit size as input to AvcEncoder one by one
	//For pro: Need one(four spacial layers) queue for each layer. When method invoked, input downsampled(one spatial layer) yuv data into the corresponding queue, and
	//	then dequeue the queue to call "InputRawBuffer" one by one until the queue is empty. If "wait" is returned, the dequeue process should end also. 
	//From my sight, yuv came from camera need go to pre-process(rotation and color conversion), then this big picture will go to SvcEncoder.
	//In side of SvcEncoder, several downsample will be done
	
	public int InputRawBuffer(/*in*/byte[] bytes, /*in*/int len, /*in*/long timestamp, /*flag*/int flag)
	{
		int res = AvcUtils.R_UNKNOWN;
		//rookie stage now
		if (mAvcEncoders == null)
		{
			return res;
		}
		
		//init some values
		int[] dst_width = new int[1];
		int[] dst_height = new int[1];
		if (mAvcEncoders[mMaxSpacialLayerIndex] != null)
		{
			mAvcEncoders[mMaxSpacialLayerIndex].queryInt(AvcEncoder.KEY_WIDTH, dst_width);
			mAvcEncoders[mMaxSpacialLayerIndex].queryInt(AvcEncoder.KEY_HEIGHT, dst_height);
		}
		int raw_siz_check = (int) (RawUtils.BytesPerPixel(mPrimeColorFormat) * dst_width[0] * dst_height[0]);
		if (raw_siz_check > len)
		{
			res = AvcUtils.R_INVALIDATE_BUFFER_SIZE;
			return res;
		}
		
		int src_width = 0;
		int src_height = 0;
		byte[] src_yuv = bytes;
		byte[] dst_yuv = bytes;
		
		for (int i=mMaxSpacialLayerIndex;i>=0;i--)
		{
			if (mSvcEncodeParams[i] != null)
			{
				if (mAvcEncoders[i] != null)
				{
					src_yuv = dst_yuv;
					dst_yuv = null;
					src_width = dst_width[0];
					src_height = dst_height[0];
					mAvcEncoders[i].queryInt(AvcEncoder.KEY_WIDTH, dst_width);
					mAvcEncoders[i].queryInt(AvcEncoder.KEY_HEIGHT, dst_height);
					
					dst_yuv = RawUtils.YuvDownsample(mPrimeColorFormat, src_yuv, src_width, src_height, dst_width[0], dst_height[0]);
					if (dst_yuv != null)
					{
						int blen = (int) (RawUtils.BytesPerPixel(mPrimeColorFormat) * dst_width[0] * dst_height[0]);
						if (mRawDataQueue[i] != null)
						{
							//For pro
							VideoBufferInfo info = new VideoBufferInfo();
							info.buffer = dst_yuv;
							info.size = blen;
							info.timestamp = timestamp;
							info.flag = flag;
							mRawDataQueue[i].add(info);
							
							Iterator<VideoBufferInfo> ite = mRawDataQueue[i].iterator();
							while (ite.hasNext())
							{
								VideoBufferInfo infoo = ite.next();
								res = mAvcEncoders[i].InputRawBuffer(dst_yuv, blen, timestamp, flag);
								if (res != AvcUtils.R_BUFFER_OK)
								{
									break;
								}
								infoo.buffer = null;
								infoo = null;
								ite.remove();
							}
						}
						else
						{
							//for rookie stage, no need to handle res, but for higher stage, I must handle it
							res = mAvcEncoders[i].InputRawBuffer(dst_yuv, blen, timestamp, flag);
						}
					}	//if (dst_yuv != null)
				}	//if (mAvcEncoders[i] != null)
			}	//if (mSvcEncodeParams[i] != null)
		}	//for()
		
		dst_yuv = null;
		return res;
	}
	
	//TODO:
	//For rookie: output one AVC buffer of one layer each call
	//usage: 
	//1. SvcEncodeOutputParam xxx = new SvcEncodeOutputParam();
	//2. byte is pre-allocated
	//3. int[] len = new int[1]
	public int OutputAvcBuffer(/*in*/byte[] bytes, /*in, out*/int[] len, /*out*/SvcEncodeOutputParam output)
	{
		int res = AvcUtils.R_UNKNOWN;
		//rookie stage now
		if (mAvcEncoders == null)
		{
			return AvcUtils.R_UNKNOWN;
		}
		
		int layeridx = mMaxSpacialLayerIndex;
		for (;layeridx>=0;layeridx--)
		{
			if (mSvcEncodeParams[layeridx] != null)
			{
				if (mAvcEncoders[layeridx] != null)
				{
					long[] ts = new long[1];
					int[] flag = new int[1];
					res = mAvcEncoders[layeridx].OutputAvcBuffer(bytes, len, ts, flag);
					if (AvcUtils.R_BUFFER_OK == res)
					{
						int[] width = new int[1];
						int[] height = new int[1];
						mAvcEncoders[layeridx].queryInt(AvcEncoder.KEY_WIDTH, width);
						mAvcEncoders[layeridx].queryInt(AvcEncoder.KEY_HEIGHT, height);
						output.timestamp = ts[0];
						output.layernumber = mMaxSpacialLayerIndex + 1;
						output.layerindex = layeridx;
						output.layerwidth = width[0];
						output.layerheight = height[0];
						/*
						 * 	.....
						 * 
						 * 
						 * */
					
					
						break;
					}
				}
			}
		}

		
		return res;
		
	}
	
}