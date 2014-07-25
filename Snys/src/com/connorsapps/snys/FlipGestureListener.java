package com.connorsapps.snys;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ListView;
import android.widget.ViewFlipper;

public class FlipGestureListener extends SimpleOnGestureListener
{
	private static final int TYPE_STRING = 0; //In both relevant adapters.
	private ListView myList;
	private final Animation right_in, left_in, right_out, left_out;
	private final int minX, maxOffY;
	private final OnClickListener rowClickListener;
	
	public FlipGestureListener(ListView myList, DisplayMetrics dm, OnClickListener rowClickListener)
	{
		this.myList = myList;
		this.rowClickListener = rowClickListener;
		
		//Load animations
		left_in = AnimationUtils.loadAnimation(myList.getContext(), R.anim.slide_in_left);
		left_out = AnimationUtils.loadAnimation(myList.getContext(), R.anim.slide_out_left);
		right_out = AnimationUtils.loadAnimation(myList.getContext(), R.anim.slide_out_right);
		right_in = AnimationUtils.loadAnimation(myList.getContext(), R.anim.slide_in_right);
		
		//Val gets stuck at 80
		minX = (int)(40.0f * dm.densityDpi / 160.0f + 0.5f);
		maxOffY = (int)(250.0f * dm.densityDpi / 160.0f + 0.5f);
	}
	
	@Override
	public boolean onSingleTapUp(MotionEvent e)
	{
		int pos = myList.pointToPosition((int)e.getX(), (int)e.getY());
		
		if (pos < 0 || pos > myList.getCount())
			return false;
		
		//Return the result of the specified handler if it's a valid row
		return rowClickListener.onRowClick(pos);
	}
	
	@Override
	public boolean onFling(MotionEvent start, MotionEvent move, float velocityX, float velocityY)
	{
		Log.d("devBug", "Fling detected!");
		
		//Return false if you stray too far off the row
		if (Math.abs(move.getY() - start.getY()) > maxOffY)
			return false;
		
		int pos = myList.pointToPosition((int)start.getX(), (int)start.getY());
		
		if (pos < 0 || pos > myList.getCount())
			return false;
		
		//Return false if it's a title
		if (myList.getAdapter().getItemViewType(pos) == TYPE_STRING)
			return false;
		
		Log.d("devBug", "Things are good");
		
		ViewFlipper flip = (ViewFlipper)myList.getChildAt(pos);
		
		//No events on flipper currently executing previous command
		if (flip.isFlipping())
			return false;
		
		Log.d("devBug", "" + flip.getDisplayedChild());
		Log.d("devBug", "" + (move.getX() - start.getX()) + " compare with " + minX);
		
		if ((move.getX() - start.getX()) > minX )//&& flip.getDisplayedChild() == 1)
		{
			Log.d("devBug", "Flipping back");
			flip.setInAnimation(left_in);
			flip.setOutAnimation(right_out);
			flip.showPrevious();
			return true;
		}
		else if ((start.getX() - move.getX()) > minX )//&& flip.getDisplayedChild() == 0)
		{
			Log.d("devBug", "Flipping forward");
			flip.setInAnimation(right_in);
			flip.setOutAnimation(left_out);
			flip.showNext();
			return true;
		}
		
		return false;
	}
	
	public static interface OnClickListener
	{
		public boolean onRowClick(int row);
	}
}
