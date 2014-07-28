package com.connorsapps.snys;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class SyncService extends Service
{
	private static SyncAdapter ada;
	private static final Object syncAdapterThreadLocker = new Object();
	
	@Override
	public void onCreate()
	{
		synchronized (syncAdapterThreadLocker)
		{
			if (ada == null)
			{
				ada = new SyncAdapter(getApplicationContext(), true);
			}
		}
	}
	
	@Override
	public IBinder onBind(Intent arg0)
	{
		return ada.getSyncAdapterBinder();
	}

}
