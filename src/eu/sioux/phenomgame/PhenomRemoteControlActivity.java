package eu.sioux.phenomgame;

import java.util.Random;

import nl.mvdvlist.test.Analog2dController;
import nl.mvdvlist.test.AnalogControlListener;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import eu.sioux.phenomgame.PhenomController.Point;
import eu.sioux.phenomgame.PhenomController.Stroke;

public class PhenomRemoteControlActivity extends Activity {
	/** Called when the activity is first created. */
	
	private final double stepSize = 1E-4;

	PhenomController phenomController = null;
	private Button mGetOperationalModeBtn = null;
	private TextView mOperationalModeTextView = null;
	private TextView statusLabel;
	private ImageView currentImage;
	private ImageView referenceImage;
	
	private final int ST_STROKE = 1;
	private final int ST_MOVING = 2;
	private final int ST_AT_HIDDEN_POS = 3;
	private final int ST_HIDDEN_PIC_CAPPED = 4;
	private final int ST_PREPARE_GAME = 5;
	private final int ST_DISPLAY_IMAGE = 6;
	private final int ST_HIDING = 7;
	private final int ST_SMALLSTEP = 8;
	private final int ST_LIVE = 9;
	
	private Stroke stageStroke;
	private Point searchPoint;
	private boolean live = false;

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
		statusLabel    = (TextView)findViewById(R.id.statusLabel);
		currentImage   = (ImageView)findViewById(R.id.currentImage);
//		referenceImage = (ImageView)findViewById(R.id.referenceImage);
		
		Analog2dController analog = (Analog2dController)findViewById(R.id.analogControl);
		analog.addListener(new AnalogControlListener() {
			@Override
			public void onPositionChanged(double currentAngle, double currentStrength) {
								
			}
		});

		phenomController = new PhenomController(statusHandler);
	}
	
	public void startLive() {
		live = true;
		phenomController.retrieveLiveImage(1, ST_LIVE);
	}
	
	public void stopLive() {
		live = false;
	}
	
	public void newGameClick(View v) {
		// Begin with getting the Stroke, the state machine inside the handler does the rest
		stopLive();
		phenomController.getStroke(ST_STROKE);
	}

	public void refreshClick(View v) {
		// Begin with getting the Stroke, the state machine inside the handler does the rest
		phenomController.retrieveLiveImage(1, ST_DISPLAY_IMAGE);
	}
	
	public void moveUpClick(View v) {
		phenomController.moveBy(new Point(0, stepSize), ST_SMALLSTEP);
	}
	
	public void moveLeftClick(View v) {
		phenomController.moveBy(new Point(-stepSize, 0), ST_SMALLSTEP);
	}
	
	public void moveRightClick(View v) {
		phenomController.moveBy(new Point(stepSize, 0), ST_SMALLSTEP);
	}
	
	public void moveDownClick(View v) {
		phenomController.moveBy(new Point(0, -stepSize), ST_SMALLSTEP);
	}

	/**
	 * Pick a random point in the current stroke
	 */
	private Point randomPoint() {
		Random r  = new Random();
		double dx = stageStroke.bottomRight.x - stageStroke.topLeft.x;
		double dy = stageStroke.bottomRight.y - stageStroke.topLeft.y;
		return new Point(
				r.nextDouble() * dx + stageStroke.topLeft.x,
				r.nextDouble() * dy + stageStroke.topLeft.y);
	}
	
	Handler statusHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case ST_STROKE:
					stageStroke = (Stroke)msg.obj;
					searchPoint = randomPoint();
					phenomController.moveTo(searchPoint, ST_MOVING);
					break;
					
				case ST_MOVING:
					waitMs(3000, ST_AT_HIDDEN_POS);
					break;
					
				case ST_AT_HIDDEN_POS:
					phenomController.retrieveLiveImage(1, ST_HIDDEN_PIC_CAPPED);
					break;
					
				case ST_HIDDEN_PIC_CAPPED:
					//referenceImage.setImageBitmap((Bitmap)msg.obj);
					phenomController.moveTo(randomPoint(), ST_HIDING);
					break;
					
				case ST_HIDING:
					waitMs(3000, ST_PREPARE_GAME);
					break;
					
				case ST_PREPARE_GAME:
					startLive();
					break;
					
				case ST_DISPLAY_IMAGE:
					currentImage.setImageBitmap((Bitmap)msg.obj);
					break;
					
				case ST_SMALLSTEP:
					waitMs(100, ST_PREPARE_GAME);
					break;
					
				case ST_LIVE:
					currentImage.setImageBitmap((Bitmap)msg.obj);
					if (live) phenomController.retrieveLiveImage(1, ST_LIVE);
					break;
				
			
				default:
					statusLabel.setText((String)msg.obj);
			}
		}
	};
	
	private void waitMs(final int ms, final int next) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(ms);
				} catch (InterruptedException e) {
				}
				
				Message m = new Message();
				m.what = next;
				statusHandler.sendMessage(m);
			}
		}).start();
	}
}

