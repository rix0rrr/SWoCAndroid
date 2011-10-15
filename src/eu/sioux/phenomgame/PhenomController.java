package eu.sioux.phenomgame;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.kobjects.base64.Base64;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.graphics.Bitmap;
import android.media.MediaRecorder.OutputFormat;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

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

	private static final String WebServiceSoapAction = "";
	private static final String WebServiceMethodName = "GetInstrumentMode"; //GetOperationalMode
	private static final String WebServiceNameSpace  = "http://tempuri.org/om.xsd";
	private static final String WebServiceUrl        = "http://192.168.24.25:8888/";			

	private static String PHENOM_NS = "http://tempuri.org/om.xsd";

	Handler statusHandler;
	
	public PhenomController(Handler statusHandler) {
		this.statusHandler = statusHandler;
	}

	void retrieveLiveImage(final int nFrameDelay, final int nextState) {
		new AsyncTask<Void, Void, Bitmap>() {
			@Override
			protected Bitmap doInBackground(final Void... params) {
				Map<String, Object> p = new HashMap<String, Object>() {{
					put("nFrameDelay", new Integer(nFrameDelay));
				}};
				
				Vector soapResult = (Vector)performSoapRequest("SEMGetLiveImageCopy", p);
				
				byte[] bytes = Base64.decode(soapResult.get(0).toString());
				SoapObject properties = (SoapObject)soapResult.get(1);
				int width  = Integer.parseInt(properties.getProperty("width").toString());
				int height = Integer.parseInt(properties.getProperty("height").toString());
				
				return makeBitmapFromGrayscale(bytes, width, height);				
			}

			@Override
			protected void onPostExecute(Bitmap result) {
				postback(nextState, result);
			}
		}.execute();
	}
	
	void setStatus(String status) {
		Message m = new Message();
		m.obj = status;
		statusHandler.sendMessage(m);
	}
		
	void acquireImage(final String detector, final Integer widthHeight, final Integer nrOfFrames, final ImageView view) {
		new AsyncTask<Void, Void, Bitmap>() {

			@Override
			protected Bitmap doInBackground(Void... arg0) {
				Map<String, Object> request = new HashMap<String, Object>() {{
					put("scan", new SoapObject(PHENOM_NS, "scanParams") {{
						put("det", detector);
						put("res", new SoapObject(PHENOM_NS, "resolution") {{
							addProperty("width", widthHeight);
							addProperty("height", widthHeight);
						}});
						put("nrOfFrames", nrOfFrames);
					}});
				}};
				
				Vector soapResult = (Vector)performSoapRequest("SEMAcquireImageCopy", request);
				
				byte[] bytes = Base64.decode(soapResult.get(0).toString());
				SoapObject properties = (SoapObject)soapResult.get(1);
				int width = Integer.parseInt(properties.getProperty("width").toString());
				int height = Integer.parseInt(properties.getProperty("height").toString());
				
				return makeBitmapFromGrayscale(bytes, width, height);				
			}

			@Override
			protected void onPostExecute(Bitmap result) {
				if (result != null) {
					view.setImageBitmap(result);
				}
			}
			
		}.execute();
	}
	
	private void postback(int what, Object obj) {
		Message m = new Message();
		m.what = what;
		m.obj = obj;
		statusHandler.sendMessage(m);
	}
	
	/**
	 * Determine stroke, post back to the handler as a Stroke class using the code what
	 */
	public void getStroke(final int what)
	{
		new AsyncTask<Void, Void, Stroke>() {
			@Override
			protected Stroke doInBackground(Void... params) {
				Vector result = (Vector)performSoapRequest("GetStageStroke", new HashMap<String, Object>());
				
				SoapObject semMin = (SoapObject)result.get(2);
				SoapObject semMax = (SoapObject)result.get(3);
				
				return new Stroke(soapPoint(semMin), soapPoint(semMax));
			}
			
			@Override
			protected void onPostExecute(Stroke result) {
				postback(what, result);
			}
		}.execute();
	}
	
	private Point soapPoint(SoapObject pt) {
		return new Point(
				Double.parseDouble(pt.getProperty("x").toString()),
				Double.parseDouble(pt.getProperty("y").toString()));
	}
	
	public void getInstrumentMode(final TextView into)
	{	
		new AsyncTask<Void, Void, String>() {

			@Override
			protected String doInBackground(Void... params) {
				Vector result = (Vector)performSoapRequest("GetInstrumentMode", new HashMap<String, Object>());
				return (String)result.get(0).toString();
			}

			@Override
			protected void onPostExecute(String result) {
				into.setText(result);
			}
		}.execute();
	}
	
	public void moveTo(final Point point, final int what) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				performSoapRequest("MoveTo", new HashMap<String, Object>() {{
					put("aPos", new SoapObject(PHENOM_NS, "position") {{
						addProperty("x", new Double(point.x));
						addProperty("y", new Double(point.y));
					}});
					put("algorithm", NavigationAlgorithm.Auto);
				}});
				
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				postback(what, null);
			}
		}.execute();
	}
	
	public void moveBy(final Point delta, final int what) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				performSoapRequest("MoveBy", new HashMap<String, Object>() {{
					put("aPos", new SoapObject(PHENOM_NS, "position") {{
						addProperty("x", new Double(delta.x));
						addProperty("y", new Double(delta.y));
					}});
					put("algorithm", NavigationAlgorithm.Raw);
				}});
				
				return null;
				
			}
			
			@Override
			protected void onPostExecute(Void result) {
				postback(what, null);
			}
		}.execute();
	}
	
	public void setJog(final double vx, final double vy, final boolean siUnit, final int nextStep) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				performSoapRequest("Jog", new HashMap<String, Object>() {{
					put("aSpeed", new SoapObject(PHENOM_NS, "jogVector") {{
						addProperty("x", new Double(vx));
						addProperty("y", new Double(vy));
					}});
					put("aSIPercFoV", new Boolean(siUnit));
				}});
				
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				postback(nextStep, null);
			}
		}.execute();
	}
	
	public void getPosition(final int nextStep) {
		new AsyncTask<Void, Void, Point>() {
			@Override
			protected Point doInBackground(Void... params) {
				Vector result = (Vector)performSoapRequest("GetStageModeAndPosition", new HashMap<String, Object>());
				
				SoapObject pos = (SoapObject)result.get(1);
				
				return soapPoint(pos);
			}
			
			@Override
			protected void onPostExecute(Point result) {
				postback(nextStep, result);
			}
		}.execute();		
	}
	
	private String envelopeToString(SoapEnvelope envelope) {
		StringWriter sw = new StringWriter();

		XmlPullParserFactory parserFactory;
		try {
	        parserFactory = XmlPullParserFactory.newInstance();
			parserFactory.setNamespaceAware(true);
			XmlSerializer serializer = parserFactory .newSerializer();
			serializer.setOutput(sw);
			
			envelope.write(serializer);
			
			return sw.toString();
		} catch (XmlPullParserException e) {
			return "ERROR";
		} catch (IllegalArgumentException e) {
			return "ERROR";
		} catch (IllegalStateException e) {
			return "ERROR";
		} catch (IOException e) {
			return "ERROR";
		}		
	}
	
	private void logLongString(String s) {
		int chunkSize = 100;
		for (int i = 0; i < s.length(); i += chunkSize) {
			int d = Math.min(chunkSize, s.length() - i);
			Log.i("soap", s.substring(i, i + d));
		}
	}
	
	/**
	 * Perform a soap request
	 * 
	 * Returns either a Vector if the response is complex (a sequence)
	 * or a SoapPrimitive.
	 */
	public Object performSoapRequest(String method, Map<String, Object> params) {
        SoapObject request = new SoapObject(WebServiceNameSpace, method);
        
        for (Entry<String, Object> e: params.entrySet()) {
        	request.addProperty(e.getKey(), e.getValue());
        }
        
        SoapSerializationEnvelope soapEnvelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        new MarshalDouble().register(soapEnvelope);
        new NavigationAlgorithm.Marshaller().register(soapEnvelope);
        soapEnvelope.setOutputSoapObject(request);
        
        logLongString(envelopeToString(soapEnvelope));
        
        HttpTransportSE hts = new HttpTransportSE(WebServiceUrl);
		try {
	        Log.i("soap", "Performing call to function: " + method);
	        setStatus("Poking the Phenom...");
			hts.call(WebServiceSoapAction, soapEnvelope);
	        Log.i("soap", "Returned!");
	        setStatus("Done!");
			
			return soapEnvelope.getResponse();
		} catch (IOException e1) {
			setStatus(e1.toString());
			Log.e("soap", "Call failed", e1);
			throw new RuntimeException(e1);
		} catch (XmlPullParserException e2) {
			setStatus(e2.toString());
			Log.e("soap", "Call failed", e2);
			throw new RuntimeException(e2);
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
	
	public static class Point {
		public final double x;
		public final double y;
		
		public Point(double x, double y) {
			this.x = x;
			this.y = y;
		}
	}
	
	public static class Stroke {
		public final Point topLeft;
		public final Point bottomRight;
		
		public Stroke(Point topLeft, Point bottomRight) {
			this.topLeft = topLeft;
			this.bottomRight = bottomRight;
		}
		
	}
}
