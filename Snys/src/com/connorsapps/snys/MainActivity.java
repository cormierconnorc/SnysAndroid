package com.connorsapps.snys;

import java.io.IOException;
import java.util.List;

import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;


public class MainActivity extends ActionBarActivity implements LoginTask.LoginCallback, OpenDbTask.DbCallback
{
	public static long WAIT_TIME = 10;
	private static NetworkManager netMan;
	private static DatabaseClient db;
//	private static List<Notification> uNoteCache, hNoteCache;
//	private static List<Group> groupCache, inviteCache;
	private ProgressFragment curProgFrag;
	private NoteFragment noteFrag;
	private GroupFragment groupFrag;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{		
		super.onCreate(savedInstanceState);
		
		//Just a fragment container
		setContentView(R.layout.activity_main);
		
		//Set up dropdown navigation
		setupDropdown();
		
		netMan = new NetworkManager();
		
		//Create progress fragment which, unlike other fragments
		//does not depend on having an open database
		curProgFrag = new ProgressFragment();
		
//		//Create cache lists (used to minimize database reads)
//		uNoteCache = new ArrayList<Notification>();
//		hNoteCache = new ArrayList<Notification>();
//		groupCache = new ArrayList<Group>();
//		inviteCache = new ArrayList<Group>();
		
		//Open database in background
		SnysDbHelper helper = new SnysDbHelper(this.getApplicationContext());
		OpenDbTask opener = new OpenDbTask(this);
		opener.execute(helper);
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
				
				return false;
			}
			
		});
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
		//TODO add actions for other menu items
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
		case R.id.action_refresh:
			this.onSuccessfulLogin();
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
		//Will also close background threads in future
		this.finish();
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
		
		tryLogin();
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
		new AsyncTask<Void, Void, Boolean>()
		{
			private String error;
			
			@Override
			protected Boolean doInBackground(Void... params)
			{
				try
				{
					MainActivity.this.startProgress();
					
					//Get information off of server
					NetworkManager.Information info = netMan.getInfo();
					
//					//Set up caches
//					uNoteCache.addAll(info.pending);
//					hNoteCache.addAll(info.handled);
//					groupCache.addAll(info.memberships);
//					inviteCache.addAll(info.invitations);
					
					//Clear out the old stuff (since this is a full update)
					//This is inefficient, of course, but the server currently
					//lacks the ability to send "update chunks" or some similar shit.
					db.deleteGroups();
					db.deleteNotifications();
					
					//Now put it into the database
					//Note: ORDER DOES MATTER HERE. Memberships
					//must be added before invitations to ensure
					//that invitation to a group the user is already
					//in are disregarded. This oddity is due to the
					//difference in the way data is stored locally
					//and on the server.
					db.insertGroups(info.memberships);
					db.insertInvitations(info.invitations);
					db.insertNotifications(info.handled);
					db.insertNotifications(info.pending);
					
					//Indicate success
					return true;
				}
				catch (IOException e)
				{
					e.printStackTrace();
					
					return false;
				}
			}
			
			@Override
			protected void onPostExecute(Boolean result)
			{
				MainActivity.this.endProgress();
				
				if (result)
				{
					showSelected();
				}
				else
				{
					new GenericDialogFragment("Error loading data", error, android.R.drawable.ic_dialog_alert, null) 
					{
						
						@Override
						public void onCancel(DialogInterface dial)
						{
							//Just load from database anyway
							showSelected();
						}
					}.show(getSupportFragmentManager(), "BadLoad");
				}
			}
		}.execute();
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
	
//	public static List<Group> getGroupCache()
//	{
//		return groupCache;
//	}
//	
//	public static List<Group> getInviteCache()
//	{
//		return inviteCache;
//	}
//	
//	public static List<Notification> getPendingCache()
//	{
//		return uNoteCache;
//	}
//	
//	public static List<Notification> getHandledCache()
//	{
//		return hNoteCache;
//	}
}
