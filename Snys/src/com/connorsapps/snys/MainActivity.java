package com.connorsapps.snys;

import java.io.IOException;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;


public class MainActivity extends ActionBarActivity implements LoginTask.LoginCallback, OpenDbTask.DbCallback, RefreshDataTask.Callback
{
	public static final String REFRESH_NETWORK_ON_START_KEY = "com.connorsapps.snys.MainActivity.refreshNetworkOnStart";
	public static final String SHOW_NOTIFICATIONS_ON_START_KEY = "com.connorsapps.snys.MainActivity.showNotificationsOnStart";
	public static String AUTHORITY = "com.connorsapps.snys.provider";
	public static String ACCOUNT_TYPE = "raspbi.mooo.com/snys";
	public static String ACCOUNT = "dummyaccount";
	public static long SYNC_REFRESH_PERIOD = 60; //30 * 60;	//Sync in background every 30 minutes.
	public static long WAIT_TIME = 10;
	private static Account account;
	private static NetworkManager netMan;
	private static DatabaseClient db;
	private boolean refreshNetworkOnStart, showNotificationsOnStart;
	private ProgressFragment curProgFrag;
	private NoteFragment noteFrag;
	private GroupFragment groupFrag;
	private MenuItem toggleOnBoot;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{		
		super.onCreate(savedInstanceState);
		
		//Just a fragment container
		setContentView(R.layout.activity_main);
		
		//Get the argument passed to this activity: should the app sync on start?
		//If not passed in (user started app manually), set to true.
		this.refreshNetworkOnStart = this.getIntent().getBooleanExtra(REFRESH_NETWORK_ON_START_KEY, true);
		//Show groups on start by default. Notifications if started from notification
		this.showNotificationsOnStart = this.getIntent().getBooleanExtra(SHOW_NOTIFICATIONS_ON_START_KEY, false);
		
		//Set up dropdown navigation
		setupDropdown();
		
		netMan = new NetworkManager(getApplicationContext());
		
		//Create progress fragment which, unlike other fragments
		//does not depend on having an open database
		curProgFrag = new ProgressFragment();
		
		//Open database in background
		SnysDbHelper helper = new SnysDbHelper(this.getApplicationContext());
		OpenDbTask opener = new OpenDbTask(this);
		opener.execute(helper);
		
		startSyncAdapter(this);
	}
	
	public static void startSyncAdapter(Context cont)
	{
		//Set up the SyncAdapter
		account = createSyncAccount(cont);

		ContentResolver.setIsSyncable(account, AUTHORITY, 1);
		ContentResolver.setSyncAutomatically(account, AUTHORITY, true);

		// Now start it
		ContentResolver.addPeriodicSync(account, AUTHORITY, Bundle.EMPTY,
				SYNC_REFRESH_PERIOD);
	}
	
	public static Account createSyncAccount(Context context)
	{
		//Create the dummy account
		Account nAc = new Account(ACCOUNT, ACCOUNT_TYPE);
		
		//Get the account manager
		AccountManager am = (AccountManager)context.getSystemService(ACCOUNT_SERVICE);
		
		//Add the dummy account
		am.addAccountExplicitly(nAc, null, null);
		
		return nAc;
	}
	
	public void setupDropdown()
	{
		ActionBar action = this.getSupportActionBar();
		action.setDisplayShowTitleEnabled(false);
		action.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		
		//Set up dropdown options
		String[] options = {"Groups", "Notifications"};
		ArrayAdapter<String> ada = new ArrayAdapter<String>(action.getThemedContext(), 
				android.R.layout.simple_list_item_1, 
				android.R.id.text1, 
				options);

		//Set adapter and callback
		action.setListNavigationCallbacks(ada, new ActionBar.OnNavigationListener(){

			@Override
			public boolean onNavigationItemSelected(int pos, long id)
			{
				if (pos == 0)
					showGroups();
				else
					showNotifications();
				
				return true;
			}
			
		});
		
		//Set selected on start.
		action.setSelectedNavigationItem(this.showNotificationsOnStart ? 1 : 0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		
		toggleOnBoot = menu.findItem(R.id.action_toggle_start_on_boot);
		
		this.setBootItemText();
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId())
		{
		case R.id.action_logout: 
			logout();
			return true;
		case R.id.action_submit:
			this.onCreateNewNotification();
			return true;
		case R.id.action_quit:
			this.exitAll();
			return true;
		case R.id.action_toggle_start_on_boot:
			this.toggleStartOnBoot();
			return true;
		case R.id.action_refresh:
			this.onRefresh();
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
				
				//Delete database cache
				db.deleteGroups();
				db.deleteNotifications();
				
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
	 * Close all of this program
	 */
	public void exitAll()
	{
		//Stop sync adapter
		ContentResolver.removePeriodicSync(account, AUTHORITY, new Bundle());
		
		//Shut down activity
		this.finish();
	}
	
	private boolean isOnBootEnabled()
	{
		ComponentName receiver = new ComponentName(this, BootReceiver.class);
		PackageManager pm = this.getPackageManager();
		
		return isEnabled(receiver, pm);
	}
	
	private boolean isEnabled(ComponentName receiver, PackageManager pm)
	{
		int isEnabled = pm.getComponentEnabledSetting(receiver);
		
		return isEnabled == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
	}
	
	/**
	 * Toggle boot receiver enabled
	 * @param item
	 */
	public void toggleStartOnBoot()
	{
		ComponentName receiver = new ComponentName(this, BootReceiver.class);
		PackageManager pm = this.getPackageManager();
		
		boolean enabled = isEnabled(receiver, pm);

		pm.setComponentEnabledSetting(receiver, 
				(enabled ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED), 
				PackageManager.DONT_KILL_APP);
		
		setBootItemText(!enabled);
	}
	
	private void setBootItemText()
	{
		this.setBootItemText(isOnBootEnabled());
	}
	
	private void setBootItemText(boolean isOnBootEnabled)
	{
		if (isOnBootEnabled)
			toggleOnBoot.setTitle("Disable sync on boot");
		else
			toggleOnBoot.setTitle("Enable sync on boot");
	}
	
	/**
	 * Prompt the user to select a group to submit to,
	 * creating a dialog that will transition to the
	 * note activity
	 */
	public void onCreateNewNotification()
	{
		new AsyncTask<Void, Void, List<Group>>()
		{
			@Override
			protected List<Group> doInBackground(Void... arg0)
			{
				//Get the relevant groups out of the database
				return db.getSubmittableGroups();
			}
			
			@Override
			protected void onPostExecute(List<Group> relevant)
			{
				new SubmitToDialogFragment(relevant).show(getSupportFragmentManager(), "GroupPrompt");
			}
			
		}.execute();
	}
	
	@Override
	public void startProgress()
	{
		if (!curProgFrag.isAdded())
			this.getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, curProgFrag).commit();
	}
	
	@Override
	public void endProgress()
	{
		this.getSupportFragmentManager().beginTransaction().remove(curProgFrag).commit();
	}

	@Override
	public void onDatabaseOpened(SQLiteDatabase db)
	{
		//Set database
		MainActivity.db = new DatabaseClient(db);
		
		//Fragments depend on open db, so only create them here
		createFragments();
		
		//Only do this if not started from notification:
		if (this.refreshNetworkOnStart)
			tryLogin();
		else
		{
			//Set network manager credentials from db and move on
			new AsyncTask<Void, Void, Void>()
			{
				@Override
				protected Void doInBackground(Void... unused)
				{
					netMan.setCredentials(MainActivity.db.getCredentials());
					
					return null;
				}
				
				@Override
				protected void onPostExecute(Void res)
				{
					onRefresh();
				}
			}.execute();
		}
	}
	
	public void createFragments()
	{
		//Create fragments dependent on open database
		noteFrag = new NoteFragment();
		//Empty bundle for arguments
		Bundle args = new Bundle();
		noteFrag.setArguments(args);

		groupFrag = new GroupFragment();
	}
	
	/**
	 * Try to log in with given credentials
	 */
	public void tryLogin()
	{
		//Start the login task
		LoginTask login = new LoginTask(this, netMan, db);
		login.execute();
	}
	
	@Override
	public void onNoSave()
	{
		LoginDialogFragment frag = new LoginDialogFragment(this, "Please log in.");
		frag.show(getSupportFragmentManager(), "LoginDialog");
	}
	
	@Override
	public void onNoConnection()
	{
		new GenericDialogFragment("Couldn't connect!",
				"This could be a connection or server issue. You can still access saved data!",
				android.R.drawable.ic_dialog_alert,
				"Damn it") {
			public void onCancel(DialogInterface dial)
			{
				//Transition anyway with no-connection note.
				//Will load whatever's in the database
				showSelected();
			}
		}.show(getSupportFragmentManager(), "DamnFragment");
	}
	
	@Override
	public void onInvalidSave()
	{
		LoginDialogFragment frag = new LoginDialogFragment(this, "Bad credentials. Please try again.");
		frag.show(getSupportFragmentManager(), "LoginDialog");
	}
	
	@Override
	public void onSuccessfulLogin()
	{
		//Task to load from server and then show
		new RefreshDataTask(this, netMan, db).execute();
	}
	
	public void onSuccessfulRefresh()
	{		
		showSelected();
		
		//Now force the visible fragment to reload
		refreshSelected();
	}
	
	public void onUnsuccessfulRefresh(String error)
	{
		new GenericDialogFragment("Error loading data", error, android.R.drawable.ic_dialog_alert, null) 
		{
			@Override
			public void onCancel(DialogInterface dial)
			{
				onSuccessfulRefresh();
			}
		}.show(getSupportFragmentManager(), "BadLoad");
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
				LoginTask task = new LoginTask(MainActivity.this, netMan, db);
				task.execute();
			}
			
		}.execute();
	}
	
	/**
	 * Register account
	 * @param email
	 * @param pass
	 */
	public void onRegister(final String email, final String pass)
	{
		new AsyncTask<Void, Void, Boolean> ()
		{
			private String error;
			private boolean cantConnect;
			
			@Override
			protected Boolean doInBackground(Void... arg0)
			{
				try
				{
					Credentials cred = netMan.register(email, pass);
					netMan.setCredentials(cred);
					db.updateCredentials(email, pass);
					return true;
				} 
				catch (IOException e)
				{
					error = "Could not connect to server";
					cantConnect = true;
					e.printStackTrace();
				} 
				catch (SnysException e)
				{
					error = e.getMessage();
				}
				
				return false;
			}
			
			@Override
			protected void onPostExecute(Boolean result)
			{
				//On failure to register
				if (!result)
				{
					if (cantConnect)
						onNoConnection();
					else
					{
						//Create a new alert that goes to try login upon cancellation
						new GenericDialogFragment("Failed to register!",
								error,
								android.R.drawable.ic_dialog_alert,
								null) {
							@Override
							public void onCancel(DialogInterface dialog)
							{
								tryLogin();
							}
						}.show(getSupportFragmentManager(), "FailedRegisterDialog");
					}
				}
			}
			
		}.execute();
	}
	
	public void onRefresh()
	{
		this.onSuccessfulLogin();
	}
	
	/**
	 * Create a registration dialog for the user to interact with
	 */
	public void createRegistrationDialog()
	{
		new RegisterDialogFragment(this).show(getSupportFragmentManager(), "RegistrationDialog");
	}
	
	/**
	 * Show appropriate fragment selected in spinner
	 */
	public void showSelected()
	{
		ActionBar action = this.getSupportActionBar();
		switch (action.getSelectedNavigationIndex())
		{
		case 0:
			showGroups();
			break;
		case 1:
			showNotifications();
			break;
		}
	}
	
	public void refreshSelected()
	{
		ActionBar action = this.getSupportActionBar();
		switch (action.getSelectedNavigationIndex())
		{
		case 0:
			groupFrag.loadData();
			break;
		case 1:
			noteFrag.loadData();
			break;
		}
	}
	
	public void showGroups()
	{
		if (groupFrag == null)
			return;
		
		//Create fragment and add
		getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, groupFrag).commit();
	}
	
	public void showNotifications()
	{	
		if (noteFrag == null)
			return;
		
		getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, noteFrag).commit();
	}

	/**
	 * Get the network manager for this application
	 * @return
	 */
	public static NetworkManager getNetworkManager()
	{
		//Block while network manager is null
		while (netMan == null)
		{
			threadWait();
		}
		
		return netMan;
	}
	
	/**
	 * Get the databse for this application
	 * @return
	 */
	public static DatabaseClient getDatabase()
	{
		//Block while database is null
		while (db == null)
		{
			threadWait();
		}
		
		return db;
	}
	
	public static void threadWait()
	{
		threadWait(WAIT_TIME);
	}
	
	public static void threadWait(long time)
	{
		try
		{
			Thread.sleep(time);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public Context getContext()
	{
		return this.getApplicationContext();
	}
}
