package com.connorsapps.snys;

import java.io.IOException;

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
			
			//Clear out the old stuff (since this is a full update)
			//This is inefficient, of course, but the server currently
			//lacks the ability to send "update chunks" or some similar shit.
			db.deleteGroups();
			db.deleteNotifications();
			
			//Now put it into the database
			//Note: ORDER DOES MATTER HERE. Memberships
			//must be added before invitations to ensure
			//that invitation to a group the user is already
			//in are disregarded. This oddity is due to the
			//difference in the way data is stored locally
			//and on the server.
			db.insertGroups(info.memberships);
			db.insertInvitations(info.invitations);
			db.insertNotifications(info.handled);
			db.insertNotifications(info.pending);
			
			//Indicate success
			return true;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			
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
		public void onSuccessfulRefresh();
		public void onUnsuccessfulRefresh(String error);
	}
}
