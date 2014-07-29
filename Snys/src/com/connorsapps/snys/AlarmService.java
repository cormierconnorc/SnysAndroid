package com.connorsapps.snys;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

public class AlarmService extends IntentService
{
	private static final long[] vibratePattern = {0, 300, 200, 300, 600, 300, 200, 300, 600, 300, 200, 300};
	private static AlarmManager man;

	public AlarmService()
	{
		super("AlarmService");
	}
	
	@Override
	protected void onHandleIntent(Intent intent)
	{
		//Get the extras passed into this service
		Notification note = (Notification)intent.getExtras().get(NoteActivity.NOTE_KEY);
		Group group = (Group)intent.getExtras().get(NoteActivity.GROUP_KEY);
		
		//Start vibrating
		startVibrate();
		
		//Create a notification with intent of noteactivity
		createNotification(note, group);
	}
	
	public void startVibrate()
	{
		Vibrator v = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
		
		v.vibrate(vibratePattern, -1);
	}
	
	public void createNotification(Notification note, Group group)
	{
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		
		builder.setContentTitle("Snys reminder!");
		builder.setSmallIcon(R.drawable.ic_launcher);
		builder.setContentText(note.getText());
		builder.setAutoCancel(true);
		
		Intent openAct = new Intent(this, MainActivity.class);
		Intent noteAct = new Intent(this, NoteActivity.class);
		noteAct.putExtra(NoteActivity.NOTE_KEY, note);
		noteAct.putExtra(NoteActivity.GROUP_KEY, group);
		
		TaskStackBuilder tsk = TaskStackBuilder.create(this);
		
		//Add activity to artificial back stack
		tsk.addNextIntent(openAct);
		tsk.addNextIntent(noteAct);
		
		PendingIntent result = tsk.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		
		builder.setContentIntent(result);
		
		NotificationManager noteMan = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
		
		//Create notification with random id
		noteMan.notify((int)(Math.random() * Integer.MAX_VALUE), builder.build());
	}
	
	private static boolean needsAlarm(Notification note)
	{
		if (note.getStatus() != Notification.Status.ALARM && note.getStatus() != Notification.Status.ALL ||
				note.getRemindAt() < System.currentTimeMillis())
			return false;
		return true;
	}
	
	/**
	 * Add all alarms from a list
	 * @param notes
	 * @param groups
	 */
	public static void addAlarms(Context cont, List<Notification> notes, List<Group> groups)
	{
		for (Notification note : notes)
		{
			//Find the proper group for this note
			Group group = null;
			for (Group g : groups)
				if (g.getId() == note.getGid())
				{
					group = g;
					break;
				}
			
			if (group == null)
				continue;
			
			addAlarm(cont, note, group);
		}
	}
	
	/**
	 * Remove the alarms associated with each notification in a list
	 * @param cont
	 * @param notes
	 */
	public static void removeAlarms(Context cont, List<Notification> notes)
	{
		for (Notification note : notes)
			removeAlarm(cont, note);
	}
	
	private static PendingIntent getAlarmIntent(Context cont, Notification note, Group group)
	{
		//Set up the intent
		Intent alarm = new Intent(cont, AlarmService.class);
		alarm.putExtra(NoteActivity.NOTE_KEY, note);
		
		if (group != null)
			alarm.putExtra(NoteActivity.GROUP_KEY, group);

		return PendingIntent.getService(cont, note.getId(), alarm, PendingIntent.FLAG_ONE_SHOT);
	}
	
	/**
	 * Add a single alarm to the alarm manager
	 * @param note
	 * @param group
	 */
	public static void addAlarm(Context cont, Notification note, Group group)
	{
		Log.d("devBug", "Trying to add alarm where note = " + note + " and group = " + group);
		
		//No need to add alarm if not the right type of note
		if (!needsAlarm(note))
		{
			//Remove it if it exists
			removeAlarm(cont, note);
			return;
		}
		
		//Make sure alarm manager is non-null
		if (man == null)
			man = (AlarmManager)cont.getSystemService(Context.ALARM_SERVICE);
		
		PendingIntent alarmService = getAlarmIntent(cont, note, group);
		
		//Cancel existing alarms associated with this intent
		man.cancel(alarmService);
	
		try
		{
			Method window = AlarmManager.class.getMethod("setWindow", int.class, long.class, long.class, PendingIntent.class);
			
			window.invoke(man, AlarmManager.RTC_WAKEUP, note.getRemindAt(), 30 * 1000, alarmService);
			
			Log.d("devBug", "Successfully invoked create on new device");
		}
		catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			Log.d("devBug", "Failed to invoke create and fell back to older version");
			//Older device
			man.set(AlarmManager.RTC_WAKEUP, note.getRemindAt(), alarmService);
		}
		
	}
	
	/**
	 * Remove the alarm associated with a single notification
	 * @param cont
	 * @param note
	 */
	public static void removeAlarm(Context cont, Notification note)
	{
		if (man == null)
			man = (AlarmManager)cont.getSystemService(Context.ALARM_SERVICE);
		
		PendingIntent alarmService = getAlarmIntent(cont, note, null);
		
		man.cancel(alarmService);
	}

}
