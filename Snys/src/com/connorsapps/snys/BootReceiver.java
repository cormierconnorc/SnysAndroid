package com.connorsapps.snys;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(Context cont, Intent intent)
	{
		if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
		{
			//Start the Sync Adapter, which will handle everything else
			MainActivity.startSyncAdapter(cont);
		}
	}

}
