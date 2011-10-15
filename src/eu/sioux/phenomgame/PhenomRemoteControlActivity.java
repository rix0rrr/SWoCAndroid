package eu.sioux.phenomgame;

import java.util.Random;

import nl.mvdvlist.test.Analog2dController;
import nl.mvdvlist.test.AnalogControlListener;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import eu.sioux.phenomgame.PhenomController.Point;
import eu.sioux.phenomgame.PhenomController.Stroke;

public class PhenomRemoteControlActivity extends Activity {
	/** Called when the activity is first created. */
	
	private final double maxSpeed = 0.004;
	private final boolean speedInFoVFraction = false;
	
	private final double significantJogDifferenceFrac = 0.05;

	PhenomController phenomController = null;
	private TextView statusLabel;
	private ImageView currentImage;
	private ImageView referenceImage;
	private TextView xcoord;
	private TextView ycoord;
	
	private double lastJogX;
	private double lastJogY;
	
	private final int ST_STROKE = 1;
	private final int ST_MOVING = 2;
	private final int ST_AT_HIDDEN_POS = 3;
	private final int ST_HIDDEN_PIC_CAPPED = 4;
	private final int ST_PREPARE_GAME = 5;
	private final int ST_DISPLAY_IMAGE = 6;
	private final int ST_HIDING = 7;
	private final int ST_SMALLSTEP = 8;
	private final int ST_LIVE = 9;
	private final int ST_POSITION = 10;
	
	private Stroke stageStroke;
	private Point searchPoint;
	private Point currentPoint;
	private boolean live = false;
	private boolean updatingPosition = false;

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
  		referenceImage = (ImageView)findViewById(R.id.referenceImage);
  		xcoord         = (TextView)findViewById(R.id.xcoord);
  		ycoord         = (TextView)findViewById(R.id.ycoord);
		
		Analog2dController analog = (Analog2dController)findViewById(R.id.analogControl);
		analog.addListener(new AnalogControlListener() {
			@Override
			public void onPositionChanged(double fx, double fy) {
				if (Math.abs(fx) < 0.01 && Math.abs(fy) < 0.01)
					phenomController.stop(-1);
				else if (Math.abs(fx - lastJogX) > significantJogDifferenceFrac || 
						Math.abs(fy - lastJogY) > significantJogDifferenceFrac) {
					
					Log.i("poschange", fx + "," + fy);
					phenomController.setJog(fx * maxSpeed, -fy * maxSpeed, speedInFoVFraction, -1);
					
					lastJogX = fx;
					lastJogY = fy;
				}
			}
		});

		phenomController = new PhenomController(statusHandler, this);
	}
	
	public void startLive() {
		if (!live) {
			live = true;
			phenomController.retrieveLiveImage(1, ST_LIVE);
		}
	}
	
	public void stopLive() {
		live = false;
	}
	
	public void startUpdatingPosition() {
		if (!updatingPosition) {
			updatingPosition = true;
			phenomController.getPosition(ST_POSITION);
		}
	}
	
	public void endUpdatingPosition() {
		updatingPosition = false;
	}
	
	public void newGameClick(View v) {
		// Begin with getting the Stroke, the state machine inside the handler does the rest
		stopLive();
		phenomController.getStroke(ST_STROKE);
	}

	public void refreshClick(View v) {
		// Begin with getting the Stroke, the state machine inside the handler does the rest
		startLive();
		startUpdatingPosition();
	}
	
	public void settingsClick(View v) {
		startActivity(new Intent(this, Preferences.class));
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
	
	private double distanceToSearchPointPx() {
		if (currentPoint == null || searchPoint == null) return 1;
		
		double dx = currentPoint.x - searchPoint.x;
		double dy = currentPoint.y - searchPoint.y;
		return Math.sqrt(dx * dx + dy * dy);
	}
	
	private double distanceToSearchPointFraction() {
		if (stageStroke == null) return 1;
		
		return Math.min(distanceToSearchPointPx() / (stageStroke.bottomRight.x - stageStroke.topLeft.x), 1); 		
	}
	
	private int interpolateColor(int one, int two, double fraction) {
		
		int r = (int)((one >> 16 & 0xFF) + ((two >> 16 & 0xFF) - (one >> 16 & 0xFF)) * fraction);
		int b = (int)((one >>  8 & 0xFF) + ((two >>  8 & 0xFF) - (one >>  8 & 0xFF)) * fraction);
		int g = (int)((one       & 0xFF) + ((two       & 0xFF) - (one       & 0xFF)) * fraction);
		Log.i("bla", "one = " + one + " two = " + two + " r = " + r + " g = " + g + "b = " + b);
		return b << 24 + g << 16 + r << 8 + 0xFF;
	}
	
	private void updateCoordLabels() {
		xcoord.setText("" + currentPoint.x);
		ycoord.setText("" + currentPoint.y);
		
		int color = interpolateColor(0x00FF00, 0xFF0000, distanceToSearchPointFraction());
		//xcoord.setTextColor(color);
		xcoord.setText("" + color);
		ycoord.setTextColor(color);
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
					referenceImage.setImageBitmap((Bitmap)msg.obj);
					phenomController.moveTo(randomPoint(), ST_HIDING);
					break;
					
				case ST_HIDING:
					waitMs(3000, ST_PREPARE_GAME);
					break;
					
				case ST_PREPARE_GAME:
					startLive();
					startUpdatingPosition();
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
					
				case ST_POSITION:
					currentPoint = (Point)msg.obj;
					updateCoordLabels();
					if (updatingPosition) phenomController.getPosition(ST_POSITION);
					break;
			
				case 0:
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

