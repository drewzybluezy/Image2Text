package edu.siu.cs.cs435.image2text;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.StringTokenizer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

/* Need to stop voice and thread on exit
 * Fix saving text name
 */

public class TranslationActivity extends Activity implements Runnable, OnInitListener{//OnInitListener{

	Button picture;
	Button save, share;
	String path;
	
	Drawable pressed_save, unpressed_save;
	Drawable pressed_share, unpressed_share;
	
	private boolean speech = true;
	private boolean autoDelete = false;
	
	TranslationActivity translationActivity;
	
	TextView translation;
	TextView preview;
	
	private Vibrator vibrator;
	
	private TextToSpeech myTTS;
	
	private int MY_DATA_CHECK_CODE = 0;
	
	private static final String TAG = "I2T";
	
	public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() +
												"/Image2Text/";
	public static final String lang = "eng";
	
	private TranslationListener translationListener;
	private ProgressDialog pd;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.translation_screen);
        
        String path = getIntent().getStringExtra("path");
        speech = getIntent().getBooleanExtra("speech", autoDelete);
        autoDelete = getIntent().getBooleanExtra("autoDelete", autoDelete);
        
        translationActivity = this;
        
        picture = (Button) findViewById(R.id.picture);
        save = (Button) findViewById(R.id.save);
        share = (Button) findViewById(R.id.share);
        translation = (TextView) findViewById(R.id.textView1);
        preview = (TextView) findViewById(R.id.textView2);
        
        vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        Resources res = getResources();
        
        unpressed_save = res.getDrawable(R.drawable.savebutton);
        pressed_save = res.getDrawable(R.drawable.savebuttonpressed);
        
        unpressed_share = res.getDrawable(R.drawable.sharebutton);
        pressed_share = res.getDrawable(R.drawable.sharebuttonpressed);
        
        setPicture(path);
        
        File dir = new File(DATA_PATH + "tessdata");
        dir.mkdirs();
        
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, MY_DATA_CHECK_CODE);
        
        Thread thread = new Thread(this);
        thread.start();
        
        
 		if (!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata")).exists()) {
 			try {

 				AssetManager assetManager = getAssets();
 				InputStream in = assetManager.open("eng.traineddata");
 				OutputStream out = new FileOutputStream(DATA_PATH
 						+ "tessdata/eng.traineddata");

 				byte[] buf = new byte[1024];
 				int len;
 				while ((len = in.read(buf)) != -1) {
 					out.write(buf, 0, len);
 				}
 				in.close();
 				out.close();
 			} catch (IOException e) {}
 			
 		}
 		
 		save.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				vibrator.vibrate(50);
				save.setBackgroundDrawable(pressed_save);
				
				new CountDownTimer(1000, 100) {
                    public void onTick(long millis) {}
    				public void onFinish() {
    					save.setBackgroundDrawable(unpressed_save);
    					
    					String s = new Date(System.currentTimeMillis()).toString();
    					s = s.replaceAll("\\s", "_");
    					s = s.replaceAll(":", "");
    					String path = "I2T" + s + ".txt";
    					save(path , translation.getText().toString());
    					Toast.makeText(translationActivity, "Text saved to: " + path, Toast.LENGTH_LONG).show();
                    }
                 }.start();	
			}
        	
        });
 		
 		share.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				vibrator.vibrate(50);
				share.setBackgroundDrawable(pressed_share);
				
				shareIt();
				new CountDownTimer(1000, 100) {
                    public void onTick(long millis) {}
    				public void onFinish() {
    					share.setBackgroundDrawable(unpressed_share);
                    }
                 }.start();
			}

			private void shareIt() {
				Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
				sharingIntent.setType("text/plain");
				String shareSubject = "This message was sent using Image2Text!";
				sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, shareSubject);
				sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT,translation.getText().toString());
				startActivity(Intent.createChooser(sharingIntent, "Share via"));
			}
        	
        });
 		//runTesseract();
 		
    }
    
    public void doPause(View v)
    {
    	vibrator.vibrate(50);
    	
    	if (!translationListener.getText().equals(translation.getText().toString()))
    		translationListener.setText(translation.getText().toString());
    	
    	
    	
    	translationListener.togglePaused();
    }

    public void run(){//Tesseract() {
    	
    	runOnUiThread(new Runnable(){

			public void run() {
				pd = ProgressDialog.show(translationActivity, "", "Please Wait, while your image is being processed.", true, true);
			}
			
		});
    	
 		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 4;

		Bitmap bitmap = BitmapFactory.decodeFile(path, options);
 		
		try {
			ExifInterface exif = new ExifInterface(path);
			int exifOrientation = exif.getAttributeInt(
					ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL);

			Log.v(TAG, "Orient: " + exifOrientation);

			int rotate = 0;

			switch (exifOrientation) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				rotate = 90;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				rotate = 180;
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				rotate = 270;
				break;
			}

			Log.v(TAG, "Rotation: " + rotate);

			if (rotate != 0) {

				// Getting width & height of the given image.
				int w = bitmap.getWidth();
				int h = bitmap.getHeight();

				// Setting pre rotate
				Matrix mtx = new Matrix();
				mtx.preRotate(rotate);

				// Rotating Bitmap
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
			}

			// Convert to ARGB_8888, required by tess
			bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
			bitmap = ConvertToBlackAndWhite(bitmap);

		} catch (IOException e) {
			Log.e(TAG, "Couldn't correct orientation: " + e.toString());
		}
		
		Log.v(TAG, "Before baseApi");

		TessBaseAPI baseAPI = new TessBaseAPI();
		baseAPI.setDebug(true);
		baseAPI.init(DATA_PATH, lang);
		baseAPI.setImage(bitmap);
        
 		String recognizedText = baseAPI.getUTF8Text();
        
 		baseAPI.end();
 		
 		if ( lang.equalsIgnoreCase("eng") ) {
			recognizedText = recognizedText.replaceAll("[^a-zA-Z0-9]+", " ");
		}

		recognizedText = recognizedText.trim();

		final String tempString = recognizedText;
		
		runOnUiThread(new Runnable(){

			public void run() {
				// TODO Auto-generated method stub
				if ( tempString.length() != 0 ) {
					translation.setText(tempString);
				}else{translation.setText("Sorry, no words found in image.");}
				
				pd.cancel();
			}
			
		});
			
 		
 		
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }
    
    public void setPicture(String path) {
    	this.path = path;
    	this.picture.setBackgroundDrawable((decodeSampledBitmapFromResource(path, 100, 100)));
    	
    }
    
    public static Drawable decodeSampledBitmapFromResource(String path,
            int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return new BitmapDrawable(BitmapFactory.decodeFile(path, options));
    }
    
    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
	    // Raw height and width of image
	    final int height = options.outHeight;
	    final int width = options.outWidth;
	    int inSampleSize = 1;
	
	    if (height > reqHeight || width > reqWidth) {
	        if (width > height) {
	            inSampleSize = Math.round((float)height / (float)reqHeight);
	        } else {
	            inSampleSize = Math.round((float)width / (float)reqWidth);
	        }
	    }
	    return inSampleSize;
	}

    public void onInit(int initStatus) {
	        //check for successful instantiation
	    if (initStatus == TextToSpeech.SUCCESS) {
	        if(myTTS.isLanguageAvailable(Locale.US)==TextToSpeech.LANG_AVAILABLE)
	            myTTS.setLanguage(Locale.US);
	        
	        translationListener = new TranslationListener(myTTS);
	        myTTS.setOnUtteranceCompletedListener(translationListener);
	    }
	    else if (initStatus == TextToSpeech.ERROR) {
	        Toast.makeText(this, "Sorry! Text To Speech failed...", Toast.LENGTH_LONG).show();
    }
}
	
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
	
	public Bitmap ConvertToBlackAndWhite(Bitmap sampleBitmap){
		ColorMatrix bwMatrix =new ColorMatrix();
		bwMatrix.setSaturation(0);
		final ColorMatrixColorFilter colorFilter= new ColorMatrixColorFilter(bwMatrix);
		Bitmap rBitmap = sampleBitmap.copy(Bitmap.Config.ARGB_8888, true);
		Paint paint=new Paint();
		paint.setColorFilter(colorFilter);
		Canvas myCanvas =new Canvas(rBitmap);
		myCanvas.drawBitmap(rBitmap, 0, 0, paint);
		return rBitmap;
		}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
    	super.onKeyDown(keyCode, event);
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		pd.cancel();
    		
    		if (autoDelete) {
    			File file = new File(path);
    			file.deleteOnExit();
    		}
    		
    		finish();
    		return true;
    	}
    	return false;
    }
	
	public void save(String fileName, String text) {
		try {
				BufferedWriter out = new BufferedWriter(new FileWriter(DATA_PATH + fileName));
				out.write(text);
				out.close();
			} catch (IOException e) {}
	}
	
	
	
	private class TranslationListener implements OnUtteranceCompletedListener {
		
		private String sourceText = "";
		private StringTokenizer st;
		private TextToSpeech tts;
		private boolean isPaused = true;
		
		private String previous="";
		private String present="";
		private String next="";
		
		private HashMap<String, String> params = new HashMap<String,String>();
		
		public TranslationListener(TextToSpeech tts)
		{
			this.tts = tts;
			params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "translation");
		}
		
		public void setText(String text)
		{
			sourceText = text;
			this.st = new StringTokenizer(text, " ");
			//present = "";
			//next = st.nextToken();
		}
		
		public String getText()
		{
			return sourceText;
		}
		
		public void togglePaused()
		{
			if (speech) isPaused = !isPaused;
			
			if (!isPaused && speech)
				speakNextWord();
		}
		
		public void hasFinished()
		{
			if(!st.hasMoreTokens()){
				togglePaused();
				setText(sourceText);
			}
				
		}
		
		public void onUtteranceCompleted(String id) {
			if (!isPaused && id.equals("translation") && speech) {
				speakNextWord();
			}
		}
		
		
		public void speakNextWord() {
			
			//previous = present;
			previous = st.hasMoreTokens() ? st.nextToken() : "";
			//present = next;
			present = st.hasMoreTokens() ? st.nextToken() : "";
			next = st.hasMoreTokens() ? st.nextToken() : "";
			
			final String previewText = previous + " " + present + " " + next;
			
			
			runOnUiThread(new Runnable(){

				public void run() {
					// TODO Auto-generated method stub
					preview.setText(previewText);
				}
				
			});
			
			tts.speak(previewText, TextToSpeech.QUEUE_FLUSH, params);
			hasFinished();
		}
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
           if(item.getItemId()==R.id.toggle_readback) {
              if (speech == true) {
            	  Toast.makeText(this, "Readback Disabled", Toast.LENGTH_LONG).show();
            	  speech = false;
              } else {
            	  Toast.makeText(this, "Readback Enabled", Toast.LENGTH_LONG).show();
            	  speech = true;
              } 
           }else if(item.getItemId()==R.id.toggle_auto_delete){
        	   if (autoDelete == true) {
        		   Toast.makeText(this, "Picture Auto-Delete Disabled", Toast.LENGTH_LONG).show(); 
        		   autoDelete = false;
        	   } else {
        		   Toast.makeText(this, "Picture Auto-Delete Enabled", Toast.LENGTH_LONG).show();
        		   autoDelete = true;
        	   }
           }
           
		return true;
	}
}
