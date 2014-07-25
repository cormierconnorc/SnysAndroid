package com.connorsapps.snys;

import java.util.Calendar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;

public class DateTimeDialogFragment extends DialogFragment
{
	private Button toSet;
	private Calendar running;
	private Calendar starting;
	
	public DateTimeDialogFragment(Button toSet)
	{
		this.toSet = toSet;
		
		//Create the starting calendar from the button's text
		starting = Calendar.getInstance();
		
		long timeToSet = Notification.fromFormattedTime(toSet.getText().toString(), true);
		
		//If time is in past, start where we are now.
		if (timeToSet < System.currentTimeMillis())
			timeToSet = System.currentTimeMillis();
		
		starting.setTimeInMillis(timeToSet);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		running = Calendar.getInstance();
		running.clear();
		
		builder.setTitle("Select a date");
		
		int year = starting.get(Calendar.YEAR);
		int month = starting.get(Calendar.MONTH);
		int dayOfMonth = starting.get(Calendar.DAY_OF_MONTH);
		
		final DatePicker picker = new DatePicker(this.getActivity());
		picker.updateDate(year, month, dayOfMonth);
		
		builder.setView(picker);
		
		builder.setPositiveButton("Set", new DialogInterface.OnClickListener()
		{
			
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				//Set running values
				int year = picker.getYear();
				int month = picker.getMonth();
				int day = picker.getDayOfMonth();
				running.set(year, month, day);
				
				//Create the child dialog, the time picker
				new TimePickerFragment().show(getActivity().getSupportFragmentManager(), "TimeDialog");
			}
		});
		
		builder.setNegativeButton("Cancel", null);
		
		return builder.create();
	}
	
	private class TimePickerFragment extends DialogFragment
	{
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			
			builder.setTitle("Select a time");
			
			int hour = starting.get(Calendar.HOUR_OF_DAY);
			int minute = starting.get(Calendar.MINUTE);
			
			final TimePicker picker = new TimePicker(getActivity());
			picker.setIs24HourView(false);
			picker.setCurrentMinute(minute);
			picker.setCurrentHour(hour);
			
			builder.setView(picker);
			
			builder.setPositiveButton("Save", new DialogInterface.OnClickListener()
			{
				
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					int hour = picker.getCurrentHour();
					int minute = picker.getCurrentMinute();
					
					running.set(Calendar.HOUR_OF_DAY, hour);
					running.set(Calendar.MINUTE, minute);
					
					//Now get the time from the calendar
					long time = running.getTimeInMillis();
					
					//Now set the Button's text
					toSet.setText(Notification.getFormattedTime(time, true));
				}
			});
			
			builder.setNegativeButton("Cancel", null);
			
			return builder.create();
		}
	}
}
