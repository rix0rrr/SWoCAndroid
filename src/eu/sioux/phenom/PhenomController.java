package eu.sioux.phenom;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Vector;

import org.kobjects.base64.Base64;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

/*
 * This class handles the logic when the user presses the
 * power button
 */
public class PhenomController {

	String TAG = PhenomController.class.getSimpleName();
	
	/*
	 * these static ints (constants) are being used by the message handler
	 */
	final static int GET_OPERATIONAL_MODE = 0;
	public final static int GET_OPERATIONAL_MODE_RESULT = 1;

	/*
	 * This variable will be used to store the reference to the handler of
	 * the parent. This way we can asynchronously report a status update  
	 */
	Handler mFeedbackHandler = null;
	
	public PhenomController(Handler handler) {
		mFeedbackHandler = handler;
	}

	void getOperationalMode() {

		/*
		 * 
		 */
		new asyncPowerHandling().execute(GET_OPERATIONAL_MODE);
	}
	
	void retrieveLiveImage(ImageView view) {
		new RetrieveLiveImage(view).execute();
	}

	private static final String WebServiceSoapAction = "";
	private static final String WebServiceMethodName = "GetInstrumentMode"; //GetOperationalMode
	private static final String WebServiceNameSpace  = "http://tempuri.org/om.xsd";
	private static final String WebServiceUrl        = "http://192.168.24.4:8888/";			
	
	private void GetInstrumentMode()
	{	
        SoapObject request = new SoapObject(WebServiceNameSpace, WebServiceMethodName);        
        //request.addProperty("PropertyName", "PropertyValue");
        SoapSerializationEnvelope soapEnvelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);		
        soapEnvelope.setOutputSoapObject(request);
        
        HttpTransportSE hts = new HttpTransportSE(WebServiceUrl);
        try {
        	Log.i("bla", "Before call");
			hts.call(WebServiceSoapAction, soapEnvelope);
        	Log.i("bla", "Aftercall");
        	
			Vector soapResult     = (Vector)soapEnvelope.getResponse();
			String instrumentMode = ((SoapPrimitive)soapResult.get(0)).toString();

			Message msg = new Message();
			msg.obj = instrumentMode;
			msg.what = GET_OPERATIONAL_MODE_RESULT;
			mFeedbackHandler.sendMessage(msg);
			
		} catch (Exception e) {
			Log.e("ERROR", e.getMessage());				
		}	        
	}
	
	/*
	 * This calls is used to do an asynchronous task
	 */
	private class asyncPowerHandling extends AsyncTask<Integer, Void, Void> {

		/*
		 * This function is called before doInBackground().
		 * 
		 * it runs in the UI thread!
		 */
		@Override
		protected void onPreExecute() {

		}

		/*
		 * this function does the actual work that has to be done
		 * asynchronously.
		 * 
		 * changes to the UI are not allowed here, as it does NOT run in the UI
		 * thread!
		 */
		@Override
		protected Void doInBackground(Integer... params) {
			if (params[0] == GET_OPERATIONAL_MODE) {
				GetInstrumentMode();
			}
			return null;
		}
		
		/*
		 * This function is called after doInBackground(). The argument is the
		 * return value of doInBackground().
		 * 
		 * it runs in the UI thread!
		 */
		@Override
		protected void onPostExecute(Void result) {
		}
	}
	
	private class RetrieveLiveImage extends AsyncTask<Void, Void, Bitmap> {
		
		private ImageView imageView;
		
		public RetrieveLiveImage(ImageView imageView) {
			this.imageView = imageView;
		}

		@Override
		protected Bitmap doInBackground(Void... params) {
	        SoapObject request = new SoapObject(WebServiceNameSpace, "SEMGetLiveImageCopy");        
	        request.addProperty("nFramesDelay", 1);
	        SoapSerializationEnvelope soapEnvelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);		
	        soapEnvelope.setOutputSoapObject(request);
	        
	        HttpTransportSE hts = new HttpTransportSE(WebServiceUrl);
	        
			try {
	        	Log.i("bla", "About to fuck off");
				hts.call(WebServiceSoapAction, soapEnvelope);
	        	Log.i("bla", "Aaaaand we're back!");
				
				Vector soapResult     = (Vector)soapEnvelope.getResponse();
				LogSoapThingy(soapResult.get(0));
				LogSoapThingy(soapResult.get(1));
				LogSoapThingy(soapResult.get(2));
				LogSoapThingy(soapResult.get(3));
				
				byte[] bytes = Base64.decode(soapResult.get(0).toString());
				SoapObject properties = (SoapObject)soapResult.get(1);
				int width = Integer.parseInt(properties.getProperty("width").toString());
				int height = Integer.parseInt(properties.getProperty("height").toString());
				
				return makeBitmapFromGrayscale(bytes, width, height);				
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			} catch (XmlPullParserException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			// Time to display that bitmap beyotches
			if (result != null) {
				imageView.setImageBitmap(result);
			}
		}
	}
	
	private Bitmap makeBitmapFromGrayscale(byte[] bytes, int width, int height) {
		// Convert to 32-bits ARGB cause Android doesn't understand 8bit grayscale
		byte[] pixels = new byte[width * height * 4];
		for (int i = 0, j = 0; i < width * height * 4; i += 4, j++) {
			pixels[i] = bytes[j];
			pixels[i + 1] = bytes[j];
			pixels[i + 2] = bytes[j];
			pixels[i + 3] = (byte)0xFF;
		}
		
		Bitmap ret = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		ret.copyPixelsFromBuffer(ByteBuffer.wrap(pixels));
		return ret;
		/*
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.outWidth  = width;
		opts.outHeight = height;
		return BitmapFactory.decodeByteArray(pixels, 0, pixels.length, opts);
		*/
	}
	
	private void LogSoapThingy(Object o) {
		if (o instanceof SoapPrimitive) LogSoapPrimitive((SoapPrimitive)o);
		else if (o instanceof SoapObject) LogSoapObject((SoapObject)o);
		else Log.w("soap", "Not a soap thingy: " + o.toString());
	}
	
	private void LogSoapPrimitive(SoapPrimitive p) {
		Log.i("soap", "SoapPrimitive " + p.getName() + ": " + p.toString());
	}
	
	private void LogSoapObject(SoapObject o) {
		Log.i("soap", "SoapObject " + o.getName() + ": " + o.toString());
		for (int i = 0; i < o.getPropertyCount(); i++) {
			Log.i("soap", "property " + i);
			LogSoapThingy(o.getProperty(i));
		}
	}
}
