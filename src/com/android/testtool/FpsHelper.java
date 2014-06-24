package com.android.testtool;


//this tool can only filter average distributed frames. If some frames are tight and some are loose, it cannot handle the case.
public class FpsHelper {
	
	boolean mEnableDrop = false;
	int mTargetFps = 0;
	long mResetTimestamp = 0;
	long mPassedFrames = 0;
	
	public void SetEnableDrop(boolean enable)
	{
		mEnableDrop = enable;
	}
	
	public void SetFrameRateControlTarget(int fps)
	{
		mTargetFps = fps;
		mResetTimestamp = 0;
		mPassedFrames = 0;
	}
	
	public boolean ShouldBeDropped(long timestamp)
	{
		if (mEnableDrop == false)
		{
			return false;
		}
		
		if (mResetTimestamp == 0 || 
			(mResetTimestamp!= 0 && timestamp < mResetTimestamp)
			)
		{
			mPassedFrames = 0;
			mResetTimestamp = timestamp;
			return false;
		}
		
		long delta = timestamp - mResetTimestamp;
		long framesShouldPass = delta * mTargetFps / 1000;
		
		if (framesShouldPass > mPassedFrames)
		{
			mPassedFrames ++;
			return false;
		}
		else
		{
			return true;
		}
	}
	
	
}