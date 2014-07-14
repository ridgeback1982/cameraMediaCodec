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
import android.media.MediaCodec;
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
import android.widget.Toast;

public class HelloCameraActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback, Camera.ErrorCallback ,RadioGroup.OnCheckedChangeListener
			, AvcEncoderSink, CheckBox.OnCheckedChangeListener
{
	private MediaRecorder mRecorder;
	private SurfaceView mSurfacePreview;
	private SurfaceView mSurfaceDecoder;
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
	private CheckBox mCBRawInput;
	private CheckBox mCBAvcGotoFile;
	private CheckBox mCBMultiEncoder;
	private CheckBox mCBVideoDecode;
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
	final int EVENT_READ_RAW_FILE = 3;
	
	long mPreviewGap_starttick = 0;
	
	//AvcEncoder mAvcEnc = null;
	SvcEncoder mSvcEnc = null;
	AvcDecoder mAvcDec = null;
	AvcDecoderBug mAvcDecBug = null;
	boolean mEnableVideoEncode;
	boolean mEnableRawInput;
	boolean mEnableVideoDecode;
	private byte[] mAvcBuf = null;
	private final static Object mAvcEncLock = new Object();  
	private final static Object mAvcDecLock = new Object(); 
	private CodecThread m_codec_thread = null;
    private Handler m_CodecMsgHandler = null;
    private int mEncCountPerSecond = 0;
    private int mEncBytesPerSecond = 0;
    long mAvcEncodeFirstOutputGap_starttick = 0;
    boolean mAvcGotoFile;
    boolean mMultiEncoder;
    final int MAX_SPACIAL_LAYER = 4;
    int[] mSvcEncodeWidth;
    int[] mSvcEncodeHeight;
    int[] mSvcEncodeFPS;
    int[] mSvcEncodeBPS;
    int	mPeriodKeyFrame = -1;	//ms
    private FileOutputStream mFOS = null;
    byte[] mRawData = null;
    Queue<PreviewBufferInfo> mPreviewBuffers_clean = null;
    Queue<PreviewBufferInfo> mPreviewBuffers_dirty = null;
    Queue<PreviewBufferInfo> mDecodeBuffers_clean = null;
    Queue<PreviewBufferInfo> mDecodeBuffers_dirty = null;
    private final int PREVIEW_POOL_CAPACITY = 5;
    private final int DECODE_UNI_SIZE = 512*1024;		//512k for 1080p, it is enough
    long mPreviewBufferSize = 0;
    long mDelay_lastPreviewTick = 0;
    long mDelay_lastEncodeTick = 0;
    long mDelay_lastDecodeTick = 0;
    int mCurAvcDecoderWidth = 0;
    int mCurAvcDecoderHeight = 0;
    int mFrameCountIntoEncoder = 0;
    int mFrameCountOutofEncoder = 0;
    int mFrameCountIntoDecoder = 0;
    int mFrameCountOutofDecoder = 0;
    
    
    //////////////////////////////////////////////raw file input
    FileInputStream mRawIS = null;
    private final String RAW_FILE_NAME = "CiscoVT_2people_640x384_25fps_900.yuv";
    private final int RAW_PIC_WIDTH = 640;
    private final int RAW_PIC_HEIGHT = 384;
    private final float RAW_BYTES_PER_PIXEL = (float) 1.5;
    private final int RAW_FRAME_COUNT = 900;
    private int mRawReadFrameCount = 0;
    //////////////////////////////////////////////
    
    
    //private boolean mAvcEncOutputSuspend = false;
    
    private final int EVENT_GET_ENCODE_OUTPUT = 1;
    private final int EVENT_REQUEST_KEY_FRAME = 2;
    private final int EVENT_GO_DECODE = 3;
	
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
            			if (mEnableRawInput == true)
            			{
            				mRawReadFrameCount = 0;
            				File file = new File(Environment.getExternalStorageDirectory(), RAW_FILE_NAME); 
            				try {
								mRawIS = new FileInputStream(file);
							} catch (FileNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
            				
            				if (mPreviewBuffers_clean == null)
            	    			mPreviewBuffers_clean = new LinkedList<PreviewBufferInfo>();
            	    		if (mPreviewBuffers_dirty == null)
            	    			mPreviewBuffers_dirty = new LinkedList<PreviewBufferInfo>();
 
        	    			mPreviewBuffers_clean.clear();
        	    			mPreviewBuffers_dirty.clear();
        	    			
        	    			int size = (int) (RAW_PIC_WIDTH * RAW_PIC_HEIGHT * RAW_BYTES_PER_PIXEL);
        	    			
        	    			for(int i=0;i<PREVIEW_POOL_CAPACITY;i++)
        	        		{
        	        			byte[] buf = new byte[size];
        	        			PreviewBufferInfo info = new PreviewBufferInfo();
        	        			info.buffer = buf;
        	        			info.size = size;
        	        			info.timestamp = 0;
        	        			mPreviewBuffers_clean.add(info);
        	        		}
        	    			
        	    			mEventHandler.sendEmptyMessage(EVENT_READ_RAW_FILE);
        	    			
        	    			//write avc to file
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
        	                    	String fname = "mc_"+Integer.toString(RAW_PIC_WIDTH)+"x"+Integer.toString(RAW_PIC_HEIGHT)+".h264";
        		                    File filePath = new File(dir, fname);
        		                    if (filePath.exists() == true && filePath.isFile() == true)
        		                    	filePath.delete();
        							mFOS = new FileOutputStream(filePath);
        						} catch (Exception e) {
        							// TODO Auto-generated catch block
        							e.printStackTrace();
        						}
        					}
        	    			
        	    			
        	    			//start avc mediacodec
        	    			mEncCountPerSecond = 0;
        					mEncBytesPerSecond = 0;
        					if (mEventHandler != null)
        					{
        						mEventHandler.removeMessages(EVENT_CALC_ENCODE_FPS);
        						mEventHandler.sendEmptyMessage(EVENT_CALC_ENCODE_FPS);
        					}
        					
        					if (mSvcEnc != null) {
	        					synchronized(mAvcEncLock) {
	        		    			mRawHeight = RAW_PIC_HEIGHT;
	        						mRawWidth = RAW_PIC_WIDTH;
	        						
	        						mFrameCountIntoEncoder = 0;
	        					    mFrameCountOutofEncoder = 0;
	        						
	        					    mSvcEnc.Stop();
	        					    int capa = mSvcEnc.GetSpacialLayerCapacity();
	        					    SvcEncodeSpacialParam[] params = new SvcEncodeSpacialParam[capa];
	        					    if (mMultiEncoder == false)
	        					    {
		        					    params[0] = new SvcEncodeSpacialParam();
		        					    params[0].mWidth = mRawWidth;
		        					    params[0].mHeight = mRawHeight;
		        					    params[0].mFrameRate = mEncode_fps;
		        					    params[0].mBitrate = mEncode_bps;
	        					    }
	        					    else
	        					    {
	        					    	for (int i=0;i<capa;i++)
	        					    	{
	        					    		params[i] = new SvcEncodeSpacialParam();
	        					    		params[i].mWidth = mSvcEncodeWidth[i];
			        					    params[i].mHeight = mSvcEncodeHeight[i];
			        					    params[i].mFrameRate = mSvcEncodeFPS[i];
			        					    params[i].mBitrate = mSvcEncodeBPS[i];
	        					    	}
	        					    }
	        					    mSvcEnc.Configure(params);
	        						mSvcEnc.Start();
	        						
	        						//if (Build.VERSION.SDK_INT >= 19)
	        						//	mAvcEnc.SetBitrateOnFly(mEncode_bps);
	        					}
        					}
        					
        					if (m_CodecMsgHandler != null)
        					{
        						m_CodecMsgHandler.removeMessages(EVENT_GET_ENCODE_OUTPUT);
        						m_CodecMsgHandler.sendEmptyMessage(EVENT_GET_ENCODE_OUTPUT);
        					}
            	    			
            			}
            			else {
	            			try {
								TestFPS();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
            			}
            			mBtnSetparam.setEnabled(false);
            			mBtnTestFPS.setText("Stop");
            			mTestingFPS = true;
            			m_bStartPreview = true;
            			
            			mCBVideoEncode.setEnabled(false);
            			mCBAvcGotoFile.setEnabled(false);
            			mCBVideoDecode.setEnabled(false);
            			//mCBRawInput.setEnabled(false);
            		}
            		else
            		{
            			if (mEnableRawInput == true)
            			{
            				if (mRawIS != null)
            				{
            					mPreviewBuffers_clean.clear();
            	    			mPreviewBuffers_dirty.clear();
            	    			mPreviewBuffers_clean = null;
            	    			mPreviewBuffers_dirty = null;
            					
            					try {
									mRawIS.close();
									
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
            					mRawIS = null;
            				}
            			}
            			else {
	            			if (mCam != null)
	            			{
	            				try {
	            					
	            					stopPreview();
	            					
	            					if (mUsePreviewBuffer == true)
	            					{
	            						mCam.setPreviewCallbackWithBuffer(null);	//it will clear all buffers added to camera by me
	            						synchronized(mAvcEncLock) {
		            						mPreviewBuffers_clean.clear();
		            						mPreviewBuffers_clean = null;
		            						mPreviewBuffers_dirty.clear();
		            						mPreviewBuffers_dirty = null;
	            						}
	            						mPreviewBufferSize = 0;
	            						
	            					}
	            					mDelay_lastEncodeTick = 0;
	        						mDelay_lastPreviewTick = 0;
	        						mDelay_lastDecodeTick = 0;
	            					
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
            			}
            			
            			mBtnSetparam.setEnabled(true);
            			mBtnTestFPS.setText("Begin test");
            			mTestingFPS = false;
            			m_bStartPreview = false;
            			setCaptureFPS_TextView(0, 0, 0);
            			mCBVideoEncode.setEnabled(true);
            			mCBAvcGotoFile.setEnabled(true);
            			//mCBRawInput.setEnabled(true);
            			mCBVideoDecode.setEnabled(true);
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
    	
    	mSurfaceDecoder = (SurfaceView)findViewById(R.id.surfaceView_decode);
    	mSurfaceDecoder.getHolder().addCallback(this);
    	
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
    	mUsePreviewBuffer = true;
    	RadioGroup  radiogroup2=(RadioGroup)findViewById(R.id.radiogroup2);
    	RadioButton radio_AllocPB;
		RadioButton radio_NotAllocPB;
		radio_AllocPB=(RadioButton)findViewById(R.id.RadioButton_AllocPB);
		radio_AllocPB.setChecked(true);
		radio_NotAllocPB=(RadioButton)findViewById(R.id.radioButton_NotAllocPB);
		radio_NotAllocPB.setChecked(false);
    	radiogroup2.setOnCheckedChangeListener(this);  
    	
    	mETFps = (EditText)findViewById(R.id.FPSedit);
    	mETBps = (EditText)findViewById(R.id.BPSedit);
    	
    	
    	mEnableVideoEncode = false;
    	mEnableVideoDecode = false;
    	mPeriodKeyFrame = -1;		//test for request key frame, ms period
    	mAvcGotoFile = false;		//really for debug, write files
    	mEnableRawInput = false;
    	mMultiEncoder = false;
    	mRawHeight = 0;
    	mRawWidth = 0;
    	mEncode_fps = 30;
    	mEncode_bps = 600000;
    	mPreviewBuffers_clean = new LinkedList<PreviewBufferInfo>();
    	mPreviewBuffers_dirty = new LinkedList<PreviewBufferInfo>();
    	mDecodeBuffers_clean = new LinkedList<PreviewBufferInfo>();
    	mDecodeBuffers_dirty = new LinkedList<PreviewBufferInfo>();
    	mAvcDecBug = new AvcDecoderBug();
    	mSvcEncodeWidth = new int[] {160, 320, 640, 1280};
        mSvcEncodeHeight = new int[] {90, 180, 360, 720};
        mSvcEncodeFPS = new int[] {6, 12, 24, 30};
        mSvcEncodeBPS = new int[] {64000, 180000, 520000, 1700000};
    	
    	
    	mCBVideoEncode = (CheckBox)findViewById(R.id.checkBoxEnableVideoEncode);
		mCBVideoEncode.setChecked(mEnableVideoEncode);
		mCBVideoEncode.setOnCheckedChangeListener(this);
		
		mCBRawInput = (CheckBox)findViewById(R.id.checkBoxRawFromFile);
		mCBRawInput.setChecked(mEnableRawInput);
		mCBRawInput.setOnCheckedChangeListener(this);
		mCBRawInput.setEnabled(false);
		
		mCBAvcGotoFile = (CheckBox)findViewById(R.id.checkBoxAvcGotoFile);
		mCBAvcGotoFile.setChecked(mAvcGotoFile);
		mCBAvcGotoFile.setOnCheckedChangeListener(this);
		
		mCBMultiEncoder = (CheckBox)findViewById(R.id.checkBoxMultiEnc);
		mCBMultiEncoder.setChecked(mMultiEncoder);
		mCBMultiEncoder.setOnCheckedChangeListener(this);
		
		mCBVideoDecode = (CheckBox)findViewById(R.id.checkBoxVideoDecode);
		mCBVideoDecode.setChecked(mEnableVideoDecode);
		mCBVideoDecode.setOnCheckedChangeListener(this);
		
	 	
	 	mEventHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case EVENT_SCREEN_ROTATE_CAMERA_REFRESH: 
					Log.i(log_tag,"handleMessage:SCREEN_ROTATE_CAMERA_REFRESH");

					
					removeMessages(EVENT_SCREEN_ROTATE_CAMERA_REFRESH);
					break;
					
				case EVENT_CALC_ENCODE_FPS:
					int dec_delay = 0;
					if (mDelay_lastDecodeTick != 0)
					{
						dec_delay = (int) (mDelay_lastPreviewTick - mDelay_lastDecodeTick);
					}
					int enc_delay = 0;
					if (mDelay_lastEncodeTick != 0)
					{
						enc_delay = (int)(mDelay_lastPreviewTick - mDelay_lastEncodeTick);
					}
						
					setEncodeFPS_TextView(mEncCountPerSecond, mRawWidth, mRawHeight, mEncBytesPerSecond*8, enc_delay, dec_delay);
					mEncCountPerSecond = 0;
					mEncBytesPerSecond = 0;
					sendEmptyMessageDelayed(EVENT_CALC_ENCODE_FPS, 1000);
					break;
					
				case EVENT_READ_RAW_FILE:
					if (mRawIS != null)
					{
						if (mPreviewBuffers_clean == null || mPreviewBuffers_dirty == null)
						{
							break;
						}
						synchronized(mAvcEncLock) {
							if (!mPreviewBuffers_clean.isEmpty())
							{
								PreviewBufferInfo info = mPreviewBuffers_clean.poll();	//remove the head of queue
								info.timestamp = System.currentTimeMillis();
								try {
									int res = mRawIS.read(info.buffer, 0, info.size);
									if (res == -1)
									{
										Toast.makeText(mThis, "EOS", Toast.LENGTH_LONG).show();
										removeMessages(EVENT_READ_RAW_FILE);
										break;
									}
									mRawReadFrameCount ++;
									
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								mPreviewBuffers_dirty.add(info);
							}
						}
						
						sendEmptyMessageDelayed(EVENT_READ_RAW_FILE, 30);
					}
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
			stopPreview();
			
			if (mUsePreviewBuffer == true)
			{
				mCam.setPreviewCallbackWithBuffer(null);	//it will clear all buffers added to camera by me
				synchronized(mAvcEncLock) {
					mPreviewBuffers_clean.clear();
					mPreviewBuffers_clean = null;
					mPreviewBuffers_dirty.clear();
					mPreviewBuffers_dirty = null;
				}
				mPreviewBufferSize = 0;
				
			}
			mDelay_lastEncodeTick = 0;
			mDelay_lastPreviewTick = 0;
			mDelay_lastDecodeTick = 0;
			
			Log.i(log_tag, "onPause, before release");
			mCam.release();
			Log.i(log_tag, "onPause, after release");
			mCam = null;
		}
		mBtnTestFPS.setText("Begin test");
		mTestingFPS = false;
		setCaptureFPS_TextView(0, 0, 0);
		
		if (m_CodecMsgHandler != null)
    	{
			m_CodecMsgHandler.removeMessages(EVENT_REQUEST_KEY_FRAME);
			m_CodecMsgHandler.removeMessages(EVENT_GET_ENCODE_OUTPUT);
			m_CodecMsgHandler.removeMessages(EVENT_GO_DECODE);
			if (Build.VERSION.SDK_INT < 18)
			{
				//[Doc]Using this method may be unsafe because some messages may not be delivered before the looper terminates. 
				m_CodecMsgHandler.getLooper().quit();
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else {
				m_CodecMsgHandler.getLooper().quitSafely();
			}
    	}
		m_codec_thread = null;
		
		if (mEnableVideoEncode == true)
		{
			setEncodeFPS_TextView(0, 0, 0, 0, 0, 0);
			if (mEventHandler != null)
			{
				mEventHandler.removeMessages(EVENT_CALC_ENCODE_FPS);
			}
			totalStopAvcEncode();
		}
		
		if (mEnableVideoDecode == true)
		{
			mCurAvcDecoderWidth = 0;
			mCurAvcDecoderHeight = 0;
			mFrameCountIntoDecoder = 0;
			mFrameCountOutofDecoder = 0;
			if (mAvcDec != null)
			{
				mAvcDec.stop();
				mAvcDec.Uninit();
				mAvcDec = null;
			}
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
//	    	else
//	    	{
//			    mCam.stopPreview();
//			    setCaptureFPS_TextView(0, 0, 0);
//			    if (sdkInt >= 9)
//			    {
//			    	setCameraDisplayOrientation(this, camIndex, mCam);
//			    }
//			    if (mUsePreviewBuffer == true)
//    				mCam.setPreviewCallbackWithBuffer(this);
//    			else
//    				mCam.setPreviewCallback(this);
//		    	mCam.startPreview(); 
//	    	}
	    }
	}
	
//	@Override
//	public boolean onKeyDown(int keyCode, KeyEvent event) {
//		switch (keyCode) {
//		case KeyEvent.KEYCODE_BACK:
//			if (mCam != null)
//			{
//				stopPreview();
//				
//				mCam.release();
//				mCam = null;
//			}
//			break;
//		}
//		return super.onKeyDown(keyCode, event);
//	}
	
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
    		synchronized(mAvcEncLock) {
	    		if (mPreviewBuffers_clean == null)
	    			mPreviewBuffers_clean = new LinkedList<PreviewBufferInfo>();
	    		if (mPreviewBuffers_dirty == null)
	    			mPreviewBuffers_dirty = new LinkedList<PreviewBufferInfo>();
	    		
	    		int size = getPreviewBufferSize(mSelectWidth, mSelectHeight, mSelectColorFormat);
	
	    		if (size > mPreviewBufferSize)
	    		{
	    			mPreviewBufferSize = size;
	    			//call of "setPreviewCallbackWithBuffer(null)" will clear the buffers we passed to camera(it is the only way to clear buffers, until Android4.4),
	    			//but "stopPreview" will not clear buffers
	    			mCam.setPreviewCallbackWithBuffer(null); 	//it OK to call even for the first time
	    			//discard the buffers we just used before
	    			mPreviewBuffers_clean.clear();
	    			mPreviewBuffers_dirty.clear();
	    			
	    			for(int i=0;i<PREVIEW_POOL_CAPACITY;i++)
	        		{
	        			byte[] mem = new byte[size];
	        			mCam.addCallbackBuffer(mem);	//ByteBuffer.array is a reference, not a copy
	        			
	        			PreviewBufferInfo info = new PreviewBufferInfo();
	        			info.buffer = null;
	        			info.size = 0;
	        			info.timestamp = 0;
	        			mPreviewBuffers_clean.add(info);
	        		}
	    			
	    		}
	    		else
	    		{
	    			//TODO: return the rest of dirty buffer to camera
	    			Log.i(log_tag, "startPreview, should I return the rest of dirty to camera?");
	    			Log.i(log_tag, "dirty size:"+mPreviewBuffers_dirty.size()+", clean size:"+mPreviewBuffers_clean.size());
	    			Iterator<PreviewBufferInfo> ite = mPreviewBuffers_dirty.iterator();
	    			while(ite.hasNext())
	    			{
	    				PreviewBufferInfo info = ite.next();
	    				mPreviewBuffers_clean.add(info);
	    				ite.remove();
	    				mCam.addCallbackBuffer(info.buffer);
	    			}
	    		}
    		}	//synchronized(mAvcEncLock)
    		mCam.setPreviewCallbackWithBuffer(this);	//call it every time, to make sure the camera is in right status: CAMERA_FRAME_CALLBACK_FLAG_CAMERA

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
		
		if (mUsePreviewBuffer == false)
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
    
    private void setEncodeFPS_TextView(int fps, int w, int h, int bps, int enc_delay, int dec_delay)
    {
    	TextView tv = (TextView)findViewById(R.id.textview_encodefps);
    	tv.setText("Encode FPS:"+String.valueOf(fps)+", "+String.valueOf(w)+"x"+String.valueOf(h)+", "+bps+"bps"+",delay:"+enc_delay+",("+dec_delay+")ms");
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
    		
    		////////////////////CAUTION: for YV12, the preview buffer size is not always w*h*1.5
    		/*
    		 * If the width of preview picture is 32 alignment, the preview buffer size is exactly w*h*1.5
    		 * If the width is not 32 alignment, the preview buffer size is bigger than w*h*1.5
    		 * In the second case, the preview buffer size is probably bigger than the MediaCodec input buffer, which cannot be handled by programmers
    		 * Acturally, the MediaCodec input buffer is not exactly w*h*1.5, it may be a little bit bigger than that. But may be still smaller than preview buffer
    		 * A easy way to avoid the mismatch of preview buffer and input buffer, to avoid a data copy, is that we only select the 32 alignment width.
    		 * 
    		 * */
    		Iterator ite = ListPreviewSizes.iterator();
    		while(ite.hasNext())
    		{
    			Size siz = (Size) ite.next();
    			if (Math.ceil(siz.width/32.0) * 32 > siz.width)
    			{
    				ite.remove();
    			}
    		}
    		
    		
    		
    		
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
    		for (Size siz1:ListPreviewSizes)
    		{
    			//ziyzhang: cannot use the inner class Size, so use a 2-element array instead
    			int[] siz_wh = new int[2];
    			siz_wh[0] = siz1.width;
    			siz_wh[1] = siz1.height;
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
	    			mRawHeight = siz.height;
					mRawWidth = siz.width;
					mFrameCountIntoEncoder = 0;
				    mFrameCountOutofEncoder = 0;
					
				    if (mSvcEnc != null)
				    {
				    	mSvcEnc.Stop();
					    int capa = mSvcEnc.GetSpacialLayerCapacity();
					    SvcEncodeSpacialParam[] params = new SvcEncodeSpacialParam[capa];
					    if (mMultiEncoder == false)
					    {
    					    params[0] = new SvcEncodeSpacialParam();
    					    params[0].mWidth = mRawWidth;
    					    params[0].mHeight = mRawHeight;
    					    params[0].mFrameRate = mEncode_fps;
    					    params[0].mBitrate = mEncode_bps;
					    }
					    else
					    {
					    	for(int i=0;i<capa;i++)
					    	{
					    		params[i] = new SvcEncodeSpacialParam();
					    		params[i].mWidth = mSvcEncodeWidth[i];
        					    params[i].mHeight = mSvcEncodeHeight[i];
        					    params[i].mFrameRate = mSvcEncodeFPS[i];
        					    params[i].mBitrate = mSvcEncodeBPS[i];
					    	}
					    }
					    mSvcEnc.Configure(params);
						mSvcEnc.Start();
					
						//if (Build.VERSION.SDK_INT >= 19)
						//	mAvcEnc.SetBitrateOnFly(mEncode_bps);
				    }
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
				
				mAvcEncodeFirstOutputGap_starttick = System.currentTimeMillis();
				
			}
		}
		

		if (mUsePreviewBuffer == true)
		{
			if (mEnableVideoEncode == true)
			{
				//check size
				int expected_size = getPreviewBufferSize(mRawWidth, mRawHeight, mSelectColorFormat);
				if (data.length >= expected_size)
				{
					synchronized(mAvcEncLock) {
						if (mPreviewBuffers_clean.isEmpty() == false)	//impossible to be empty, :-)
						{
							PreviewBufferInfo info = mPreviewBuffers_clean.poll();	//remove the head of queue
							info.buffer = data;
							info.size = getPreviewBufferSize(mRawWidth, mRawHeight, mSelectColorFormat);
							info.timestamp = System.currentTimeMillis();
							mPreviewBuffers_dirty.add(info);
						}
						//Log.d(log_tag, "onpreview, return buffer to list. dirty size is"+mPreviewBuffers_dirty.size());
					}
				}
				else
				{
					camera.addCallbackBuffer(data);
				}

				//debug for delay
				mDelay_lastPreviewTick = System.currentTimeMillis();
				
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
					mSvcEnc = new SvcEncoder();
					mSvcEnc.Init();
					
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
					
					if (m_CodecMsgHandler != null)
			    	{
						m_CodecMsgHandler.removeMessages(EVENT_REQUEST_KEY_FRAME);
						m_CodecMsgHandler.removeMessages(EVENT_GET_ENCODE_OUTPUT);
						m_CodecMsgHandler.removeMessages(EVENT_GO_DECODE);
						if (Build.VERSION.SDK_INT < 18)
						{
							//[Doc]Using this method may be unsafe because some messages may not be delivered before the looper terminates.
							m_CodecMsgHandler.getLooper().quit();
							try {
								Thread.sleep(50);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						else {
							m_CodecMsgHandler.getLooper().quitSafely();
						}
			    	}
					m_codec_thread = null;
					
					totalStopAvcEncode();
					mRawWidth = 0;
					mRawHeight = 0;
					mEncCountPerSecond = 0;
					mEncBytesPerSecond = 0;
					mDelay_lastEncodeTick = 0;
					mDelay_lastPreviewTick = 0;
					mDelay_lastDecodeTick = 0;
					setEncodeFPS_TextView(0, mRawWidth, mRawHeight, 0, 0, 0);
				}
				
				
				mEnableVideoEncode = arg1;
			}
			break;
			
		case R.id.checkBoxRawFromFile:
			{
				if (arg1 != mEnableRawInput)
				{
					
					mEnableRawInput = arg1;
				}
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
			
		case R.id.checkBoxMultiEnc:
			{
				if (arg1 != mMultiEncoder)
				{
					mMultiEncoder = arg1;
				}
			}
			break;
			
		case R.id.checkBoxVideoDecode:
			if (arg1 != mEnableVideoDecode)
			{
				if (arg1 == true)
				{
					if (mDecodeBuffers_clean == null)
		    			mDecodeBuffers_clean = new LinkedList<PreviewBufferInfo>();
		    		if (mDecodeBuffers_dirty == null)
		    			mDecodeBuffers_dirty = new LinkedList<PreviewBufferInfo>();
		    		
		    		for(int i=0;i<PREVIEW_POOL_CAPACITY;i++)
	        		{
	        			PreviewBufferInfo info = new PreviewBufferInfo();
	        			info.buffer = new byte[DECODE_UNI_SIZE];
	        			info.size = 0;
	        			info.timestamp = 0;
	        			mDecodeBuffers_clean.add(info);
	        		}
		    		
		    		mAvcDec = new AvcDecoder();
		    		mAvcDec.Init();
		    		
		    		if (m_CodecMsgHandler != null)
		    		{
						if (m_CodecMsgHandler.hasMessages(EVENT_GO_DECODE) == false)
						{
							m_CodecMsgHandler.sendEmptyMessage(EVENT_GO_DECODE);
						}
		    		}
				}
				else
				{
					if (m_CodecMsgHandler != null)
			    	{
						m_CodecMsgHandler.removeMessages(EVENT_GO_DECODE);
			    	}
					
					mCurAvcDecoderWidth = 0;
					mCurAvcDecoderHeight = 0;
					mDelay_lastDecodeTick = 0;
					mDecodeBuffers_clean = null;
					mDecodeBuffers_dirty = null;
					mFrameCountIntoDecoder = 0;
					mFrameCountOutofDecoder = 0;
					if (mAvcDec != null)
					{
						mAvcDec.stop();
						mAvcDec.Uninit();
						mAvcDec = null;
					}
				}
				
				mEnableVideoDecode = arg1;
			}
			break;
		}
	}
	
	private void totalStopAvcEncode()
	{
		Log.d(log_tag, "totalStopAvcEncode ++");
		
		mFrameCountIntoEncoder = 0;
	    mFrameCountOutofEncoder = 0;
		
		synchronized(mAvcEncLock) {
			if (mSvcEnc != null)
			{
				mSvcEnc.Stop();
				mSvcEnc.Uninit();
				mSvcEnc = null;
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
							//STEP 1: handle input buffer
							int res = AvcEncoder.R_BUFFER_OK;
							synchronized(mAvcEncLock) {
								if (mSvcEnc != null)
								{
									if (mPreviewBuffers_dirty != null && mPreviewBuffers_clean != null)
									{
										Iterator<PreviewBufferInfo> ite = mPreviewBuffers_dirty.iterator();
										while (ite.hasNext())
										{
											PreviewBufferInfo info = ite.next();
											byte[] data = info.buffer;
											int data_size = info.size;
											if (mSelectColorFormat == ImageFormat.YV12)
											{
												int[] cs = new int[1];
												mSvcEnc.queryInt(AvcEncoder.KEY_COLORFORMAT, cs);
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
												if (mRawData == null)	//only log once
													Log.e(log_tag, "preview size MUST be YV12, cur is "+mSelectColorFormat);
												mRawData = data;
											}
											
											
											int flag = 0;
											if (mEnableRawInput)
											{
												if (mFrameCountIntoEncoder == RAW_FRAME_COUNT - 1)
												{
													flag = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
												}
											}
											res = mSvcEnc.InputRawBuffer(mRawData, data_size, info.timestamp, flag);
											if (res != AvcEncoder.R_BUFFER_OK)
											{
												Log.w(log_tag, "mAvcEnc.InputRawBuffer, maybe wrong:"+res);
												break;		//the rest buffers shouldn't go into encoder, if the previous one get problem 
											}
											else
											{
												mFrameCountIntoEncoder ++;
												//Log.d(log_tag, "mFrameCountIntoEncoder = "+mFrameCountIntoEncoder);
												
												ite.remove();
												mPreviewBuffers_clean.add(info);
												if (mCam != null)
												{
													mCam.addCallbackBuffer(data);
												}
												//Log.d(log_tag, "EVENT_GET_ENCODE_OUTPUT, handle input buffer once. clean size is"+mPreviewBuffers_clean.size());
											}
										}	//while
									}	//if (mPreviewBuffers_dirty != null && mPreviewBuffers_clean != null)
									
								}	//if (mSvcEnc != null)
							}	//synchronized(mAvcEncLock)
								
							
									
							//STEP 2: handle output buffer
							res = AvcEncoder.R_BUFFER_OK;
							while(res == AvcEncoder.R_BUFFER_OK)
							{
								int[] len = new int[1];
								len[0] = mAvcBuf.length;
								SvcEncodeOutputParam svc_output = new SvcEncodeOutputParam();
								//long[] ts = new long[1];
								//int[] flags = new int[1];
								synchronized(mAvcEncLock) {
									
									if (mSvcEnc != null)
									{
										res = mSvcEnc.OutputAvcBuffer(mAvcBuf, len, svc_output);
//										if (res == AvcEncoder.R_INVALIDATE_BUFFER_SIZE)
//										{
//											//mAvcBuf should be refreshed
//											len[0] = mAvcBuf.length;
//											res = mAvcEnc.OutputAvcBuffer(mAvcBuf, len, ts, flags);
//										}
									}
									else
									{
										res = AvcEncoder.R_TRY_AGAIN_LATER;
									}
								}
								
								if (res == AvcEncoder.R_BUFFER_OK)
								{
									mFrameCountOutofEncoder ++;
									//Log.d(log_tag, "mFrameCountOutofEncoder = "+mFrameCountOutofEncoder);
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
									
									//debug for delay
									mDelay_lastEncodeTick = svc_output.timestamp;
									
									if (mEnableVideoDecode == true &&
											(mDecodeBuffers_clean != null && mDecodeBuffers_dirty != null)
										)
									{
										synchronized(mAvcDecLock) {
											Iterator<PreviewBufferInfo> ite = mDecodeBuffers_clean.iterator();
											if (ite.hasNext())
											{
												PreviewBufferInfo info = ite.next();
												if (info.buffer.length >= len[0])
												{
													info.timestamp = svc_output.timestamp;
													info.size = len[0];
													System.arraycopy(mAvcBuf, 0, info.buffer, 0, len[0]);
													ite.remove();
													mDecodeBuffers_dirty.add(info);
												}
												else {
													Log.e(log_tag, "decoder uni buffer too small, need "+len[0]+" but has "+info.buffer.length);
												}
											}
										}
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

							m_CodecMsgHandler.sendEmptyMessageDelayed(EVENT_GET_ENCODE_OUTPUT, 10);
						}
						break;

						case EVENT_REQUEST_KEY_FRAME:
						{
							if (mSvcEnc != null)
							{
								if (Build.VERSION.SDK_INT < 19)
								{
									synchronized(mAvcEncLock) {
										Log.d(log_tag, "CodecThread, EVENT_REQUEST_KEY_FRAME under api level 19");
										mSvcEnc.Stop();
		        					    int capa = mSvcEnc.GetSpacialLayerCapacity();
		        					    SvcEncodeSpacialParam[] params = new SvcEncodeSpacialParam[capa];
		        					    if (mMultiEncoder == false)
		        					    {
			        					    params[0] = new SvcEncodeSpacialParam();
			        					    params[0].mWidth = mRawWidth;
			        					    params[0].mHeight = mRawHeight;
			        					    params[0].mFrameRate = mEncode_fps;
			        					    params[0].mBitrate = mEncode_bps;
		        					    }
		        					    else
		        					    {
		        					    	for(int i=0;i<capa;i++)
		        					    	{
		        					    		params[i] = new SvcEncodeSpacialParam();
		        					    		params[i].mWidth = mSvcEncodeWidth[i];
				        					    params[i].mHeight = mSvcEncodeHeight[i];
				        					    params[i].mFrameRate = mSvcEncodeFPS[i];
				        					    params[i].mBitrate = mSvcEncodeBPS[i];
		        					    	}
		        					    }
		        					    mSvcEnc.Configure(params);
		        						mSvcEnc.Start();
									}
								}
								else {
									mSvcEnc.RequestKeyFrameSoon();
								}
							}
							
							if (mPeriodKeyFrame > 0)
								m_CodecMsgHandler.sendEmptyMessageDelayed(EVENT_REQUEST_KEY_FRAME, mPeriodKeyFrame);
						}
						break;
						
						case EVENT_GO_DECODE:
						{
							if (mAvcDec != null)
							{
								synchronized(mAvcDecLock) {
									int res = AvcDecoder.R_BUFFER_OK;
									
									//STEP 1: handle input buffer
									if (mDecodeBuffers_dirty != null && mDecodeBuffers_clean != null)
									{
										Iterator<PreviewBufferInfo> ite = mDecodeBuffers_dirty.iterator();
										while (ite.hasNext())
										{
											int flag = 0;
											PreviewBufferInfo info = ite.next();

											//ASSUME: 
											//A. there is no sub sps, appending to sps
											//B. sps/pps is appended to frame when size changes
											//TODO: 
											//1. check the AVC data, to find SPS/PPS. Is it low performance?
											byte[] sps_nal = null;
											int sps_len = 0;
											byte[] pps_nal = null;
											int pps_len = 0;
											int nal_type = AvcUtils.NAL_TYPE_UNSPECIFY;
											ByteBuffer byteb = ByteBuffer.wrap(info.buffer, 0, info.size);
											//SPS
											if (true == AvcUtils.goToPrefix(byteb))
											{
												int sps_position = 0;
												int pps_position = 0;
												nal_type = AvcUtils.getNalType(byteb);
												if (AvcUtils.NAL_TYPE_SPS == nal_type)
												{
													Log.d(log_tag, "Parsing codec frame, AVC NAL type: SPS");
													sps_position = byteb.position() - AvcUtils.START_PREFIX_LENGTH - AvcUtils.NAL_UNIT_HEADER_LENGTH;
													//PPS
													if (true == AvcUtils.goToPrefix(byteb))
													{
														nal_type = AvcUtils.getNalType(byteb);
														if (AvcUtils.NAL_TYPE_PPS == nal_type)
														{
															pps_position = byteb.position() - AvcUtils.START_PREFIX_LENGTH - AvcUtils.NAL_UNIT_HEADER_LENGTH;
															sps_len = pps_position - sps_position;
															sps_nal = new byte[sps_len];
															int cur_pos = byteb.position();
															byteb.position(sps_position);
															byteb.get(sps_nal, 0, sps_len);
															byteb.position(cur_pos);
															//slice
															if (true == AvcUtils.goToPrefix(byteb))
															{
																nal_type = AvcUtils.getNalType(byteb);
																int pps_end_position = byteb.position() - AvcUtils.START_PREFIX_LENGTH - AvcUtils.NAL_UNIT_HEADER_LENGTH;
																pps_len = pps_end_position - pps_position;
															}
															else {
																pps_len = byteb.position() - pps_position;
																//pps_len = byteb.limit() - pps_position;
															}
															if (pps_len > 0)
															{
																pps_nal = new byte[pps_len];
																cur_pos = byteb.position();
																byteb.position(pps_position);
																byteb.get(pps_nal, 0, pps_len);
																byteb.position(cur_pos);
															}
														}
														else{
															//Log.d(log_tag, "Parsing codec frame, AVC NAL type: "+nal_type);
															throw new UnsupportedOperationException("SPS is not followed by PPS, nal type :"+nal_type);
														}
													}
												}
												else{
													//Log.d(log_tag, "Parsing codec frame, AVC NAL type: "+nal_type);
												}
												
												
												//it is the decoder bug's show time
												if (mAvcDecBug != null)
												{
													boolean drop =  mAvcDecBug.ShouldDropAvcFrame(mFrameCountIntoDecoder, nal_type);
													if (drop == true)
													{
														mFrameCountIntoDecoder ++;
														ite.remove();
														mDecodeBuffers_clean.add(info);
														Log.w(log_tag, "Avc decoder bug decide to drop the frame, nal type="+nal_type);
														continue;
													}
												}
												
												
												//2. configure AVC decoder with SPS/PPS
												if (sps_nal != null && pps_nal != null)
												{
													int[] width = new int[1];
													int[] height = new int[1];
													AvcUtils.parseSPS(sps_nal, width, height);
													Log.d(log_tag, "Parsing codec frame, get SPS/PPS NAL, mCurAvcDecoderHeight="+mCurAvcDecoderHeight+",height="+height[0]+",mCurAvcDecoderWidth="+mCurAvcDecoderWidth+",width="+width[0]);
													//if (mCurAvcDecoderWidth < width[0] || mCurAvcDecoderHeight < height[0])
													if (mCurAvcDecoderWidth != width[0] || mCurAvcDecoderHeight != height[0])
													{
														mCurAvcDecoderWidth = width[0];
														mCurAvcDecoderHeight = height[0];
//														mFrameCountIntoDecoder = 0;
//														mFrameCountOutofDecoder = 0;
													
														mAvcDec.stop();													
														mAvcDec.tryConfig(mSurfaceDecoder.getHolder().getSurface(), sps_nal, pps_nal);
														mAvcDec.start();
														
													}
//													else
//													{
//														mAvcDec.flush();
//													}
													flag = MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
												}
											}
											
											res = mAvcDec.InputAvcBuffer(info.buffer, info.size, info.timestamp, flag);
											if (res != AvcDecoder.R_BUFFER_OK)
											{
												//Log.w(log_tag, "mAvcDec.InputAvcBuffer, maybe wrong:"+res);
												break;		//the rest buffers shouldn't go into decoder, if the previous one get problem 
											}
											else
											{
												mFrameCountIntoDecoder ++;
												ite.remove();
												mDecodeBuffers_clean.add(info);
												//Log.d(log_tag, "EVENT_GO_DECODE, handle input buffer once. clean size is"+mDecodeBuffers_clean.size());
											}
										}	//while
									}	//if (mDecodeBuffers_dirty != null && mDecodeBuffers_clean != null)
									
									
									//STEP 2: handle output buffer
									int[] len = new int[1];
									long[] ts = new long[1];
									res = AvcDecoder.R_BUFFER_OK;
									while(res == AvcDecoder.R_BUFFER_OK)
									{
										res = mAvcDec.OutputRawBuffer(null, len, ts);
										
										if (res == AvcDecoder.R_BUFFER_OK)
										{
											mFrameCountOutofDecoder ++;
											mDelay_lastDecodeTick = ts[0];
											//do nothing, because it is for surface rendering
											
											//Log.i(log_tag, "get decoded data, len="+len[0]);
										}
										else if (res == AvcDecoder.R_OUTPUT_UPDATE)
										{
											res = AvcDecoder.R_BUFFER_OK;
										}
										else if (res == AvcDecoder.R_TRY_AGAIN_LATER)
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
							
							m_CodecMsgHandler.sendEmptyMessageDelayed(EVENT_GO_DECODE, 10);
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
    
    private class PreviewBufferInfo
    {
    	public byte[] buffer;
    	public int size;
    	public long timestamp;
    }
    
}