package eu.sioux.phenom;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class PhenomRemoteControlActivity extends Activity {
	/** Called when the activity is first created. */

	PhenomController phenomController = null;
	private Button mGetOperationalModeBtn = null;
	private TextView mOperationalModeTextView = null;

	final static String TAG = PhenomController.class.getSimpleName();
	/*
	 * Called when the activity is created
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		
		/*
		 * here you set the content view. This is actually the user interface.
		 * 
		 * "R.layout.main" refers to "res/layout/main.xml" Android creates an
		 * R.java file in the "gen"-directory that creates these ID's. This is
		 * done for every resource in your project when compiling.
		 */
		setContentView(R.layout.main);

		/*
		 * the PhenomController class has been created to communicate with the Phenom
		 */
		phenomController = new PhenomController(mHandler);

		/*
		 * as we have set the layout above via setContentView, the items in that
		 * layout can be called by findViewById(). Let's get the button that gets the
		 * operational mode of the system
		 */
		mGetOperationalModeBtn = (Button) findViewById(R.id.getOperationalModeButton);
		
		/*
		 * here is the textview where we show the current operational mode in
		 */
		mOperationalModeTextView = (TextView) findViewById(R.id.getOperationalModeTextView);

		/*
		 * now when the button gets pressed, we want the PhenomController to get the
		 * current operational state. Here we create an onClickListener and connect it
		 * to the button
		 */
		mGetOperationalModeBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				/*
				 * this function is non-blocking. We will be receiving the status via the
				 * handler passed to the PhenomController constructor
				 */
				phenomController.getOperationalMode();
			}
		});
		
	}
	/*
	 * this is the message handler that we passed on to the PhenomController object.
	 * It will send messages when it has new data. It doesn't matter from which thread
	 * the message is send, all messages will always be handled here in the same thread.
	 * 
	 * This is imported when changing UI. You are only allowed to change UI stuff in the
	 * UI thread. So handling UI changes in this handler is safe as it is always being done
	 * in the UI thread.
	 */
	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			/*
			 * messages send via the Handler methods, like sendMessage() or sendEmptyMessage, 
			 * will end up in this handler. The message ID can be derived from msg.what. So
			 * here we the handle different messages.
			 * 
			 * Only one at the moment...
			 */
			if (msg.what == PhenomController.GET_OPERATIONAL_MODE_RESULT) {
				/*
				 * Hey! We received operational mode info, show it in the textview!
				 */
				String result = (String) msg.obj;
				mOperationalModeTextView.setText(result);				
			} else {
				/*
				 * check the logcat for this message in case of problems to see if you message
				 * is being handled.
				 */
				Log.e(TAG, "Unhandled message from PhenomController: " + Integer.toString(msg.what));
			}
		}
	};
}

