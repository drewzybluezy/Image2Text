package edu.siu.cs.cs435.image2text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Vibrator;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

//need to add flash toggle
//need to add mute boolean as well
//add settings and perhaps menubuttons/bar

public class CameraActivity extends Activity implements SurfaceHolder.Callback, OnInitListener {

	private Button camera_button;
	private Button flash;
	private SurfaceView camera_view;
	
	private SurfaceHolder holder;
	
	private boolean previewRunning = false;
	
	private boolean speech = true;
	private boolean autoDelete = false;
	
	private Camera camera;
	private Camera.PictureCallback pictureCallback;
	
	private Vibrator vibrator;
	
	private CameraActivity cameraActivity;
	
	private Drawable unpressed_camera;
	private Drawable pressed_camera;
	
	private Drawable flashon;
	private Drawable noflash;
	
	private boolean flashEnabled = true;
	
	String directory, fileName;
	
	File outputFile;
	
	private AutoFocusCallback myAutoFocusCallback;
	
	private TextToSpeech myTTS;
	private int MY_DATA_CHECK_CODE = 0;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.camera_screen);
        
        cameraActivity = this;
        Resources res = getResources();
        vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        
        unpressed_camera = res.getDrawable(R.drawable.camerabutton);
        pressed_camera = res.getDrawable(R.drawable.camerabuttonpressed);
        
        flashon = res.getDrawable(R.drawable.flashon);
        noflash = res.getDrawable(R.drawable.noflash);
        
        unpressed_camera.setAlpha(130);
        pressed_camera.setAlpha(130);
        
        camera_button = (Button) findViewById(R.id.camera_button);
        camera_view = (SurfaceView) findViewById(R.id.camera_view);
        
        camera_button.setBackgroundDrawable(unpressed_camera);
        flash = (Button) findViewById(R.id.flash);
        
        //make file
        directory = Environment.getExternalStorageDirectory().toString() + "/Image2Text/";
        
        File dir = new File(directory);
        dir.mkdirs();
        
        
        holder = camera_view.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        //speech intent        
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, MY_DATA_CHECK_CODE);
        
        pictureCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera c) {
            
            	
             FileOutputStream outStream = null;

             fileName = "Image2Text_" +
	                  System.currentTimeMillis()+ ".bmp";
             
             outputFile = new File(directory, fileName);
             
              try {
	              outStream = new FileOutputStream(outputFile);
	              outStream.write(data);
	              outStream.close();
             } catch (FileNotFoundException e) {}
              catch (IOException e) {} finally {}
              Toast.makeText(cameraActivity, "Picture saved to " + directory + " as " +
            		  	fileName, Toast.LENGTH_LONG).show();
              
              Intent translateText = new Intent(CameraActivity.this, TranslationActivity.class);
			  translateText.putExtra("path", outputFile.getPath());
			  translateText.putExtra("speech", speech);
			  translateText.putExtra("autoDelete", autoDelete);
              startActivity(translateText);
            }
        };
        
        camera_view.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				speakWords("focusing");
				camera.autoFocus(myAutoFocusCallback);
			}
        	
        });
        
        flash.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				vibrator.vibrate(100);
				Camera.Parameters p = camera.getParameters();
				if (flashEnabled) {
					flash.setBackgroundDrawable(noflash);
					Toast.makeText(cameraActivity, "Flash disabled", Toast.LENGTH_SHORT).show();
					p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
				} 
				else {
					flash.setBackgroundDrawable(flashon);
					Toast.makeText(cameraActivity, "Flash enabled", Toast.LENGTH_SHORT).show();
					p.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
				}
				
				camera.setParameters(p);
				flashEnabled = !flashEnabled;
			}
        	
        });
        
        camera_button.setOnClickListener(new OnClickListener() {
          	 public void onClick(View v) {
          		 vibrator.vibrate(100);
          		 camera_button.setBackgroundDrawable(pressed_camera);
          		//speakWords("Steady");
          		 
          		 new CountDownTimer(3000, 100) {
                    public void onTick(long millis) {}
    				public void onFinish() {
    					camera_button.setBackgroundDrawable(unpressed_camera);
    					if(camera != null) {
    	                	 //camera.autoFocus(myAutoFocusCallback);
    	                	 camera.takePicture(null, null, null, pictureCallback);
    	                 }
                    }
                 }.start();   
           }});
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    public void surfaceCreated(SurfaceHolder holder) {
		camera = Camera.open();
		
	}
    
	public void surfaceChanged(SurfaceHolder holder,
	          int format, int w, int h) {
		
		if (previewRunning) {	
            camera.stopPreview();
        }
		
		Camera.Parameters p = camera.getParameters();
	    List<Camera.Size> previewSizes = p.getSupportedPreviewSizes();
	    
	    Camera.Size previewSize;
	    
	    if (previewSizes.get(0).height < previewSizes.get(previewSizes.size() - 1).height)
	    	previewSize = previewSizes.get(previewSizes.size() - 1);
	    else
	    	previewSize = previewSizes.get(0);
	    
        Toast.makeText(cameraActivity, "W: " + previewSize.width + " H: " +
    		  	previewSize.height, Toast.LENGTH_LONG).show();
        
        p.setPreviewSize(previewSize.width, previewSize.height);
        p.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
        camera.setParameters(p);
        try {
        	 
         camera.setPreviewDisplay(holder);
        } catch (IOException e) {}
        
        camera.startPreview();
        camera.autoFocus(myAutoFocusCallback);
        
        previewRunning = true;
		
	}

	


	//speech initializer
    protected void speakWords(String speech) {
        //speak straight away
        if (this.speech) myTTS.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
    }

    public void onInit(int initStatus) {
	        //check for successful instantiation
	    if (initStatus == TextToSpeech.SUCCESS) {
	        if(myTTS.isLanguageAvailable(Locale.US)==TextToSpeech.LANG_AVAILABLE)
	            myTTS.setLanguage(Locale.US);
	    }
	    else if (initStatus == TextToSpeech.ERROR) {
	        Toast.makeText(this, "Sorry! Text To Speech failed...", Toast.LENGTH_LONG).show();
	    }
    }

    //speech intent    
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (requestCode == MY_DATA_CHECK_CODE) {
	        if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
	            myTTS = new TextToSpeech(this, this);
	        }
	        else {
	            Intent installTTSIntent = new Intent();
	            installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
	            startActivity(installTTSIntent);
	        }
	    }
	}    

	//release camera
	public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.cancelAutoFocus();
        previewRunning = false;
        camera.release();
  
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
    	super.onKeyDown(keyCode, event);
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		System.exit(0);
    		return true;
    	}
    	return false;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) { 
           case R.id.toggle_readback: {
              if (speech == true) {
            	  Toast.makeText(this, "Readback Disabled", Toast.LENGTH_LONG).show();
            	  speech = false;
            	  break;
              } else {
            	  Toast.makeText(this, "Readback Enabled", Toast.LENGTH_LONG).show();
            	  speech = true;
            	  break;
              } 
           }
        
           case R.id.toggle_auto_delete:{
        	   if (autoDelete == true) {
        		   Toast.makeText(this, "Picture Auto-Delete Disabled", Toast.LENGTH_LONG).show(); 
        		   autoDelete = false;
        		   break;
        	   } else {
        		   Toast.makeText(this, "Picture Auto-Delete Enabled", Toast.LENGTH_LONG).show();
        		   autoDelete = true;
        		   break;
        	   }
           }
        }
        	return true;
    }
}
