package com.connorsapps.snys;

import java.util.List;

import android.accounts.Account;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

public class SyncAdapter extends AbstractThreadedSyncAdapter implements RefreshDataTask.Callback
{
	public static final int NOTE_ID = 1298037;
	private DatabaseClient db;
	private NetworkManager netMan;
	private NotificationManager noteMan;
	
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
		noteMan = (NotificationManager)getContext().getSystemService(Context.NOTIFICATION_SERVICE);
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
		Log.d("devBug", "Successful background sync.");
		
		//Get new unhandled notifications and add to database so they don't show up again
		List<Notification> pending = db.getNewUnhandledNotifications();
		
		Log.d("devBug", "Prompting for " + pending.size() + " new notes.");
		
		if (pending.size() != 0)
			createPendingNotification(pending);
	}

	@Override
	public void onUnsuccessfulRefresh(String error)
	{
		// Do nothing but log error
		Log.e("devBug", "Error in background sync task: " + error);
	}
	
	public void createPendingNotification(List<Notification> pending)
	{
		NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext());
		
		builder.setContentTitle(String.valueOf(pending.size()) + " pending Snys notification" + (pending.size() == 1 ? "." : "s."));
		builder.setSmallIcon(R.drawable.ic_launcher);
		builder.setContentText("Click here to handle.");
		builder.setAutoCancel(true);
		
		Intent openAct = new Intent(getContext(), MainActivity.class);
		openAct.putExtra(MainActivity.REFRESH_NETWORK_ON_START_KEY, false);
		openAct.putExtra(MainActivity.SHOW_NOTIFICATIONS_ON_START_KEY, true);
		
		TaskStackBuilder tsk = TaskStackBuilder.create(getContext());
		
		//Add activity to artificial back stack
		tsk.addNextIntent(openAct);
		
		PendingIntent result = tsk.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		
		builder.setContentIntent(result);
		
		noteMan.notify(NOTE_ID, builder.build());
	}

}
