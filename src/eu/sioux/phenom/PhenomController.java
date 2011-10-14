package eu.sioux.phenom;

import java.util.Vector;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

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
			hts.call(WebServiceSoapAction, soapEnvelope);
			
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
}
