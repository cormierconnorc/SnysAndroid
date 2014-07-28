package com.connorsapps.snys;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

public class SyncAdapter extends AbstractThreadedSyncAdapter implements RefreshDataTask.Callback
{
	private DatabaseClient db;
	private NetworkManager netMan;
	
	public SyncAdapter(Context context, boolean autoInitialize)
	{
		super(context, autoInitialize);
		init();
	}
	
	public SyncAdapter(Context context, boolean autoInitialize,
			boolean allowParallelSyncs)
	{
		super(context, autoInitialize, allowParallelSyncs);
		init();
	}
	public void init()
	{
		Log.d("devBug", "Initializing syncer");
		db = new DatabaseClient(new SnysDbHelper(getContext()).getWritableDatabase());
		netMan = new NetworkManager();
	}
	
	@Override
	public void onPerformSync(Account arg0, Bundle arg1, String arg2,
			ContentProviderClient arg3, SyncResult arg4)
	{
		//Set network manager credentials (done each time in the event of user logout and switch)
		Credentials cred = db.getCredentials();
		
		//No sync if credentials are null!
		if (cred == null)
		{
			Log.e("devBug", "No credentials for background sync!");
			return;
		}
		
		netMan.setCredentials(cred);
		
		//Sync otherwise
		new RefreshDataTask(this, netMan, db).execute();
	}

	@Override
	public void startProgress()
	{
		// Do nothing
	}

	@Override
	public void endProgress()
	{
		// Do nothing
	}

	@Override
	public void onSuccessfulRefresh()
	{
		// TODO Create notification for pending notifications
		Log.d("devBug", "Successful background sync.");
	}

	@Override
	public void onUnsuccessfulRefresh(String error)
	{
		// Do nothing but log error
		Log.e("devBug", "Error in background sync task: " + error);
	}

}
