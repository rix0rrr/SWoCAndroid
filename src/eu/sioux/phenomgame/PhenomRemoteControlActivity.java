package eu.sioux.phenomgame;

import java.util.Random;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import eu.sioux.phenomgame.PhenomController.Point;
import eu.sioux.phenomgame.PhenomController.Stroke;

public class PhenomRemoteControlActivity extends Activity {
	/** Called when the activity is first created. */

	PhenomController phenomController = null;
	private Button mGetOperationalModeBtn = null;
	private TextView mOperationalModeTextView = null;
	private TextView statusLabel;
	
	private final int ST_STROKE = 1;
	private final int ST_MOVED_TO_SEARCH = 2;
	private Stroke stageStroke;
	private Point searchPoint;

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
		statusLabel = (TextView)findViewById(R.id.statusLabel);
		phenomController = new PhenomController(statusHandler);

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
				phenomController.getInstrumentMode(mOperationalModeTextView);
			}
		});
	}
	
	public void clickGetLiveImage(View v) {
		ImageView i = (ImageView)findViewById(R.id.imageView);
		phenomController.retrieveLiveImage(1, i);
	}
	
	public void clickAcquireImage(View v) {
		ImageView i = (ImageView)findViewById(R.id.imageView);
		phenomController.getStroke(ST_STROKE);
	}
	
	private void selectSearchPoint() {
		Random r  = new Random();
		double dx = stageStroke.bottomRight.x - stageStroke.topLeft.x;
		double dy = stageStroke.bottomRight.y - stageStroke.topLeft.y;
		searchPoint = new Point(
				r.nextDouble() * dx + stageStroke.topLeft.x,
				r.nextDouble() * dy + stageStroke.topLeft.y);
	}
	
	Handler statusHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case ST_STROKE:
					stageStroke = (Stroke)msg.obj;
					selectSearchPoint();
					phenomController.moveTo(searchPoint, ST_MOVED_TO_SEARCH);
					break;
					
				case ST_MOVED_TO_SEARCH:
					statusLabel.setText("I moved!");
			
				default:
					statusLabel.setText((String)msg.obj);
			}
		}
	};
}

