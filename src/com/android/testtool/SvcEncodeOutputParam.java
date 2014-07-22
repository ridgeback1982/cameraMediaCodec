package com.android.testtool;

public class SvcEncodeOutputParam implements Cloneable{
	public long 		timestamp;
	public long 		sampletimestamp;
	public int 			marker;					//always true, as "last nal"
	public int			layernumber;
	public int			layerindex;
	public int			layerwidth;
	public int			layerheight;
	public int			frame_idc;
	public int			nal_ref_idc;
	public int			priority;
	public int 			frametype;
	public int 			spaicialid;
	public int			maxspacialid;
	public int			temporalid;
	public int			maxtemporalid;
	
	
	//fix me, it is not general SVC encoder logic
	public int			streamid;
	public int			modeindex;
	
	
	
	
	public SvcEncodeOutputParam clone() {  
		SvcEncodeOutputParam o = null;  
        try {  
            o = (SvcEncodeOutputParam) super.clone();  
        } catch (CloneNotSupportedException e) {  
            e.printStackTrace();  
        }  
        return o;  
    }  
	
	
	
	
}