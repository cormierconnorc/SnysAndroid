package com.connorsapps.snys;

import java.io.IOException;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.connorsapps.snys.SnysContract.Account;


public class LoginTask extends AsyncTask<Boolean, Boolean, Boolean>
{
	private MainActivity callback;
	private NetworkManager man;
	private SQLiteDatabase db;
	
	//Flags
	private boolean hasSave, isValidSave;
	
	public LoginTask(MainActivity callback)
	{
		this.callback = callback;
		this.man = callback.getNetworkManager();
		this.db = callback.getDatabase();
	}
	
	@Override
	protected Boolean doInBackground(Boolean... arg0)
	{
		String[] projection = {
			Account.COLUMN_EMAIL,
			Account.COLUMN_PASS
		};
		
		Cursor res = db.query(Account.TABLE_NAME, projection, null, null, null, null, null);
		
		//If cursor is empty (no account info!)
		if (res.getCount() == 0)
		{
			this.onProgressUpdate();
			res.close();
			return false;
		}
		
		//If account info does exist, set flag
		this.hasSave = true;
		
		//Now get it
		res.moveToFirst();
		String email = res.getString(res.getColumnIndexOrThrow(Account.COLUMN_EMAIL));
		String pass = res.getString(res.getColumnIndexOrThrow(Account.COLUMN_PASS));
		res.close();
		
		//Set network manager's credentials
		man.setCredentials(new NetworkManager.Credentials(email, pass));
		
		try
		{
			this.isValidSave = man.checkValid();
			this.onProgressUpdate();
			return this.isValidSave;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	@Override
	protected void onProgressUpdate(Boolean... unused)
	{
		if (!this.hasSave)
		{
			callback.onNoSave();
		}
		else if (!this.isValidSave)
		{
			callback.onInvalidSave();
		}
		else
		{
			callback.onSuccessfulLogin();
		}
	}

}
