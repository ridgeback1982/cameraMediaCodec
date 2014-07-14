package com.android.testtool;

import java.nio.ByteBuffer;

import android.media.MediaCodecInfo;



class YuvUtils {
	public static int YuvDownsample(int src_format, byte[] src_yuv, int src_width, int src_height, byte[] dst_yuv, int dst_width, int dst_height)
	{
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
			    {
			    	//not supported currently
			    	dst_yuv = null;
			    }
			    break;
			    
			    default:
			    {
			    	dst_yuv = null;
			    }
			    break;
			}
		}
		
		
		
		return 0;
		
	}
	
	public static float BytesPerPixel(int format)
	{
		switch(format)
		{
			case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:				/*I420 --- YUV4:2:0 */
		    case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:		/*yv12 --- YVU4:2:0 */
		    case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:			/*NV12 --- */
		    case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:	/*NV21 --- */
		    	return (float) 1.5;
		    	
		    default:
		    	return 0;
		}
	}
}