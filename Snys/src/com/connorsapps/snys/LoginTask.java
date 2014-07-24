package com.connorsapps.snys;

import java.io.IOException;

import android.os.AsyncTask;


public class LoginTask extends AsyncTask<Void, Void, Boolean>
{
	private LoginCallback callback;
	private NetworkManager man;
	private DatabaseClient db;
	
	//Flags
	private boolean hasSave, isNoConnect;
	
	public LoginTask(LoginCallback callback, NetworkManager man, DatabaseClient db)
	{
		this.callback = callback;
		this.man = man;
		this.db = db;
	}
	
	@Override
	protected void onPreExecute()
	{
		callback.startProgress();
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
		callback.endProgress();
		
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
	
	/**
	 * Callback for login task
	 * @author connor
	 *
	 */
	public static interface LoginCallback extends ProgressCallback
	{
		/**
		 * Callback method for missing save data
		 */
		public void onNoSave();
		
		/**
		 * Callback for when server couldn't be reached.
		 */
		public void onNoConnection();
		
		/**
		 * Callback method for invalid save data
		 */
		public void onInvalidSave();
		
		/**
		 * Callback method for successful login
		 */
		public void onSuccessfulLogin();
	}

}
