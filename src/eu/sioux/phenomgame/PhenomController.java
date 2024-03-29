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

import android.content.Context;
import android.graphics.Bitmap;
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
	private Context myContext = null; 
	
	/*
	 * these static ints (constants) are being used by the message handler
	 */
	final static int GET_OPERATIONAL_MODE = 0;
	public final static int GET_OPERATIONAL_MODE_RESULT = 1;

	private static final String WebServiceSoapAction = "";
	private static final String WebServiceNameSpace  = "http://tempuri.org/om.xsd";

	private static String PHENOM_NS = "http://tempuri.org/om.xsd";

	Handler statusHandler;
	
	public PhenomController(Handler statusHandler, Context context) {
		this.statusHandler = statusHandler;
		this.myContext = context;
	}
	
	void doAsync(final Runnable r) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					r.run();					
				} catch (Exception e) {
					setStatus(e.toString());
				}				
			}
		}).start();
	}
	
	
	void retrieveLiveImage(final int nFrameDelay, final int nextState) {
		doAsync(new Runnable() {
			@Override
			public void run() {
				Map<String, Object> p = new HashMap<String, Object>() {{
					put("nFrameDelay", new Integer(nFrameDelay));
				}};
				
				Vector soapResult = (Vector)performSoapRequest("SEMGetLiveImageCopy", p);
				
				byte[] bytes = Base64.decode(soapResult.get(0).toString());
				SoapObject properties = (SoapObject)soapResult.get(1);
				int width  = Integer.parseInt(properties.getProperty("width").toString());
				int height = Integer.parseInt(properties.getProperty("height").toString());
				
				postback(nextState, makeBitmapFromGrayscale(bytes, width, height));				
			}
		});
	}
	
	void setStatus(String status) {
		Message m = new Message();
		m.obj = status;
		statusHandler.sendMessage(m);
	}
		
	void acquireImage(final String detector, final Integer widthHeight, final Integer nrOfFrames, final ImageView view) {
		doAsync(new Runnable() {
			@Override
			public void run() {
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
				
				view.setImageBitmap(makeBitmapFromGrayscale(bytes, width, height));				
			}
		});
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
		doAsync(new Runnable() {
			@Override
			public void run() {
				Vector result = (Vector)performSoapRequest("GetStageStroke", new HashMap<String, Object>());
				
				SoapObject semMin = (SoapObject)result.get(2);
				SoapObject semMax = (SoapObject)result.get(3);
				
				postback(what, new Stroke(soapPoint(semMin), soapPoint(semMax)));
			}
		});
	}
	
	private Point soapPoint(SoapObject pt) {
		return new Point(
				Double.parseDouble(pt.getProperty("x").toString()),
				Double.parseDouble(pt.getProperty("y").toString()));
	}
	
	public void moveTo(final Point point, final int what) {
		doAsync(new Runnable() {
			@Override
			public void run() {
				performSoapRequest("MoveTo", new HashMap<String, Object>() {{
					put("aPos", new SoapObject(PHENOM_NS, "position") {{
						addProperty("x", new Double(point.x));
						addProperty("y", new Double(point.y));
					}});
					put("algorithm", NavigationAlgorithm.Auto);
				}});
					
				postback(what, null);
			}
		});
	}
	
	public void moveBy(final Point delta, final int what) {
		doAsync(new Runnable() {
			@Override
			public void run() {
				performSoapRequest("MoveBy", new HashMap<String, Object>() {{
					put("aPos", new SoapObject(PHENOM_NS, "position") {{
						addProperty("x", new Double(delta.x));
						addProperty("y", new Double(delta.y));
					}});
					put("algorithm", NavigationAlgorithm.Raw);
				}});
				
				postback(what, null);
			}
		});
	}
	
	/**
	 * Set the jog
	 * 
	 * If fovCoordinates is true, vx and vy are a fraction of the FOV.
	 * Otherwise, m/s?? 
	 */
	public void setJog(final double vx, final double vy, final boolean fovCoordinates, final int nextStep) {
		doAsync(new Runnable() {
			@Override
			public void run() {
				performSoapRequest("Jog", new HashMap<String, Object>() {{
					put("aSpeed", new SoapObject(PHENOM_NS, "jogVector") {{
						addProperty("x", new Double(vx));
						addProperty("y", new Double(vy));
					}});
					put("aSIPercFoV", new Boolean(fovCoordinates));
				}});
				
				postback(nextStep, null);
			}
		});
	}
	
	public void stop(final int nextStep) {
		doAsync(new Runnable() {
			@Override
			public void run() {
				performSoapRequest("Stop", new HashMap<String, Object>());
				postback(nextStep, null);
			}
		});		
	}
	
	public void getPosition(final int nextStep) {
		doAsync(new Runnable() {
			@Override
			public void run() {
				Vector result = (Vector)performSoapRequest("GetStageModeAndPosition", new HashMap<String, Object>());
				
				SoapObject pos = (SoapObject)result.get(1);
				
				postback(nextStep, soapPoint(pos));
			}
		});		
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
        
        //logLongString(envelopeToString(soapEnvelope));
        
        String url = "http://" +Preferences.getIpAddress(myContext) + ":" + Preferences.getPortNumber(myContext);
        HttpTransportSE hts = new HttpTransportSE(url);
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
			//throw new RuntimeException(e1);
			return null;
		} catch (XmlPullParserException e2) {
			setStatus(e2.toString());
			Log.e("soap", "Call failed", e2);
			return null;
			//throw new RuntimeException(e2);
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
