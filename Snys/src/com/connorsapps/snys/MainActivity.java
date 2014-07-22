package com.connorsapps.snys;

import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;


public class MainActivity extends ActionBarActivity 
{
	private NetworkManager netMan;
	private DatabaseClient db;

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
		new AsyncTask<Void, Void, Void>()
		{

			@Override
			protected Void doInBackground(Void... arg0)
			{
				db.deleteCredentials();
				netMan.setCredentials(null);
				return null;
			}
			
			@Override
			protected void onPostExecute(Void res)
			{
				tryLogin();
			}
			
		}.execute();
	}

	/**
	 * Callback method for database opened
	 * @param db
	 */
	public void onDatabaseOpened(SQLiteDatabase db)
	{
		//Set database
		this.db = new DatabaseClient(db);
		
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
				db.updateCredentials(email, password);				
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
	
	public DatabaseClient getDatabase()
	{
		return db;
	}
}
