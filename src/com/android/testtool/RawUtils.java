package com.android.testtool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.MediaCodecInfo;
import android.os.Environment;



class RawUtils {
	//Map to WseVieoType
	public static final int CS_UNKNOWN = 0;
	public static final int	CS_RGB24 = 1;
	public static final int CS_I420 = 2;
	public static final int CS_YUY2 = 3;
	public static final int CS_BGR24 = 4;
	public static final int CS_YV12 = 5;
	public static final int CS_RGB24Flip = 6;
	public static final int	CS_BGR24Flip = 7;
	public static final int CS_RGBA32 = 8;
	public static final int CS_BGRA32 = 9;
	public static final int CS_ARGB32 = 10;
	public static final int CS_ABGR32 = 11;
	public static final int CS_RGBA32Flip = 12;
	public static final int CS_RBGRA32Flip = 13;
	public static final int CS_ARGB32Flip = 14;
	public static final int CS_ABRG32Flip = 15;
	public static final int CS_NV21 = 16;
	public static final int CS_NV12 = 17;

	
	//debug only
	private static FileOutputStream mFOS = null;
	
	public static byte[] YuvDownsample(int src_format, byte[] src_yuv, int src_width, int src_height, int dst_width, int dst_height)
	{
		byte[] dst_yuv = null;
		if (src_yuv == null)
			return dst_yuv;
		//simple implementation: copy the corner
		if (src_width == dst_width && src_height == dst_height)
		{
			dst_yuv = src_yuv;
		}
		else
		{
			dst_yuv = new byte[(int) (dst_width*dst_height*BytesPerPixel(src_format))];
			switch(src_format)
			{
				case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:				/*I420 --- YUV4:2:0 */
				case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:		/*yv12 --- YVU4:2:0 */
				{
					//copy Y
					int src_yoffset = 0;
					int dst_yoffset = 0;
					for (int i=0;i<dst_height;i++)
					{
						System.arraycopy(src_yuv, src_yoffset, dst_yuv, dst_yoffset, dst_width);
						src_yoffset += src_width;
						dst_yoffset += dst_width;
					}
					
					//copy u
					int src_uoffset = 0;
					int dst_uoffset = 0;
					src_yoffset = src_width*src_height;
					dst_yoffset = dst_width*dst_height;
					for (int i=0;i<dst_height/2;i++)
					{
						System.arraycopy(src_yuv, src_yoffset + src_uoffset, 
								dst_yuv, dst_yoffset + dst_uoffset, dst_width/2);
						src_uoffset += src_width/2;
						dst_uoffset += dst_width/2;
					}
					
					//copy v
					int src_voffset = 0;
					int dst_voffset = 0;
					src_uoffset = src_width*src_height + src_width*src_height/4;
					dst_uoffset = dst_width*dst_height + dst_width*dst_height/4;
					for (int i=0;i<dst_height/2;i++)
					{
						System.arraycopy(src_yuv, src_uoffset + src_voffset, 
								dst_yuv, dst_uoffset + dst_voffset, dst_width/2);
						src_voffset += src_width/2;
						dst_voffset += dst_width/2;
					}
					
				}
				break;
				
				case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:			/*NV12 --- */
			    case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:	/*NV21 --- */
			    case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
			    case MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar:
			    {
			    	//copy Y
					int src_yoffset = 0;
					int dst_yoffset = 0;
					for (int i=0;i<dst_height;i++)
					{
						System.arraycopy(src_yuv, src_yoffset, dst_yuv, dst_yoffset, dst_width);
						src_yoffset += src_width;
						dst_yoffset += dst_width;
					}
					
					//copy u and v
					int src_uoffset = 0;
					int dst_uoffset = 0;
					src_yoffset = src_width*src_height;
					dst_yoffset = dst_width*dst_height;
					for (int i=0;i<dst_height/2;i++)
					{
						System.arraycopy(src_yuv, src_yoffset + src_uoffset, 
								dst_yuv, dst_yoffset + dst_uoffset, dst_width);
						src_uoffset += src_width;
						dst_uoffset += dst_width;
					}
			    }
			    break;
			    
			    default:
			    {
			    	dst_yuv = null;
			    }
			    break;
			}
		}
		
		
		//debug only 
//		if (mFOS == null && dst_width == 640 && dst_height == 360)
//		{
//			try {
//	        	File dir = Environment.getExternalStorageDirectory();
//	        	String fname = "640x360.yuv";
//	            File filePath = new File(dir, fname);
//	            if (filePath.exists() == true && filePath.isFile() == true)
//	            	filePath.delete();
//				mFOS = new FileOutputStream(filePath);
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		if (mFOS != null && dst_width == 640 && dst_height == 360)
//		{
//			try {
//				mFOS.write(dst_yuv, 0, dst_yuv.length);
//				mFOS.flush();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
		
		
		return dst_yuv;
		
	}
	
	public static float BytesPerPixel(int format)
	{
		switch(format)
		{
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:				/*I420 --- YUV4:2:0 */
		    case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:		/*yv12 --- YVU4:2:0 */
		    case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:			/*NV12 --- */
		    case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:	/*NV21 --- */
		    case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar: 
		    case MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar:
		    	return (float) 1.5;
		    	
		    default:
		    	return 0;
		}
	}
}