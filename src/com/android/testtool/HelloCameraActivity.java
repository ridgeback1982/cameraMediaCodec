package com.android.testtool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.android.testtool.R;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.MediaCodecInfo;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

public class HelloCameraActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback, Camera.ErrorCallback ,RadioGroup.OnCheckedChangeListener
			, AvcEncoderSink, CheckBox.OnCheckedChangeListener
{
	private MediaRecorder mRecorder;
	private SurfaceView mSurfacePreview;
	private SurfaceTexture mSurfaceTexture;
	HelloCameraActivity mThis = this;
	private Button mBtnTestSpecifiedParam;
	private Button mBtnSetparam;
	private Button mBtnTestFPS;
	private Button mBtnTestResetPreview;
	private Spinner mSpnColorFormat;
	private Spinner mSpnSize;
	private Spinner mSpnFacing;
	private Spinner mSpnFPS;
	private CheckBox mCBVideoEncode;
	private CheckBox mCBAvcGotoFile;
	private EditText mETFps;
	private EditText mETBps;	//kbps
	boolean mUseSurfaceTexture;
	private OnClickListener OnClickEvent;
	private final String log_tag = "TestCamera";
	private int yy;
	private int mRawHeight;
	private int mRawWidth;
	int mEncode_fps;
	int mEncode_bps;
	
	private boolean m_bStartPreview = false;
	
	List<Integer> mListPreviewFormats_front;
	List<Integer> mListPreviewFps_front;
	List<int[]>	mListPreviewSizes_front;	//0-w, 1-h
	List<Integer> mListPreviewFormats_back;
	List<Integer> mListPreviewFps_back;
	List<int[]>	mListPreviewSizes_back;		//0-w, 1-h
	List<String> mList_string_facing;	//temp
	
	int mSelectedFacing = 0;
	int mSelectColorFormat = 17;
	int mSelectWidth = 320;
	int mSelectHeight = 240;
	int mSelectFPS = 25;
	
	boolean mTestingFPS = false;
	Camera mCam = null;
	long mLastTestTick = 0;
	int mLastTestCount = 0;
	//boolean mShowPreview = true;
	Context mCtx;
	boolean mUsePreviewBuffer;
	ByteBuffer mPreviewBuffer;
	
	private Handler mEventHandler;
	final int EVENT_SCREEN_ROTATE_CAMERA_REFRESH = 1;
	final int EVENT_CALC_ENCODE_FPS = 2;
	
	long mPreviewGap_starttick = 0;
	
	AvcEncoder mAvcEnc = null;
	boolean mEnableVideoEncode;
	private byte[] mAvcBuf = null;
	private final static Object mAvcEncLock = new Object();  
	private CodecThread m_codec_thread = null;
    private Handler m_CodecMsgHandler = null;
    private int mEncCountPerSecond = 0;
    private int mEncBytesPerSecond = 0;
    long mAvcEncodeFirstOutputGap_starttick = 0;
    boolean mAvcGotoFile;
    int	mPeriodKeyFrame = -1;	//ms
    private FileOutputStream mFOS = null;
    byte[] mRawData = null;
    Queue<byte[]> mPreviewBuffers = null;
    
    //private boolean mAvcEncOutputSuspend = false;
    
    private final int EVENT_GET_ENCODE_OUTPUT = 1;
    private final int EVENT_REQUEST_KEY_FRAME = 2;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(log_tag, "onCreate ++");
        super.onCreate(savedInstanceState);
        
      	this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      	this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      	getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        setContentView(R.layout.main);
        //test
      	Build bd = new Build();
      	Log.i(log_tag,"Device Info: model="+bd.MODEL+",product="+bd.PRODUCT+",device="+bd.DEVICE+",manufa="+bd.MANUFACTURER+",brand="+bd.BRAND+",HW="+bd.HARDWARE+",ID="+bd.ID+ ",Display=" + bd.DISPLAY
      			+ ",SDK ver=" + Build.VERSION.SDK_INT
      			+ ", SDK release =" + Build.VERSION.RELEASE);

		//test
		String dev_model = bd.MODEL;
		
		mCtx = this.getBaseContext();
        
        
        OnClickEvent = new OnClickListener(){
            public void onClick(View v) {
                //...
            	switch(v.getId())
            	{
            	case R.id.button_testspecifiedparam:
	            	{
	            		int numOfCameras = 0;
	            		int sdkInt = Build.VERSION.SDK_INT;
	            		if (sdkInt >= 9)
	            			numOfCameras = Camera.getNumberOfCameras();
	            		else
	            			numOfCameras = 1;
	            		
	            		Log.i(log_tag, "TestSpecifiedParam, set some sizes");
	            		List<Rect> list_rect_test = new ArrayList<Rect>();
	            		list_rect_test.clear();
	            		list_rect_test.add(new Rect(0, 0, 160, 90));	//left, top, right, bottom
	                    list_rect_test.add(new Rect(0, 0, 320, 180));
	                    list_rect_test.add(new Rect(0, 0, 640, 360));
	                    list_rect_test.add(new Rect(0, 0, 1280, 720));
	                    
	            		for(int i=0;i<numOfCameras;i++)
	            		{
	            			CameraInfoCollector collector = new CameraInfoCollector();
	            			//collector.setDebugLog(true);
	            			collector.setCamIndex(i);
	            			collector.init();
	            			TestSpecifiedParam(collector, list_rect_test);
	            			collector.uninit();
	            			collector = null;
	            		}
	            	}
	            	Log.i(log_tag,"test of specified param is over");
            		break;
            	case R.id.button_setparam:
	            	{
	            		int numOfCameras = 0;
	            		int sdkInt = Build.VERSION.SDK_INT;
	            		if (sdkInt >= 9)
	            		{
	            			numOfCameras = Camera.getNumberOfCameras();
	            		}
	            		else
	            		{
	            			
	            			
	            				numOfCameras = 1;
	            			
	            		}
	            		Log.i(log_tag,"sdkInt ="+sdkInt+".MODEL="+Build.MODEL+". num of camera is "+numOfCameras);
	            		
	            		//fill facing spinner
	            		mList_string_facing = new ArrayList<String>();
	            		
	            		Log.i(log_tag, "Test supported param");
	            		for(int i=0;i<numOfCameras;i++)
	            		{
	            			CameraInfoCollector collector = new CameraInfoCollector();
	            			//collector.setDebugLog(true);
	            			collector.setCamIndex(i);
	            			collector.init(); 
	            			
	            			
	            			{
	            				collector.exception_test();
	            			}
	            			
	            			
	            			int facing = collector.getFacing(false);
	            			mList_string_facing.add(new String(""+facing));
	            			
	            			if (facing == 1)
	            				GetherCameraInfoAndSetparam(collector,mListPreviewFormats_front,mListPreviewFps_front,mListPreviewSizes_front);
	            			else
	            				GetherCameraInfoAndSetparam(collector,mListPreviewFormats_back,mListPreviewFps_back,mListPreviewSizes_back);
	            			collector.uninit();
	            			collector = null;
	            		}
	            		
	            		FillSpinner_Facing(mList_string_facing);
	            		mSpnFacing.setVisibility(View.VISIBLE);
	            		
	            	}
	            	Log.i(log_tag,"test supported param is over");
            		break;
            	case R.id.button_testFPS:
            		if (mTestingFPS == false)
            		{
            			try {
							TestFPS();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
            			mBtnTestFPS.setText("Stop");
            			mTestingFPS = true;
            			m_bStartPreview = true;
            		}
            		else
            		{
            			if (mCam != null)
            			{
            				try {
            					
            					stopPreview();
	            				
	            				Log.i(log_tag, "before camera release");
	            				mCam.release();
	            				Log.i(log_tag, "after camera release");
            				}
            				catch(Exception ex)
            				{
            					ex.printStackTrace();
            				}
            				mCam = null;
            			}
            			mBtnTestFPS.setText("Begin test");
            			mTestingFPS = false;
            			m_bStartPreview = false;
            			setCaptureFPS_TextView(0, 0, 0);
            		}
            		break;
            		
            	}
            	//Log.i(log_tag,"onClick");
            }
        };
        
        mSurfaceTexture = new SurfaceTexture(0);
        
        
        mListPreviewFormats_front = new ArrayList<Integer>();
    	mListPreviewFps_front = new ArrayList<Integer>();
    	mListPreviewSizes_front = new ArrayList<int[]>();
    	mListPreviewFormats_back = new ArrayList<Integer>();
    	mListPreviewFps_back = new ArrayList<Integer>();
    	mListPreviewSizes_back = new ArrayList<int[]>();

    	mSurfacePreview = (SurfaceView)findViewById(R.id.surfaceView_preview);
    	SurfaceHolder sh = mSurfacePreview.getHolder();
    	Log.i(log_tag, "new mSurfacePreview="+sh);
    	sh.addCallback(this);
    	//sh.setFormat(android.graphics.PixelFormat.RGB_565);
    	//sh.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mBtnTestSpecifiedParam = (Button)findViewById(R.id.button_testspecifiedparam);
    	mBtnSetparam = (Button)findViewById(R.id.button_setparam);
    	mBtnTestFPS = (Button)findViewById(R.id.button_testFPS);
    	mBtnTestResetPreview = (Button)findViewById(R.id.button_resetPreview);
    	mBtnTestSpecifiedParam.setOnClickListener(OnClickEvent);
    	mBtnSetparam.setOnClickListener(OnClickEvent);
    	mBtnTestFPS.setOnClickListener(OnClickEvent);
    	mBtnTestResetPreview.setOnClickListener(OnClickEvent);
    	mBtnTestResetPreview.setVisibility(View.INVISIBLE);
    	
    	mSpnColorFormat = (Spinner)findViewById(R.id.Spinner_cf);
    	mSpnSize = (Spinner)findViewById(R.id.Spinner_size);
    	mSpnFacing = (Spinner)findViewById(R.id.Spinner_facing);
    	mSpnFPS = (Spinner)findViewById(R.id.Spinner_fps);
    	mSpnColorFormat.setOnItemSelectedListener(new SpinnerSelectedListener());
    	mSpnSize.setOnItemSelectedListener(new SpinnerSelectedListener());
    	mSpnFacing.setOnItemSelectedListener(new SpinnerSelectedListener());
    	mSpnFPS.setOnItemSelectedListener(new SpinnerSelectedListener());
    	mSpnColorFormat.setVisibility(View.INVISIBLE);
		mSpnSize.setVisibility(View.INVISIBLE);
		mSpnFacing.setVisibility(View.INVISIBLE);
		mSpnFPS.setVisibility(View.INVISIBLE);
		
		//set Spinner's width dynamically
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int rot = getWindowManager().getDefaultDisplay().getRotation();
		if (rot == Surface.ROTATION_0 || rot == Surface.ROTATION_180)
		{
			mSpnColorFormat.setMinimumWidth(dm.widthPixels/4);
			mSpnSize.setMinimumWidth(dm.widthPixels/4);
			mSpnFacing.setMinimumWidth(dm.widthPixels/4);
			mSpnFPS.setMinimumWidth(dm.widthPixels/4);
		}
		else
		{
			mSpnColorFormat.setMinimumWidth(dm.heightPixels/4);
			mSpnSize.setMinimumWidth(dm.heightPixels/4);
			mSpnFacing.setMinimumWidth(dm.heightPixels/4);
			mSpnFPS.setMinimumWidth(dm.heightPixels/4);
		}
		
		
		
		//radiogroup1
		mUseSurfaceTexture = false;
		RadioGroup  radiogroup1=(RadioGroup)findViewById(R.id.radiogroup1);
    	RadioButton radio_sv;
		RadioButton radio_st;
    	radio_sv=(RadioButton)findViewById(R.id.RadioButton_surfaceview);
    	radio_sv.setChecked(true);
    	radio_st=(RadioButton)findViewById(R.id.radioButton_surfacetexture);
    	radio_st.setChecked(false);
    	radiogroup1.setOnCheckedChangeListener(this);  
    	
    	//radiogroup2
    	mUsePreviewBuffer = false;
    	RadioGroup  radiogroup2=(RadioGroup)findViewById(R.id.radiogroup2);
    	RadioButton radio_AllocPB;
		RadioButton radio_NotAllocPB;
		radio_AllocPB=(RadioButton)findViewById(R.id.RadioButton_AllocPB);
		radio_AllocPB.setChecked(false);
		radio_NotAllocPB=(RadioButton)findViewById(R.id.radioButton_NotAllocPB);
		radio_NotAllocPB.setChecked(true);
    	radiogroup2.setOnCheckedChangeListener(this);  
    	
    	mETFps = (EditText)findViewById(R.id.FPSedit);
    	mETBps = (EditText)findViewById(R.id.BPSedit);
    	
    	
    	mEnableVideoEncode = false;
    	mPeriodKeyFrame = -1;		//test for request key frame, ms period
    	mAvcGotoFile = false;		//really for debug, write files
    	mRawHeight = 0;
    	mRawWidth = 0;
    	mEncode_fps = 18;
    	mEncode_bps = 440000;
    	
    	
    	mCBVideoEncode = (CheckBox)findViewById(R.id.checkBoxEnableVideoEncode);
		mCBVideoEncode.setChecked(mEnableVideoEncode);
		mCBVideoEncode.setOnCheckedChangeListener(this);
		
		mCBAvcGotoFile = (CheckBox)findViewById(R.id.checkBoxAvcGotoFile);
		mCBAvcGotoFile.setChecked(mAvcGotoFile);
		mCBAvcGotoFile.setOnCheckedChangeListener(this);
    	
	 	
	 	mEventHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case EVENT_SCREEN_ROTATE_CAMERA_REFRESH: 
					Log.i(log_tag,"handleMessage:SCREEN_ROTATE_CAMERA_REFRESH");

					
					removeMessages(EVENT_SCREEN_ROTATE_CAMERA_REFRESH);
					break;
					
				case EVENT_CALC_ENCODE_FPS:
					setEncodeFPS_TextView(mEncCountPerSecond, mRawWidth, mRawHeight, mEncBytesPerSecond*8);
					mEncCountPerSecond = 0;
					mEncBytesPerSecond = 0;
					sendEmptyMessageDelayed(EVENT_CALC_ENCODE_FPS, 1000);
					break;
				}
			}
		};
		
		Log.d(log_tag, "onCreate --");
    }
    
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
    	int i=0;
    	Log.i(log_tag, "surfaceChanged, holder="+holder+",surface valid="+holder.getSurface().isValid());
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        int i=0;
        Log.i(log_tag, "surfaceCreated, holder="+holder+",surface valid="+holder.getSurface().isValid());
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
    	Log.i(log_tag, "surfaceDestroyed, holder="+holder+",surface valid="+holder.getSurface().isValid());
    }
    
    class SpinnerSelectedListener implements OnItemSelectedListener{  
    	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,  long arg3) {   
    		
    		boolean try_runtume = false;
    		
    		switch(arg0.getId())
    		{
    		case R.id.Spinner_facing:
    			
    			String str = mList_string_facing.get(arg2);
    			if (str.equals("0"))
    			{
    				
	    			//fill spinner
    				int siz = mListPreviewFormats_back.size();
    				Log.i("xxx","size="+siz);
	        		FillSpinner_CF_Size_FPS(mListPreviewFormats_back, mListPreviewSizes_back, mListPreviewFps_back);
	        		mSelectedFacing = 0;
    			}
    			else
    			{
    				FillSpinner_CF_Size_FPS(mListPreviewFormats_front, mListPreviewSizes_front, mListPreviewFps_front);
    				mSelectedFacing = 1;
    			}
    			
    			mSpnColorFormat.setVisibility(View.VISIBLE);
        		mSpnSize.setVisibility(View.VISIBLE);
        		mSpnFPS.setVisibility(View.VISIBLE);
    			break;
    		case R.id.Spinner_cf:
    			if (mSelectedFacing == 0)
    			{
    				mSelectColorFormat = mListPreviewFormats_back.get(arg2).intValue();
    			}
    			else
    			{
    				mSelectColorFormat = mListPreviewFormats_front.get(arg2).intValue();
    			}
    			try_runtume = true;
    			break;
    		case R.id.Spinner_size:
    			if (mSelectedFacing == 0)
    			{
    				int[] siz_wh = mListPreviewSizes_back.get(arg2);
    				mSelectWidth = siz_wh[0];
    				mSelectHeight = siz_wh[1];
    			}
    			else
    			{
    				int[] siz_wh = mListPreviewSizes_front.get(arg2);
    				mSelectWidth = siz_wh[0];
    				mSelectHeight = siz_wh[1];
    			}
    			try_runtume = true;
    			break;
    		case R.id.Spinner_fps:
    			if (mSelectedFacing == 0)
    			{
	    			mSelectFPS = mListPreviewFps_back.get(arg2).intValue();
    			}
    			else {
    				mSelectFPS = mListPreviewFps_front.get(arg2).intValue();
    			}
    			try_runtume = true;
	    		break;
    		}
    		
    		if (mTestingFPS == true && try_runtume == true)
    		{
    			mPreviewGap_starttick = System.currentTimeMillis();
    			
    			stopPreview();
    			
    			try {
					startPreview();
    				//TestFPS();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
	     }  
    	
	     public void onNothingSelected(AdapterView<?> arg0) {  
	    	 switch(arg0.getId())
	    		{
	    		case R.id.Spinner_facing:
	    			Log.i("xxx","Spinner_facing not select");
	    			break;
	    		case R.id.Spinner_cf:
	    			Log.i("xxx","Spinner_cf not select");
	    			break;
	    		case R.id.Spinner_size:
	    			Log.i("xxx","Spinner_size not select");
	    			break;
	    		}
	     }  
    }  

    @Override
    public void onPause()
    {
    	Log.i(log_tag, "onPause ++");
    	super.onPause();
    	
    	if (mCam != null)
		{
    		Log.i(log_tag, "onPause, before stopPreview");
			mCam.stopPreview();
    		if (mUsePreviewBuffer == true)
				mCam.setPreviewCallbackWithBuffer(null);
			else
				mCam.setPreviewCallback(null);
			
			Log.i(log_tag, "onPause, before release");
			mCam.release();
			Log.i(log_tag, "onPause, after release");
			mCam = null;
		}
		mBtnTestFPS.setText("Begin test");
		mTestingFPS = false;
		setCaptureFPS_TextView(0, 0, 0);
		
		
		if (mEnableVideoEncode == true)
		{
			setEncodeFPS_TextView(0, 0, 0, 0);
			if (mEventHandler != null)
			{
				mEventHandler.removeMessages(EVENT_CALC_ENCODE_FPS);
			}
			
			totalStopAvcEncode();
		}
		
		Log.i(log_tag, "onPause --");
    }
    
    @Override
    protected void onResume() {
    	if(getRequestedOrientation()!=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
    		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    	}

    	super.onResume();
    }
    
    @Override
    public void onDestroy()
    {
    	Log.i(log_tag, "onDestroy");
    	super.onDestroy();
    }
    
	@Override
	public void onConfigurationChanged(Configuration newConfig) {    
	    super.onConfigurationChanged(newConfig);
	    Log.i(log_tag,"onConfigurationChanged");
	    int sdkInt = Build.VERSION.SDK_INT;
	    int camIndex = 0;
    	if (mSelectedFacing == 0)
    	{
    		CameraInfo info = new CameraInfo();
    		Camera.getCameraInfo(0, info);
    		if (info.facing == 0)
    			camIndex = 0;
    		else
    			camIndex = 1;
    	}
    	else {
    		CameraInfo info = new CameraInfo();
    		Camera.getCameraInfo(0, info);
    		if (info.facing == 1)
    			camIndex = 0;
    		else
    			camIndex = 1;
    	}
	    if (mCam != null)
	    {
	    	if (sdkInt >= 14)
	    	{
	    		setCameraDisplayOrientation(this, camIndex, mCam);
	    	}
	    	else
	    	{
			    mCam.stopPreview();
			    setCaptureFPS_TextView(0, 0, 0);
			    if (sdkInt >= 9)
			    {
			    	setCameraDisplayOrientation(this, camIndex, mCam);
			    }
			    if (mUsePreviewBuffer == true)
    				mCam.setPreviewCallbackWithBuffer(this);
    			else
    				mCam.setPreviewCallback(this);
		    	mCam.startPreview(); 
	    	}
	    }
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (mCam != null)
			{
				stopPreview();
				
				mCam.release();
				mCam = null;
			}
			break;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	public static void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) 
	{     
		android.hardware.Camera.CameraInfo info =  new android.hardware.Camera.CameraInfo();     
		android.hardware.Camera.getCameraInfo(cameraId, info);     
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;     
		switch (rotation) 
		{         
			case Surface.ROTATION_0: degrees = 0; break;         
			case Surface.ROTATION_90: degrees = 90; break;        
			case Surface.ROTATION_180: degrees = 180; break;        
			case Surface.ROTATION_270: degrees = 270; break;     
		}     
		int result;     
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) 
		{         
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;  // compensate the mirror
		} else {
			// back-facing
			result = (info.orientation - degrees + 360) % 360;     
		}
		camera.setDisplayOrientation(result);
	}
	
	private int getPreviewBufferSize(int width, int height, int format)
	{
		int size = 0;
		switch(format)
		{
		case ImageFormat.YV12:
			{
				int yStride   = (int) Math.ceil(width / 16.0) * 16;
				int uvStride  = (int) Math.ceil( (yStride / 2) / 16.0) * 16;
				int ySize     = yStride * height;
				int uvSize    = uvStride * height / 2;
				size = ySize + uvSize * 2;
			}
			break;
			
		case ImageFormat.NV21:
			{
				float bytesPerPix = (float)ImageFormat.getBitsPerPixel(format) / 8;
				size = (int) (width * height * bytesPerPix);
			}
			break;
		}

		return size;
	}
	
	private void startPreview() throws IOException
	{
		if (mCam == null)
			return;
		
		if (mUseSurfaceTexture == true)
    	{
    		mCam.setPreviewTexture(mSurfaceTexture);
    	}
    	else
    	{
    		mCam.setPreviewDisplay(mSurfacePreview.getHolder());
    	}
    	
    	Camera.Parameters para = mCam.getParameters();
    	try {
    		para.setPreviewFormat(mSelectColorFormat);
        	para.setPreviewSize(mSelectWidth, mSelectHeight);
        	//para.setPreviewFrameRate(mSelectFPS);
        	para.setPreviewFpsRange(mSelectFPS*1000, mSelectFPS*1000);
    		
    		List<String> supportedFocus = para.getSupportedFocusModes();
			if (supportedFocus != null && supportedFocus.indexOf(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) >= 0){
				Log.i(log_tag,"set param, setFocusMode to FOCUS_MODE_CONTINUOUS_VIDEO");
				para.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
	        }
	        
	        mCam.setParameters(para);
    		
    		Log.i(log_tag,"set param, mSelectColorFormat="+mSelectColorFormat+",mSelectWidth="+mSelectWidth+",mSelectHeight="+mSelectHeight+",mSelectFPS="+mSelectFPS);
    	}
    	catch(Exception ex)
    	{
    		//if (ex == )
    		Log.i(log_tag,"camera setparameters exception="+ex);
    	}
    	
    	if (mUsePreviewBuffer == true)
    	{
    		int size = getPreviewBufferSize(mSelectWidth, mSelectHeight, mSelectColorFormat);

    		//mPreviewBuffer = ByteBuffer.allocate(size);
        	//mCam.addCallbackBuffer(mPreviewBuffer.array());
    		
    		//re-create bytes buffer every time. I think the better way is: after stop, camera return all the buffers I give him through "OnPreview" callback.
    		//Then I can select reuse the bytes buffer or re-create them
    		if (mPreviewBuffers == null)
    		{
    			mPreviewBuffers = new LinkedList<byte[]>();
    		}
    		else
    		{
    			mPreviewBuffers.clear();
    		}
    		for(int i=0;i<5;i++)
    		{
    			byte[] mem = new byte[size];
    			mCam.addCallbackBuffer(mem);	//ByteBuffer.array is a reference, not a copy
    		}
    		
			mCam.setPreviewCallbackWithBuffer(this);
			Log.i(log_tag,"alloc preview buffer");
    	}
		else
		{
			Log.i(log_tag,"NOT alloc preview buffer");
			mCam.setPreviewCallback(this);
		}
    	
    	mCam.setErrorCallback(this);
    	
    	Log.i(log_tag,"before startPreview");
    	mCam.startPreview();
    	Log.i(log_tag,"after startPreview");
	}
	
	private void stopPreview()
	{
		if (mCam == null)
			return;
		Log.i(log_tag,"before stopPreview");
		mCam.stopPreview();
		Log.i(log_tag,"after stopPreview");
		
		if (mUsePreviewBuffer == true)
			mCam.setPreviewCallbackWithBuffer(null);	//it will clear all buffers added to camera by me
		else
			mCam.setPreviewCallback(null);		
	}
	
    private void TestFPS() throws IOException
    {
    	int sdkInt = Build.VERSION.SDK_INT;
    	int camIndex = 0;
    	try {
	    	if (sdkInt >= 9)
	    	{
		    	if (mSelectedFacing == 0)
		    	{
		    		CameraInfo info = new CameraInfo();
		    		Camera.getCameraInfo(0, info);
		    		if (info.facing == 0)
		    			camIndex = 0;
		    		else
		    			camIndex = 1;
		    	}
		    	else {
		    		CameraInfo info = new CameraInfo();
		    		Camera.getCameraInfo(0, info);
		    		if (info.facing == 1)
		    			camIndex = 0;
		    		else
		    			camIndex = 1;
		    	}
		    	Log.i(log_tag,"before camera open");
		    	mCam = Camera.open(camIndex);
		    	Log.i(log_tag,"after camera open");
	    	}
	    	else
	    	{
	    		mCam = Camera.open();
	    	}
    	}
    	catch (Exception exception) {
			Log.e(log_tag,"[Error]camera open exception,"+exception.toString());
			return;
		}
    	
    	if (sdkInt >= 9)
    	{
    		setCameraDisplayOrientation(this, camIndex, mCam);
    	}
    	
 
    	startPreview();
    }
    
    private void setCaptureFPS_TextView(int fps, int w, int h)
    {
    	TextView tv = (TextView)findViewById(R.id.textview_capturefps);
    	tv.setText("Capture FPS:"+String.valueOf(fps)+", "+String.valueOf(w)+"x"+String.valueOf(h));
    }
    
    private void setEncodeFPS_TextView(int fps, int w, int h, int bps)
    {
    	TextView tv = (TextView)findViewById(R.id.textview_encodefps);
    	tv.setText("Encode FPS:"+String.valueOf(fps)+", "+String.valueOf(w)+"x"+String.valueOf(h)+", "+bps+"bps");
    }
    
    private void TestSpecifiedParam(CameraInfoCollector collector, List<Rect> list_rect)
    {
    	
    	collector.getFacing(true);	//for log
    	Camera cam = collector.getCamera();
		Parameters para = cam.getParameters();
		for (Rect rec:list_rect)
		{
			para.setPreviewSize(rec.right-rec.left, rec.bottom-rec.top);
			
			boolean setOK = true;
    		try {
    			cam.setParameters(para);
    		}
    		catch(RuntimeException ex)
    		{
    			setOK = false;
    		}
    		if (setOK == true)
    		{
    			Log.i(log_tag, "setParameters: size="+(rec.right-rec.left)+"x"+(rec.bottom-rec.top)+"-----------------OK");
    		}
    		else
    		{
    			Log.e(log_tag, "setParameters: size="+(rec.right-rec.left)+"x"+(rec.bottom-rec.top)+"-----------------FAIL");
    		}
		}
		
    }
    
    private void GetherCameraInfoAndSetparam(CameraInfoCollector collector, List<Integer> ListPreviewFormats_dst, List<Integer> ListPreviewFps_dst, List<int[]> ListPreviewSizes_dst)
    {
    	
    	List<Integer> ListPreviewFormats;
    	List<int[]> ListPreviewFpsRanges;
    	List<Size>	ListPreviewSizes;
    	{
    		Log.i(log_tag, "Group.................................................");
    		collector.getFacing(true);	//for log
    		collector.getOrientation(true); //for log
    		ListPreviewFormats = collector.getSupportedPreviewFormats(true);
    		ListPreviewFpsRanges = collector.getSupportedPreviewFpsRanges(true);
    		
    		Camera cam = collector.getCamera();
    		Parameters para = cam.getParameters();

    		
    		ListPreviewSizes = collector.getSupportedPreviewSizes(true);
    		
    		//copy the List
    		ListPreviewFormats_dst.clear();
    		for(Integer inte:ListPreviewFormats)
    		{
    			Integer intee = new Integer(inte.intValue());
    			if(!ListPreviewFormats_dst.contains(intee))
    			{
    				ListPreviewFormats_dst.add(intee);
    			}
    		}
    		
    		ListPreviewFps_dst.clear();
    		for (int[] ints:ListPreviewFpsRanges)
    		{
    			
    			for (int i=ints[0]/1000;i<=ints[1]/1000;i++)
    			{
    				if (!ListPreviewFps_dst.contains(i))
    				{
    					ListPreviewFps_dst.add(i);
    				}
    			}
    		}
    		
    		ListPreviewSizes_dst.clear();
    		for (Size siz:ListPreviewSizes)
    		{
    			//ziyzhang: cannot use the inner class Size, so use a 2-element array instead
    			int[] siz_wh = new int[2];
    			siz_wh[0] = siz.width;
    			siz_wh[1] = siz.height;
    			if (!ListPreviewSizes_dst.contains(siz_wh))
    			{
    				ListPreviewSizes_dst.add(siz_wh);
    			}
    		}
    	}
    }
    
    private void FillSpinner_Facing(List<String> list_string_facing)
    {
    	ArrayAdapter<String> adapter_facing = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,list_string_facing);
		adapter_facing.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	mSpnFacing.setAdapter(adapter_facing);
    }

    private void FillSpinner_CF_Size_FPS(List<Integer> ListPreviewFormats, List<int[]> ListPreviewSizes, List<Integer> ListPreviewFPS)
    {
    	List<String> list_string_cf = new ArrayList<String>();
    	List<String> list_string_size = new ArrayList<String>();
    	List<String> list_string_FPS = new ArrayList<String>();
    	
    	for(Integer inte:ListPreviewFormats)
    	{
    		switch(inte.intValue())
    		{
    		case 17:
    			{
    			String str = new String("NV21");
    			list_string_cf.add(str);
    			}
    			break;
    		case 842094169:
    			{
    			String str = new String("YV12");
    			list_string_cf.add(str);
    			}
    			break;
    		}
    	}
    	ArrayAdapter<String> adapter_cf = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,list_string_cf);
    	adapter_cf.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	mSpnColorFormat.setAdapter(adapter_cf);
    	//mSpnColorFormat.invalidate();
    	
    	for(int[] siz_wh:ListPreviewSizes)
    	{
    		String str = Integer.toString(siz_wh[0]) + "x" + Integer.toString(siz_wh[1]);
    		list_string_size.add(str);
    	}
    	ArrayAdapter<String> adapter_size = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,list_string_size);
    	adapter_size.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	mSpnSize.setAdapter(adapter_size);
    	
    	for (Integer fps:ListPreviewFPS)
    	{
    		
    		String str = Integer.toString(fps.intValue());
    		list_string_FPS.add(str);
    		
    	}
    	ArrayAdapter<String> adapter_FPS = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,list_string_FPS);
    	adapter_FPS.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	mSpnFPS.setAdapter(adapter_FPS);
    }

	public void onPreviewFrame(byte[] data, Camera camera) {
		// TODO Auto-generated method stub
		//On Samsung GT-P7500(Android4.0.4), "camera.getParameters" will cause ANR
		
		//Size siz = camera.new Size(mSelectWidth, mSelectHeight);
		if (data == null)
		{
			return;
		}
		
		Parameters param = camera.getParameters();
		Size siz = param.getPreviewSize();
		int format = param.getPreviewFormat();
		
		if (siz.height == 0 || siz.width == 0)
		{
			return;
		}
		
		if (mPreviewGap_starttick != 0)
		{
			long endtick = System.currentTimeMillis();
			Log.i(log_tag, "Preview Gap is "+ (endtick-mPreviewGap_starttick));
			mPreviewGap_starttick = 0;
		}

		
		//Log.i(log_tag,"onPreviewFrame called, data="+data);
		long curTick = System.currentTimeMillis();
		if (mLastTestTick == 0)
		{
			mLastTestTick = curTick;
		}
		if (curTick > mLastTestTick + 1000)
		{
			//Log.i(log_tag, "Current FPS = "+mLastTestCount+"(c="+curTick+",p="+mLastTestTick+")");
			setCaptureFPS_TextView(mLastTestCount, siz.width, siz.height); 
			mLastTestCount = 0;
			mLastTestTick = curTick;
		}
		else
			mLastTestCount++;
		
		
		
		//TODO: set config of avc encoder, if need
		
		if (mEnableVideoEncode == true)
		{
			if (mRawHeight != siz.height || mRawWidth != siz.width)
			{
				Log.d(log_tag, "onPreviewFrame, pic size changed to "+siz.width+"x"+siz.height);
				mRawHeight = siz.height;
				mRawWidth = siz.width;
				if (mAvcEnc != null)
				{
					if (mAvcGotoFile == true)
					{
						try {
							if (mFOS != null)
							{
								mFOS.close();
								mFOS = null;
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
	                    try {
	                    	File dir = Environment.getExternalStorageDirectory();
	                    	String fname = "mc_"+Integer.toString(siz.width)+"x"+Integer.toString(siz.height)+".h264";
		                    File filePath = new File(dir, fname);
		                    if (filePath.exists() == true && filePath.isFile() == true)
		                    	filePath.delete();
							mFOS = new FileOutputStream(filePath);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					//mAvcEncOutputSuspend = true;
					mEncCountPerSecond = 0;
					mEncBytesPerSecond = 0;
					mEventHandler.removeMessages(EVENT_CALC_ENCODE_FPS);
					mEventHandler.sendEmptyMessage(EVENT_CALC_ENCODE_FPS);
					
					synchronized(mAvcEncLock) {
						mAvcEncodeFirstOutputGap_starttick = System.currentTimeMillis();
						mAvcEnc.stop();
						mAvcEnc.tryConfig(mRawWidth, mRawHeight, mEncode_fps, mEncode_bps);
						mAvcEnc.start();
					}
					
					if (m_CodecMsgHandler != null)
					{
						m_CodecMsgHandler.removeMessages(EVENT_GET_ENCODE_OUTPUT);
						m_CodecMsgHandler.sendEmptyMessage(EVENT_GET_ENCODE_OUTPUT);
						
						if (mPeriodKeyFrame > 0)
						{
							m_CodecMsgHandler.removeMessages(EVENT_REQUEST_KEY_FRAME);
							m_CodecMsgHandler.sendEmptyMessageDelayed(EVENT_REQUEST_KEY_FRAME, 30);
						}
					}
					
					//mAvcEncOutputSuspend = false;
				}
			}
//			if (mAvcEnc != null)
//			{
//				float bytesPerPix = (float)ImageFormat.getBitsPerPixel(format) / 8;
//				int data_size = (int) (mRawWidth * mRawHeight * bytesPerPix);
//				if (data_size <= data.length)
//				{
//					if (format == ImageFormat.YV12)
//					{
//						int[] cs = new int[1];
//						mAvcEnc.queryInt(AvcEncoder.KEY_COLORFORMAT, cs);
//						if (cs[0] == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar)
//						{
//							if (mRawData == null)
//							{
//								mRawData = new byte[data_size];
//							}
//							swapYV12toI420(data, mRawData, mRawWidth, mRawHeight);
//						}
//						else
//						{
//							//I hope the omx default color format is YV12
//							mRawData = data;
//						}
//					}
//					else
//					{
//						Log.e(log_tag, "preview size MUST be YV12, cur is "+format);
//						mRawData = data;
//					}
//					
//					synchronized(mAvcEncLock)
//					{
//						int res = mAvcEnc.InputRawBuffer(mRawData, data_size, System.currentTimeMillis());
//						if (res != 0)
//						{
//							Log.w(log_tag, "onPreviewFrame. mAvcEnc.InputRawBuffer res="+res);
//						}
//					}
//				}
//			}
		}
		
		
		
		
		if (mUsePreviewBuffer == true)
		{
			if (mEnableVideoEncode == true)
			{
				synchronized(mAvcEncLock) {
					mPreviewBuffers.add(data);
					
					Log.d(log_tag, "onpreview, return buffer to list. pb size is"+mPreviewBuffers.size());
				}
			}
			else {
				camera.addCallbackBuffer(data);
			}
		}
		
//			 FileOutputStream outStream = null;
//			try {
//				outStream = mCtx.openFileOutput("capture.yuv", Context.MODE_APPEND);
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}  
//		     try {
//		    	 if(outStream != null)
//		    	 {
//		    		 outStream.write(data);
//		    		 outStream.close();
//		    	 }
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}  
				 
	}

	public void onCheckedChanged(RadioGroup group, int checkedId) {
		// TODO Auto-generated method stub
		switch (checkedId)
		{
		case R.id.RadioButton_surfaceview:
			Log.i(log_tag, "[Radio] RadioButton_surfaceview");
			mUseSurfaceTexture = false;
			break;
		case R.id.radioButton_surfacetexture:
			Log.i(log_tag, "[Radio] radioButton_surfacetexture");
			mUseSurfaceTexture = true;
			break;
		case R.id.RadioButton_AllocPB:
			Log.i(log_tag, "[Radio] RadioButton_AllocPB");
			mUsePreviewBuffer = true;
			break;
		case R.id.radioButton_NotAllocPB:
			Log.i(log_tag, "[Radio] radioButton_NotAllocPB");
			mUsePreviewBuffer = false;
			break;
		}
		
	}

	public void onError(int arg0, Camera arg1) {
		// TODO Auto-generated method stub
		Log.e(log_tag, "onError, arg0="+arg0+",arg1="+arg1.toString());
	}

	public int onUpdateOutputBufferSize(/*in*/int size) {
		// TODO Auto-generated method stub
		mAvcBuf = new byte[size];
		return 0;
	}

	public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
		// TODO Auto-generated method stub
		switch(arg0.getId())
		{
		case R.id.checkBoxEnableVideoEncode:
			if (arg1 != mEnableVideoEncode)
			{
				if (arg1 == true)
				{
					mAvcEnc = new AvcEncoder();
					mAvcEnc.Init(this);
					
					mAvcBuf = new byte[AvcEncoder.DEFAULT_AVC_BUF_SIZE];
					
					if (!mETFps.getText().toString().equals(""))
					{
						mEncode_fps = Integer.parseInt(mETFps.getText().toString());
					}
					if (!mETBps.getText().toString().equals(""))
					{
						mEncode_bps = Integer.parseInt(mETBps.getText().toString()) * 1000;
					}
					
					m_codec_thread = new CodecThread();
					m_codec_thread.start();
					
					try {
						Thread.sleep(10);	//sleep for m_CodecMsgHandler be created
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					if (mEventHandler != null)
						mEventHandler.sendEmptyMessage(EVENT_CALC_ENCODE_FPS);
				}
				else
				{
					if (mEventHandler != null)
						mEventHandler.removeMessages(EVENT_CALC_ENCODE_FPS);
					
					totalStopAvcEncode();
					mRawWidth = 0;
					mRawHeight = 0;
					mEncCountPerSecond = 0;
					mEncBytesPerSecond = 0;
					setEncodeFPS_TextView(0, mRawWidth, mRawHeight, 0);
				}
				
				
				mEnableVideoEncode = arg1;
			}
			break;
			
		case R.id.checkBoxAvcGotoFile:
			{
				if (arg1 != mAvcGotoFile)
				{
					mAvcGotoFile = arg1;
				}
			}
			break;
		}
	}
	
	private void totalStopAvcEncode()
	{
		Log.d(log_tag, "totalStopAvcEncode ++");
		
		if (m_CodecMsgHandler != null)
    	{
			m_CodecMsgHandler.removeMessages(EVENT_REQUEST_KEY_FRAME);
			m_CodecMsgHandler.removeMessages(EVENT_GET_ENCODE_OUTPUT);
			m_CodecMsgHandler.getLooper().quit();
    	}
		m_codec_thread = null;
		
		synchronized(mAvcEncLock) {
			if (mAvcEnc != null)
			{
				mAvcEnc.stop();
				mAvcEnc.Uninit();
				mAvcEnc = null;
			}
		}
		
		if(mAvcGotoFile == true)
		{
			if(mFOS != null)
			{
				try {
					mFOS.close();
					mFOS = null;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		Log.d(log_tag, "totalStopAvcEncode --");
	}

	
	private class CodecThread extends Thread{				
		@Override
		public void run(){
			Looper.prepare();
			m_CodecMsgHandler = new Handler(){
				@Override
				public void handleMessage(Message msg){					
					//Log.i("wme_android","CpuHelper, handleMessage, what= "+msg.what);
					switch (msg.what) {
						case EVENT_GET_ENCODE_OUTPUT:
						{
							if (mAvcEnc != null)
							{
								synchronized(mAvcEncLock) {
									int res = AvcEncoder.R_BUFFER_OK;
									
									//STEP 1: handle input buffer
									Iterator<byte[]> ite = mPreviewBuffers.iterator();
									while (ite.hasNext())
									{
										byte[] data = ite.next();
										
										int data_size = getPreviewBufferSize(mRawWidth, mRawHeight, mSelectColorFormat);
										if (data_size <= data.length)
										{
											if (mSelectColorFormat == ImageFormat.YV12)
											{
												int[] cs = new int[1];
												mAvcEnc.queryInt(AvcEncoder.KEY_COLORFORMAT, cs);
												if (cs[0] == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar)
												{
													if (mRawData == null || 
															mRawData.length < data_size)
													{
														mRawData = new byte[data_size];
													}
													swapYV12toI420(data, mRawData, mRawWidth, mRawHeight);
												}
												else
												{
													//I hope the omx default color format is YV12
													mRawData = data;
												}
											}
											else
											{
												Log.e(log_tag, "preview size MUST be YV12, cur is "+mSelectColorFormat);
												mRawData = data;
											}
											

											res = mAvcEnc.InputRawBuffer(mRawData, data_size, System.currentTimeMillis());
											if (res != AvcEncoder.R_BUFFER_OK)
											{
												Log.w(log_tag, "mAvcEnc.InputRawBuffer, maybe wrong:"+res);
												break;		//the rest buffers shouldn't go into encoder, if the previous one get problem 
											}
											else
											{
												Log.d(log_tag, "EVENT_GET_ENCODE_OUTPUT, handle input buffer once. pb size is"+mPreviewBuffers.size());
												if (mCam != null)
												{
													mCam.addCallbackBuffer(data);
												}
												ite.remove();
											}
										}
									}
									
									
									//STEP 2: handle output buffer
									while(res == AvcEncoder.R_BUFFER_OK)
									{
										int[] len = new int[1];
										len[0] = mAvcBuf.length;
										res = mAvcEnc.OutputAvcBuffer(mAvcBuf, len);
										if (res == AvcEncoder.R_INVALIDATE_BUFFER_SIZE)
										{
											//mAvcBuf should be refreshed
											len[0] = mAvcBuf.length;
											res = mAvcEnc.OutputAvcBuffer(mAvcBuf, len);
										}
										
										if (res == AvcEncoder.R_BUFFER_OK)
										{
											//TODO:
											//write avc to file, or calc the fps
											if(mAvcGotoFile == true)
											{
												if(mFOS != null)
												{
													try {
														mFOS.write(mAvcBuf, 0, len[0]);
													} catch (IOException e) {
														// TODO Auto-generated catch block
														e.printStackTrace();
													}
												}
											}
											
											mEncBytesPerSecond += len[0];
											mEncCountPerSecond++;
											if (mAvcEncodeFirstOutputGap_starttick != 0)
											{
												long tick = System.currentTimeMillis();
												Log.i("AvcEncoder", "first Avc encoder output gap is "+(tick - mAvcEncodeFirstOutputGap_starttick));
												mAvcEncodeFirstOutputGap_starttick = 0;
											}
											
											//Log.i(log_tag, "get encoded data, len="+len[0]);
										}
										else if (res == AvcEncoder.R_OUTPUT_UPDATE)
										{
											res = AvcEncoder.R_BUFFER_OK;
										}
										else if (res == AvcEncoder.R_TRY_AGAIN_LATER)
										{
//											try {
//												Thread.sleep(1);
//											} catch (InterruptedException e) {
//												// TODO Auto-generated catch block
//												e.printStackTrace();
//											}
										}
										else
										{
											//not possible from Android doc
										}
									}
									
								}
							}
							
							m_CodecMsgHandler.sendEmptyMessageDelayed(EVENT_GET_ENCODE_OUTPUT, 30);
						}
						break;

						case EVENT_REQUEST_KEY_FRAME:
						{
							if (mAvcEnc != null)
							{
								if (Build.VERSION.SDK_INT < 19)
								{
									synchronized(mAvcEncLock) {
										Log.d(log_tag, "CodecThread, EVENT_REQUEST_KEY_FRAME under api level 19");
										mAvcEnc.stop();
										mAvcEnc.tryConfig(mRawWidth, mRawHeight, mEncode_fps, mEncode_bps);
										mAvcEnc.start();
									}
								}
								else {
									mAvcEnc.RequestKeyFrameSoon();
								}
							}
							
							if (mPeriodKeyFrame > 0)
								m_CodecMsgHandler.sendEmptyMessageDelayed(EVENT_REQUEST_KEY_FRAME, mPeriodKeyFrame);
						}
						break;
						
						default:
						break;
					}					
				}
			};
			Looper.loop();
		}		
	}
	
	 //yv12 to yuv420  
    private void swapYV12toI420(byte[] yv12bytes, byte[] i420bytes, int width, int height)   
    {        
        System.arraycopy(yv12bytes, 0, i420bytes, 0,width*height);  
        System.arraycopy(yv12bytes, width*height+width*height/4, i420bytes, width*height, width*height/4);  
        System.arraycopy(yv12bytes, width*height, i420bytes, width*height+width*height/4, width*height/4);    
    } 
    
}