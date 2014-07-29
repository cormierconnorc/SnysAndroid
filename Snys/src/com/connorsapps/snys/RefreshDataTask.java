package com.connorsapps.snys;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.os.AsyncTask;

public class RefreshDataTask extends AsyncTask<Void, Void, Boolean>
{
	private Callback callback;
	private NetworkManager netMan;
	private DatabaseClient db;
	private String error;
	
	public RefreshDataTask(Callback callback, NetworkManager netMan, DatabaseClient db)
	{
		this.callback = callback;
		this.netMan = netMan;
		this.db = db;
	}
	
	@Override
	protected void onPreExecute()
	{
		callback.startProgress();
	}
	
	@Override
	protected Boolean doInBackground(Void... params)
	{
		try
		{
			//Get information off of server
			NetworkManager.Information info = netMan.getInfo();
			
			//Get old handled note list
			List<Notification> lameDuckNotes = db.getHandledNotifications();
			//Remove all notifications that are still handled
			lameDuckNotes.removeAll(info.handled);
			
			//Now remove the alarms associated with the remaining notifications (which no longer exist)
			AlarmService.removeAlarms(callback.getContext(), lameDuckNotes);
			
			//Add new alarms
			AlarmService.addAlarms(callback.getContext(), info.handled, info.memberships);
			
			//Now put it into the database
			//Note: ORDER DOES MATTER HERE. Memberships
			//must be added before invitations to ensure
			//that invitation to a group the user is already
			//in are disregarded. This oddity is due to the
			//difference in the way data is stored locally
			//and on the server.
			
			//To this end, append invitations to memberships and pending to handled
			info.memberships.addAll(info.invitations);
			info.handled.addAll(info.pending);
			
			db.updateGroups(info.memberships);
			db.updateNotifications(info.handled);
			
			//Indicate success
			return true;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			
			error = "Could not reach server!";
			
			return false;
		}
	}
	
	@Override
	protected void onPostExecute(Boolean result)
	{
		callback.endProgress();
		
		if (result)
			callback.onSuccessfulRefresh();
		else
			callback.onUnsuccessfulRefresh(error);
	}
	
	public static interface Callback extends ProgressCallback
	{
		public Context getContext();
		public void onSuccessfulRefresh();
		public void onUnsuccessfulRefresh(String error);
	}
}
