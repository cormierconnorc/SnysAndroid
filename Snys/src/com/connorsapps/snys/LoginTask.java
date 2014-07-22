package com.connorsapps.snys;

import java.io.IOException;

import android.os.AsyncTask;


public class LoginTask extends AsyncTask<Void, Void, Boolean>
{
	private MainActivity callback;
	private NetworkManager man;
	private DatabaseClient db;
	
	//Flags
	private boolean hasSave, isNoConnect;
	
	public LoginTask(MainActivity callback)
	{
		this.callback = callback;
		this.man = callback.getNetworkManager();
		this.db = callback.getDatabase();
	}
	
	@Override
	protected void onPreExecute()
	{
		callback.setProgressBarIndeterminateVisibility(true);
	}
	
	@Override
	protected Boolean doInBackground(Void... arg0)
	{
		Credentials creds = db.getCredentials();
		
		if (creds == null)
			return false;
		
		//If account info does exist, set flag
		this.hasSave = true;
		
		//Set network manager's credentials
		man.setCredentials(creds);
		
		try
		{
			return man.checkValid();
		}
		catch (IOException e)
		{
			this.isNoConnect = true;
			e.printStackTrace();
			return false;
		}
	}
	
	@Override
	protected void onPostExecute(Boolean result)
	{		
		callback.setProgressBarIndeterminateVisibility(false);
		
		if (!this.hasSave)
		{
			callback.onNoSave();
		}
		else if (this.isNoConnect)
		{
			callback.onNoConnection();
		}
		else if (!result)
		{
			callback.onInvalidSave();
		}
		else
		{
			callback.onSuccessfulLogin();
		}
	}

}
