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
	private NetworkManager netMan;
	private DatabaseClient db;
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
		
		this.netMan = new NetworkManager();
		
		//Open database in background
		SnysDbHelper helper = new SnysDbHelper(this.getApplicationContext());
		OpenDbTask opener = new OpenDbTask(this);
		opener.execute(helper);
		
		//Create fragments
		noteFrag = new NoteFragment();
		//Empty bundle for arguments
		Bundle args = new Bundle();
		noteFrag.setArguments(args);

		groupFrag = new GroupFragment();
		
		curProgFrag = new ProgressFragment();
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
		this.db = new DatabaseClient(db);
		
		tryLogin();
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
					
					//Clear out the old stuff (since this is a full update)
					//This is inefficient, of course, but the server currently
					//lacks the ability to send "update chunks" or some similar shit.
					db.deleteGroups();
					db.deleteNotifications();
					
					//Now put it into the database
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
		//Create fragment and add
		getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, groupFrag).commit();
	}
	
	public void showNotifications()
	{	
		getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, noteFrag).commit();
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
