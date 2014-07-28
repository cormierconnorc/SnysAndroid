package com.connorsapps.snys;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class StubAuthenticatorService extends Service
{
	private StubAuthenticator authy;
	
	@Override
	public void onCreate()
	{
		authy = new StubAuthenticator(this);
	}

	@Override
	public IBinder onBind(Intent arg0)
	{
		return authy.getIBinder();
	}

}
