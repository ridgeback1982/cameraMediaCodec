package com.android.testtool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import android.util.Log;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.hardware.Camera.CameraInfo;
import android.os.Build;

public class CameraInfoCollector 
{
	Camera	mCamera;

	int sdkInt;
	List<int[]> tempFpsList;
	//boolean allow_debug = true;
	private final String log_tag = "TestCamera";
	int camIndex = 0;
	public CameraInfoCollector ()
	{
		mCamera = null;

		sdkInt = Build.VERSION.SDK_INT;
		tempFpsList = null;

	}
//	public void setDebugLog(boolean bOutputLog)
//	{
//		allow_debug = bOutputLog;
//	}
	public void setCamIndex(int index)
	{
		camIndex = index;
	}
	public Camera getCamera()
	{
		return mCamera;
	}
	
	public void exception_test()
	{
		Log.i(log_tag, "exception_test start");
		Parameters para = null;
		
		try {
			para = mCamera.getParameters();
			if (para == null)
			{
				Log.w(log_tag, "getParameters NULL");
				return;
			}	
		}
		catch(Exception ex)
		{
			Log.w(log_tag, "getParameters exception");
			return;
		}
		
		//1. test color formats
		if (para != null)
		{
			int cf = 0;
			try {
				List<Integer> list_integer = para.getSupportedPreviewFormats();
				if (list_integer == null)
				{
					Log.w(log_tag, "getSupportedPreviewFormats NULL");
					
				}
			}
			catch(Exception ex)
			{
				Log.w(log_tag, "getSupportedPreviewFormats exception");
			}
			
			try {
				cf = para.getPreviewFormat();
			}
			catch(Exception ex)
			{
				Log.w(log_tag, "getPreviewFormat exception");
			}
			
			Log.w(log_tag, "setPreviewFormat cf="+cf);
			
			try{
				para.setPreviewFormat(cf);
			}
			catch(Exception ex)
			{
				Log.w(log_tag, "setPreviewFormat exception");
			}
		}
		
		//2. test fps
		if (para != null)
		{
			List<int[]> list_range;
			try {
				list_range = para.getSupportedPreviewFpsRange();
				if (list_range == null)
				{
					Log.w(log_tag, "getSupportedPreviewFpsRange NULL");
				}
			}
			catch(Exception ex)
			{
				Log.w(log_tag, "getSupportedPreviewFpsRange exception");
			}
			
			int[] range = new int[2];
			try {
				para.getPreviewFpsRange(range);
			}
			catch(Exception ex)
			{
				Log.w(log_tag, "getPreviewFpsRange exception");
			}
			
			try {
				para.setPreviewFpsRange(range[0], range[1]);
			}
			catch(Exception ex)
			{
				Log.w(log_tag, "setPreviewFpsRange exception");
			}
			
			try{
				List<Integer> list_rate = para.getSupportedPreviewFrameRates();
				if (list_rate == null)
				{
					Log.w(log_tag, "getSupportedPreviewFrameRates NULL");
				}
			}
			catch(Exception ex)
			{
				Log.w(log_tag, "getSupportedPreviewFrameRates exception");
			}
			
			int rate = 0;
			try {
				rate = para.getPreviewFrameRate();
			}
			catch(Exception ex)
			{
				Log.w(log_tag, "getPreviewFrameRate exception");
			}
			
			try {
				para.setPreviewFrameRate(rate);
			}
			catch(Exception ex)
			{
				Log.w(log_tag, "setPreviewFrameRate exception");
			}
		}
		
		//3. size
		if (para != null)
		{
			List<Size> size_list = null;
			try {
				size_list = para.getSupportedPreviewSizes();
				if (size_list == null)
				{
					Log.w(log_tag, "getSupportedPreviewSizes NULL");
				}
				else
				{
					Log.i(log_tag, "getSupportedPreviewSizes size="+size_list.size());
					for(Size siz:size_list)
					{
						Log.i(log_tag, "getSupportedPreviewSizes, w="+siz.width+",h="+siz.height);
					}
				}
			}
			catch(Exception ex)
			{
				Log.w(log_tag, "getSupportedPreviewSizes exception");
			}
			
			Size siz = null;
			try {
				siz = para.getPreviewSize();
				if (siz == null)
				{
					Log.w(log_tag, "getPreviewSize NULL");
				}
				else
				{
					Log.i(log_tag, "getPreviewSize siz, w="+siz.width+",h="+siz.height);
				}
			}
			catch(Exception ex)
			{
				Log.w(log_tag, "getPreviewSize exception");
			}
			
			try {
				para.setPreviewSize(siz.width, siz.height);
			}
			catch(Exception ex)
			{
				Log.w(log_tag, "setPreviewSize exception");
			}
			
		}
		
		Log.i(log_tag, "exception_test end");
	}
	
	public boolean init()
	{
		Log.i(log_tag, "init begin, sdkInt="+sdkInt);
		if (sdkInt >= 9)
		{
			//Log.i(log_tag, "init for api>=9");
			try {
				mCamera = Camera.open(camIndex);
				//Log.i(log_tag, "init for api>=9, mCamera="+mCamera+"camIndex="+camIndex);
			}
			catch(RuntimeException ex)
			{
				Log.e(log_tag,"open camera fail");
				return false;
			}
			
//			if (mCamera != null)
//			{
//				Parameters param = mCamera.getParameters();
//				Log.i(log_tag, "after open, param="+param);
//			}
		}
		else
		{
			Log.i(log_tag, "init for api<9");
			
				//Log.i(log_tag, "	init for other, mCamera="+mCamera);
				try {
					mCamera = Camera.open();
				}
				catch(RuntimeException ex)
				{
					//Log.e(log_tag,"		open camera fail");
					return false;
				}
				//Log.i(log_tag, "	after open, mCamera="+mCamera);
				
//				if (mCamera != null)
//				{
//					Parameters param = mCamera.getParameters();
//					Log.i(log_tag, "	after open, param="+param);
//				}
		}
		
		return true;
	}
	
	public void uninit()
	{

				mCamera.release();
				mCamera = null;

		
		
		if (tempFpsList != null)
		{
			tempFpsList.clear();
			tempFpsList = null; 
		}
	}
	
//	public int getNumOfCameras()
//	{
//		if (allow_debug == true)
//			Log.i(log_tag, "Camera count:"+mNumOfCameras);
//		return mNumOfCameras;
//	}
	
	public int getFacing(boolean allow_debug)
	{
		if (sdkInt >= 9)
		{
			CameraInfo caminfo = new CameraInfo();
			Camera.getCameraInfo(camIndex, caminfo);
			if (allow_debug == true)
				Log.i(log_tag, "Facing:"+caminfo.facing);
			return caminfo.facing;
		}
		else
		{
			
				return 0;
		}
	}
	
	public int getOrientation(boolean allow_debug)
	{
		if (sdkInt >= 9)
		{
			CameraInfo caminfo = new CameraInfo();
			Camera.getCameraInfo(camIndex, caminfo);
			if (allow_debug == true)
			{
				Log.i(log_tag, "Orientation:"+caminfo.orientation);
			}
			return caminfo.orientation;
		}
		else
		{
			
				return 0;
		
		}
	}
	
	public List<Integer> getSupportedPreviewFormats(boolean allow_debug)
	{
		Log.i(log_tag, "getSupportedPreviewFormats, mCamera="+mCamera);
		Parameters para = mCamera.getParameters();
		Log.i(log_tag, "getSupportedPreviewFormats, para="+para);
		
		if (allow_debug == true)
		{
			List<Integer> list_integer = para.getSupportedPreviewFormats();
			
			Iterator<Integer> ite = list_integer.iterator();
			while(ite.hasNext())
			{
				Integer cf = ite.next();
				int first_index = list_integer.indexOf(cf);
				int last_index = list_integer.lastIndexOf(cf);
				if (first_index != last_index)
				{
					ite.remove();
					//list_integer.remove(cf);
				}
			}
			
			for (Integer intt : list_integer)
			{
				Log.i(log_tag,"supported format:"+intt.intValue());
			}
			
			return list_integer;
		}
		else
			return para.getSupportedPreviewFormats();
	}
	
	public List<int[]> getSupportedPreviewFpsRanges(boolean allow_debug)
	{
		Parameters para = mCamera.getParameters();
		//tempFpsList = new ArrayList<int[]>();
		List<Integer> fps_list = para.getSupportedPreviewFrameRates();
		for (int fps : fps_list)
		{

			if (allow_debug == true)
			{
				Log.i(log_tag,"supported fps:"+fps);
			}
		}
		
		
		
		if (sdkInt >= 9)
		{
			if (allow_debug == true)
			{
				//List<Integer> fps_list = parameters.getSupportedPreviewFrameRates();
				List<int[]> fps_range = para.getSupportedPreviewFpsRange();
				
				Iterator<int[]> ite = fps_range.iterator();
				while(ite.hasNext())
				{
					int[] fps = ite.next();
					Iterator<int[]> ite_in = fps_range.iterator();
					while(ite_in.hasNext())
					{
						int[] fps_in = ite_in.next();
						if (fps_in.equals(fps))
						{
							continue;
						}
						if (fps[Parameters.PREVIEW_FPS_MAX_INDEX] <= fps_in[Parameters.PREVIEW_FPS_MAX_INDEX] &&
							fps_in[Parameters.PREVIEW_FPS_MIN_INDEX] <= fps[Parameters.PREVIEW_FPS_MIN_INDEX])
						{
							ite.remove();
							break;
						}
					}
				}
				
				
				for (int[] fps : fps_range)
				//for (int fps : fps_list)
				{
					Log.i(log_tag,"supported fps range:"+fps[Parameters.PREVIEW_FPS_MIN_INDEX]+"~"+fps[Parameters.PREVIEW_FPS_MAX_INDEX]);
				}
				return fps_range;
			}
			else
				return para.getSupportedPreviewFpsRange();
		}
		else
		{
			List<int[]> fps_range = new ArrayList<int []>();
			int[] one_range = new int[2];
			one_range[0] = 1000;
			one_range[1] = 30000;
			fps_range.add(one_range);
			Log.i(log_tag,"supported fps range:"+one_range[0]+"~"+one_range[1]);
			
			return fps_range;
		}
	}
	
	public List<Size> getSupportedPreviewSizes(boolean allow_debug)
	{
		Parameters para = mCamera.getParameters();
		
		if (allow_debug == true)
		{
			List<Size> size_list = para.getSupportedPreviewSizes();
			for(Size siz : size_list)
			{
				Log.i(log_tag,"supported size:"+siz.width+"x"+siz.height);
			}
			return size_list;
		}
		else
			return para.getSupportedPreviewSizes();
	}
	
	
	
}