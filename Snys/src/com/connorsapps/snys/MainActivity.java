package com.connorsapps.snys;

import java.io.IOException;
import java.util.Arrays;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import com.connorsapps.snys.SnysContract.Account;


public class MainActivity extends ActionBarActivity 
{
	private NetworkManager netMan;
	private SQLiteDatabase db;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		//Progress
		this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.activity_main);
		
		this.netMan = new NetworkManager();
		
		//Open database in background
		SnysDbHelper helper = new SnysDbHelper(this.getApplicationContext());
		OpenDbTask opener = new OpenDbTask(this);
		opener.execute(helper);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_logout) 
		{
			logout();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void logout()
	{
		//TODO move to background?
		db.delete(Account.TABLE_NAME, null, null);
		this.netMan.setCredentials(null);
		tryLogin();
	}

	/**
	 * Callback method for database opened
	 * @param db
	 */
	public void onDatabaseOpened(SQLiteDatabase db)
	{
		//Set database
		this.db = db;
		
		tryLogin();
	}
	
	/**
	 * Try to log in with given credentials
	 */
	public void tryLogin()
	{
		//Start the login task
		LoginTask login = new LoginTask(this);
		login.execute();
	}
	
	/**
	 * Callback method for missing save data
	 */
	public void onNoSave()
	{
		LoginDialogFragment frag = new LoginDialogFragment(this, "Please log in.");
		frag.show(getSupportFragmentManager(), "LoginDialog");
	}
	
	/**
	 * Callback for when server couldn't be reached.
	 */
	public void onNoConnection()
	{
		new GenericDialogFragment("Couldn't connect!",
				"This could be a connection or server issue. You can still access saved data!",
				android.R.drawable.ic_dialog_alert,
				"Damn it").show(getSupportFragmentManager(), "DamnFragment");
	}
	
	/**
	 * Callback method for invalid save data
	 */
	public void onInvalidSave()
	{
		LoginDialogFragment frag = new LoginDialogFragment(this, "Bad credentials. Please try again.");
		frag.show(getSupportFragmentManager(), "LoginDialog");
	}
	
	/**
	 * Callback method for successful login
	 */
	public void onSuccessfulLogin()
	{
		//Run tests, for now
		new Thread(new Runnable() {
			public void run()
			{
				try
				{
					Log.d("devBug", netMan.getInfo().toString());
					Log.d("devBug", Arrays.toString(netMan.getNotifications()));
					Log.d("devBug", Arrays.toString(netMan.getGroups()));
					Log.d("devBug", Arrays.toString(netMan.getInvitations()));
				} 
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}).start();
		
	}
	
	/**
	 * Callback method for dialog login
	 * @param email
	 * @param password
	 */
	public void onUpdateCredentials(final String email, final String password)
	{
		//Start and update database task with the new credentials
		new AsyncTask<Boolean, Boolean, Boolean> ()
		{
			@Override
			protected Boolean doInBackground(Boolean... vals)
			{
				//Update credentials in database
				SQLiteDatabase db = MainActivity.this.getDatabase();
			
				//Values to insert
				ContentValues values = new ContentValues();
				values.put(Account._ID, 1);
				values.put(Account.COLUMN_EMAIL, email);
				values.put(Account.COLUMN_PASS, password);
				
				//Run insert
				db.insert(Account.TABLE_NAME, null, values);
				
				return true;
			}
			
			@Override
			protected void onPostExecute(Boolean result)
			{
				//Start a new login task (from main thread, for safety)
				LoginTask task = new LoginTask(MainActivity.this);
				task.execute();
			}
			
		}.execute();
	}

	public NetworkManager getNetworkManager()
	{
		return netMan;
	}
	
	public SQLiteDatabase getDatabase()
	{
		return db;
	}
}
