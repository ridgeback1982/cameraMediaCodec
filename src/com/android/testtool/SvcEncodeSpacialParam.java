package com.android.testtool;


public class SvcEncodeSpacialParam implements Cloneable{
	public int 		mWidth;
	public int 		mHeight;
	public int		mFrameRate;
	public int		mBitrate;
	
	//fix me, it is not general SVC encoder logic
	public int		mStreamID;
	public int		mModeIdx;
	
	public SvcEncodeSpacialParam clone() {  
		SvcEncodeSpacialParam o = null;  
        try {  
            o = (SvcEncodeSpacialParam) super.clone();  
        } catch (CloneNotSupportedException e) {  
            e.printStackTrace();  
        }  
        return o;  
    }  
	
	public boolean isMeValid()
	{
		if (mWidth != 0 && mHeight != 0 && mFrameRate != 0&& mBitrate != 0)
			return true;
		else
			return false;
	}
}