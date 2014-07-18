package com.android.testtool;

import java.nio.ByteBuffer;

import android.util.Log;


public class AvcUtils {
	
	public static final int R_BUFFER_OK = 0;
	public static final int R_TRY_AGAIN_LATER = -1;
	public static final int R_OUTPUT_UPDATE = -2;
	public static final int R_INVALID_STATE = -3;
	public static final int R_INVALIDATE_BUFFER_SIZE = -10;
	public static final int R_UNKNOWN = -40;
	
	public static final int START_PREFIX_CODE = 0x00000001;
	public static final int START_PREFIX_LENGTH = 4;
	public static final int NAL_UNIT_HEADER_LENGTH = 1;
	public static final int NAL_TYPE_UNSPECIFY				= 0x00;
	public static final int NAL_TYPE_CODED_SLICE 			= 0x01;
	public static final int NAL_TYPE_CODED_SLICE_IDR 		= 0x05;
	public static final int NAL_TYPE_SEI 					= 0x06;
	public static final int NAL_TYPE_SPS 					= 0x07;
	public static final int NAL_TYPE_PPS 					= 0x08;
	public static final int NAL_TYPE_SUBSET_SPS 			= 0x0f;
	
	public static boolean goToPrefix(final ByteBuffer buffer)
	{
		int presudo_prefix = 0xffffffff;
		while(buffer.hasRemaining())
		{
			presudo_prefix = (presudo_prefix << 8) | (buffer.get() & 0xff);
			if (presudo_prefix == START_PREFIX_CODE)
			{
				return true;
			}
		}
		return false;
	}
	
	public static int getNalType(final ByteBuffer buffer)
	{
		return buffer.get() & 0x1f;
	}
	
	public static int getGolombUE(final BitBufferLite bitb) {
		int leadingZeroBits = 0;
		while (!bitb.getBit()) {
			leadingZeroBits++;
		}
		final int suffix = bitb.getBits(leadingZeroBits);
		final int minimum = (1 << leadingZeroBits) - 1;
		return minimum + suffix;
	}
	
	//TODO: need support extra profile_idc and pic_order_cnt_type
	//usage: int[] width = new int[1];
	//sps should contains 00 00 00 01 67 ......
	public static void parseSPS(/*in*/byte[] sps, /*out*/int[] width, /*out*/int[] height)	//sps buffer doesn't include nal-type byte
	{
		ByteBuffer byteb = ByteBuffer.wrap(sps);
		if (false == goToPrefix(byteb) || NAL_TYPE_SPS != getNalType(byteb))
			return;
		
		BitBufferLite bitb = new BitBufferLite(byteb);
		
		int profile_idc = bitb.getBits(8);				//profile idc
		bitb.getBits(16);								//constraint_set0...,
		getGolombUE(bitb);
		if (profile_idc == 100 || profile_idc == 110 || profile_idc == 122
			|| profile_idc == 244 || profile_idc == 44 || profile_idc == 83
			|| profile_idc == 86 || profile_idc == 118 || profile_idc == 128) 
		{
			Log.e("AvcUtils", "SPS parsing do not support such profile idc, "+profile_idc);
			throw new UnsupportedOperationException("Profile idc NOT supported yet.");
		}
		int log2_max_frame_num_minus4 = getGolombUE(bitb);
		int pic_order_cnt_type = getGolombUE(bitb);
		if (pic_order_cnt_type == 0)
		{
			int log2_max_pic_order_cnt_lsb_minus4 = getGolombUE(bitb);
		}
		else if (pic_order_cnt_type == 1)
		{
			Log.e("AvcUtils", "SPS parsing do not support such pic_order_cnt_type, "+pic_order_cnt_type);
			throw new UnsupportedOperationException("pic_order_cnt_type NOT supported yet.");
		}
		else
		{
			//pic_order_cnt_type shall be "2", do nothing
		}
		
		int num_ref_frames = getGolombUE(bitb);
		int gaps_in_frame_num_value_allowed_flag = bitb.getBits(1);	//1 bit
		
		//KEY POINT
		int pic_width_in_mbs_minus1 = getGolombUE(bitb);
		width[0] = (pic_width_in_mbs_minus1 + 1) * 16;
		int pic_height_in_map_units_minus1 = getGolombUE(bitb);
		height[0] = (pic_height_in_map_units_minus1 + 1) * 16;
		
		//over
		return;
	}
}