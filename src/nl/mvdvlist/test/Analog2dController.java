package nl.mvdvlist.test;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;


public class Analog2dController extends View {
	private Paint  outerBorderPaint;
	private Paint  dotPaint;
	private int    controlSize;
	private Path   outerBorderPath;
	private double currentAngle, currentStrength;
	private int    dotPositionX, controlCenterX;
	private int    dotPositionY, controlCenterY;
	private Set<AnalogControlListener> listeners;
	private static int DOTRADIUS = 10;

	public Analog2dController(Context context) {
		super(context);
		init();
	}

	public Analog2dController(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public Analog2dController(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	public void addListener(AnalogControlListener l) {
		listeners.add(l);
	}
	
	public void removeListener(AnalogControlListener l) {
		listeners.remove(l);
	}
	
	//end of public interface
	
	
	private void init()	{
		setFocusable(true);

		outerBorderPaint = new Paint();
		outerBorderPaint.setAntiAlias(true); 
		outerBorderPaint.setColor(0xFFAAAAAA);
		outerBorderPaint.setStyle(Style.STROKE);
		outerBorderPaint.setStrokeWidth(2.0f);
		
		dotPaint = new Paint();
		dotPaint.setAntiAlias(true);
		dotPaint.setColor(0xFFDDDDDD);
		//for debug-lines:
		dotPaint.setStyle(Style.FILL);
		dotPaint.setTextSize(15);
		
		outerBorderPath = new Path(); //initialized in onSizeChanged
		listeners = new HashSet<AnalogControlListener>();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		//TODO get 5px offset to draw ball outside border
		super.onDraw(canvas);
		//canvas.drawCircle(circleCenterX, circleCenterY, controlSize, outerBorderPaint);
		//canvas.drawPath(outerBorderPath, outerBorderPaint);
		
		canvas.drawLine(dotPositionX+controlCenterX, dotPositionY+controlCenterY, controlCenterX, controlCenterY, outerBorderPaint);
		canvas.drawCircle(dotPositionX+controlCenterX, dotPositionY+controlCenterY, DOTRADIUS, dotPaint);
		
		//String debug = String.format("angle: %1.3f", currentAngle);
		//String debug2 = String.format("strength: %1.3f", currentStrength);
		//canvas.drawText(debug, 0, 10, dotPaint);
		//canvas.drawText(debug2, 0, 25, dotPaint);
	}
	
	private void updateDotPosition(float touchX, float touchY)	{
		float strengthMax = controlSize; //maximum length of the 'line'
		
		float relativeX = touchX - controlCenterX;
		float relativeY = touchY - controlCenterY;
		
		double angle = Math.atan2(relativeY, relativeX); //angle of the line in radials
		double length = relativeX / Math.cos(angle); //length of 'line' between circle center and touched position
		//strength will always be a value between 0 and 1, with 1 == maximum
		double strength;
		//TODO limit to the diamond-shape instead of circle
		//store X,Y position to render the center dot		
		if (length > strengthMax) {
			strength = 1;
			//avoid expensive tangens, cosine etc
			double reduction = length/strengthMax;
			dotPositionX = (int) (Math.round(relativeX / reduction));
			dotPositionY = (int) (Math.round(relativeY / reduction));
		}
		else {
			strength = length/strengthMax;
			dotPositionX = (int) relativeX;
			dotPositionY = (int) relativeY;
		}
		assert(Math.abs(dotPositionX - controlCenterX) < controlSize);
		assert(Math.abs(dotPositionY - controlCenterY) < controlSize);
		
		
		if (angle != currentAngle || strength != currentStrength) {
			currentAngle = angle;
			currentStrength = strength;
			updateListeners();
		}
	}
	
	private void resetDotPosition() {
		currentAngle = 0;
		currentStrength = 0;
		dotPositionX = 0;
		dotPositionY = 0;
		updateListeners();
	}
	
	private void updateListeners() {
		for (AnalogControlListener l : listeners) {
			l.onPositionChanged(dotPositionX/(double)controlSize, dotPositionY/(double)controlSize);
		}
	}
	
	@Override
	protected void onSizeChanged(int width, int height, int oldw, int oldh) {
		super.onSizeChanged(width, height, oldw, oldh);

		controlCenterX = width/2;
		controlCenterY = height/2;
		controlSize = Math.min(controlCenterX, controlCenterY) - DOTRADIUS; //prevent from touching edge
		
		outerBorderPath.reset();
		//set path to a diamond shape, start at north
		outerBorderPath.moveTo(controlCenterX , controlCenterY + controlSize);
		outerBorderPath.lineTo(controlCenterX + controlSize, controlCenterY);
		outerBorderPath.lineTo(controlCenterX, controlCenterY - controlSize);
		outerBorderPath.lineTo(controlCenterX - controlSize, controlCenterY);
		outerBorderPath.close();

		resetDotPosition();
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		//return super.onTouchEvent(event);
		switch (ev.getAction()) {
		case MotionEvent.ACTION_MOVE:
		case MotionEvent.ACTION_DOWN:
			updateDotPosition(ev.getX(), ev.getY());
			invalidate();
			return true;
		case MotionEvent.ACTION_UP:
			resetDotPosition();
			invalidate();
			return true;
		default:
			return false;	
		}
	}
}
