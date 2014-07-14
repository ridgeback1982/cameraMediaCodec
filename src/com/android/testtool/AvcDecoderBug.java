package com.android.testtool;

public class AvcDecoderBug {
	private int mFrameCount = 0;
	public boolean ShouldDropAvcFrame(int frame_count, int nal_type)
	{
		boolean drop = false;
		if (frame_count > 30)
		{
			//drop sps/pps
//			if (nal_type == AvcUtils.NAL_TYPE_SPS || nal_type == AvcUtils.NAL_TYPE_PPS)
//			{
//				drop  = true;
//			}
			
			//drop idr
//			if (nal_type == AvcUtils.NAL_TYPE_CODED_SLICE_IDR)
//			{
//				drop = true;
//			}
			
			//drop p
//			if (nal_type == AvcUtils.NAL_TYPE_CODED_SLICE)
//			{
//				if (frame_count % 150 == 0)
//				{
//					drop = true;
//				}
//			}
		}
		mFrameCount = frame_count;
		
		return drop;
	}
}