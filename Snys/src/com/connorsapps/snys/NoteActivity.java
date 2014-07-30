package com.connorsapps.snys;

import java.io.IOException;
import java.util.Arrays;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class NoteActivity extends ActionBarActivity implements ProgressCallback, DeleteNoteTask.Callback
{
	public static final String GROUP_KEY = "com.connorsapps.snys.NoteActivity.group";
	public static final String NOTE_KEY = "com.connorsapps.snys.NoteActivity.note";
	public static final String EDIT_KEY = "com.connorsapps.snys.NoteActivity.editMode";
	public static final String INITIAL_KEY = "com.connorsapps.snys.NoteActivity.initialSubmit";
	private Group group;
	private Notification note;
	private DatabaseClient db;
	private boolean editMode, initialSubmit;
	private NetworkManager netMan;
	//Gui elements
	private Button saveButton, timeButton, remindAtTimeButton;
	private ViewGroup remindRow;
	private Spinner statusSpinner;
	private TextView groupnameView;
	private EditText noteText;
	

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(R.layout.activity_note);
		
		//Set up button to calling activity
		this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		loadArgs();
		
		//Set title
		this.getSupportActionBar().setTitle("Notification from " + group.getGroupname());
		
		//Now set up the gui
		loadGui();
		
		//Now display as appropriate
		refreshGui();
	}
	
	public void loadArgs()
	{
		//Get arguments
		this.group = this.getIntent().getExtras().getParcelable(GROUP_KEY);

		if (group == null)
			throw new RuntimeException("Must pass group in bundle!");

		this.note = this.getIntent().getExtras().getParcelable(NOTE_KEY);

		if (note == null)
			throw new RuntimeException("Must pass note in bundle!");
		
		//Check if we're starting in edit mode
		this.editMode = this.getIntent().getExtras().getBoolean(EDIT_KEY);
		this.initialSubmit = this.getIntent().getExtras().getBoolean(INITIAL_KEY);

		this.db = MainActivity.getDatabase();
		this.netMan = MainActivity.getNetworkManager();
	}
	
	public void loadGui()
	{
		this.saveButton = (Button)this.findViewById(R.id.an_save_button);
		this.timeButton = (Button)this.findViewById(R.id.an_time_selector_button);
		this.remindAtTimeButton = (Button)this.findViewById(R.id.an_remind_time_selector_button);
		this.remindRow = (ViewGroup)this.findViewById(R.id.an_remind_at_row);
		this.statusSpinner = (Spinner)this.findViewById(R.id.an_status_spinner);
		this.groupnameView = (TextView)this.findViewById(R.id.an_group_name);
		this.noteText = (EditText)this.findViewById(R.id.an_note_text);
		
		//Set unchanging elements
		this.groupnameView.setText(group.getGroupname());
		
		
		//Set listeners
		this.saveButton.setOnClickListener(new OnClickListener() 
		{

			@Override
			public void onClick(View arg0)
			{
				NoteActivity.this.saveNote();
			}
			
		});
		
		View.OnClickListener timeButtonListener = new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				DateTimeDialogFragment frag = new DateTimeDialogFragment((Button)v);
				frag.show(getSupportFragmentManager(), "DateTime");
			}
		};
		
		this.timeButton.setOnClickListener(timeButtonListener);
		this.remindAtTimeButton.setOnClickListener(timeButtonListener);
		
		loadSpinnerAdapter();
		
		this.statusSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View selected,
					int position, long id)
			{
				//Now refresh the views
				NoteActivity.this.refreshGui();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent)
			{
				// Do nothing
			}
			
		});
		
		// Now set values as appropriate
		this.noteText.setText(note.getText());
		this.timeButton.setText(Notification.getFormattedTime(note.getTime(), true));
		this.remindAtTimeButton.setText(Notification.getFormattedTime(note.getRemindAt(), true));		
	}
	
	/**
	 * Load the spinner adapter, removing the unhandled
	 * option if applicable
	 */
	public void loadSpinnerAdapter()
	{
		//Set up status spinner adapter and listener
		Notification.Status[] statOptions = Notification.Status.values();

		// Remove unhandled option if not already unhandled
		if (this.note.getStatus() != Notification.Status.UNHANDLED)
		{
			Notification.Status[] nStat = new Notification.Status[statOptions.length - 1];
			int i = 0;
			for (Notification.Status stat : statOptions)
				if (stat != Notification.Status.UNHANDLED)
					nStat[i++] = stat;
			statOptions = nStat;
		}

		ArrayAdapter<Notification.Status> statAda = new ArrayAdapter<Notification.Status>(
				this.getBaseContext(), android.R.layout.simple_list_item_1,
				statOptions);
		this.statusSpinner.setAdapter(statAda);
		
		//Set initial value
		this.statusSpinner.setSelection(Arrays.binarySearch(statOptions, note.getStatus()));
	}
	
	/**
	 * Set all elements that can change
	 */
	public void refreshGui()
	{
		//Put in appropriate mode
		this.toggleEdit(editMode);
		
		//Hide/show changing rows based on Spinner status
		Notification.Status spin = (Notification.Status)this.statusSpinner.getSelectedItem();
		if (spin == Notification.Status.ALL ||
			spin == Notification.Status.JUST_EMAIL ||
			spin == Notification.Status.ALARM)
		{
			this.remindRow.setVisibility(View.VISIBLE);
		}
		else
		{
			this.remindRow.setVisibility(View.INVISIBLE);
		}
		
		//Set save button to appropriate text
		this.saveButton.setText(this.initialSubmit ? "Submit Note" : "Save Changes");
	}
	
	public void toggleEdit(boolean editMode)
	{
		//Edit mode allows you to change note properties
		//not just handle notes.
		this.timeButton.setEnabled(editMode);
		this.noteText.setEnabled(editMode);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.note, menu);
		
		//Show/hide options menu selections as appropriate (Requires contributor+ permissions to show)
		MenuItem edit = menu.findItem(R.id.edit_option);
		MenuItem delete = menu.findItem(R.id.delete_option);

		if (this.group.getPermissions() == Group.Permissions.ADMIN
				|| this.group.getPermissions() == Group.Permissions.CONTRIBUTOR)
		{
			edit.setVisible(true);
			delete.setVisible(true);
		} else
		{
			edit.setVisible(true);
			delete.setVisible(true);
		}
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case android.R.id.home:
			//Finish this activity if the user presses the up button
			this.finish();
			return true;
		case R.id.edit_option:
			this.editMode = !this.editMode;
			refreshGui();
			return true;
		case R.id.delete_option:
			deleteNote();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	public void saveNote()
	{
		//Save the note, submit if new
		new AsyncTask<Void, Void, Boolean>()
		{
			private String error;
			private Notification updated;
			private boolean handledOnServer;
			
			@Override
			protected void onPreExecute()
			{
				startProgress();
				
				//Get updated notification
				String text = noteText.getText().toString();
				long time = Notification.fromFormattedTime(timeButton.getText().toString(), true);
				Notification.Status nStat = (Notification.Status)statusSpinner.getSelectedItem();
				long remindAt = Notification.fromFormattedTime(remindAtTimeButton.getText().toString(), true);
				
				//Now create object
				updated = new Notification(note.getId(), note.getGid(), text, time, nStat, remindAt);
			}

			@Override
			protected Boolean doInBackground(Void... params)
			{
				try
				{
					if (initialSubmit)
						createNote();
					else
						updateNote();
					
					return true;
				}
				catch (IOException e)
				{
					e.printStackTrace();
					error = "Could not reach server. " +
							(initialSubmit ? "Note could not be saved" : "Changes were saved locally but will not persist.");
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
				endProgress();
				
				//Reload the spinner if necessary
				//If handled, prevent it from being unhandled again
				if ((handledOnServer || (initialSubmit && result)) && 
						note.getStatus() == Notification.Status.UNHANDLED && 
						updated.getStatus() != Notification.Status.UNHANDLED)
				{
					//Update only the relevant fields
					note.setStatus(updated.getStatus());
					note.setRemindAt(updated.getRemindAt());
					
					loadSpinnerAdapter();
				}
				
				if (!result)
				{
					//Alter error message if successfully handled
					if (handledOnServer)
						error = "Note was handled, but this error occurred while trying to edit: " + error;
					
					new GenericDialogFragment("Failed to save note", error, android.R.drawable.ic_dialog_alert, null)
							.show(getSupportFragmentManager(), "BadError");
				}
				else
				{
					//No longer initial
					initialSubmit = false;
			
					//Set note
					note = updated;
					
					//Cheer
					new GenericDialogFragment("Successfully saved note", "Updates will persist.", android.R.drawable.ic_dialog_info, null)
					{
						public void onCancel(DialogInterface dialSoap)
						{
							finish();
						}
					}.show(getSupportFragmentManager(), "GoodMessage");
				}
			}
			
			public void createNote() throws IOException, SnysException
			{
				if (updated.getStatus() == Notification.Status.UNHANDLED)
				{
					updated = netMan.createNote(updated.getGid(), updated.getText(), updated.getTime());
				}
				else
				{
					updated = netMan.createAndHandleNote(updated.getGid(), 
						updated.getText(),
						updated.getTime(), 
						updated.getServerStatus(), 
						updated.getRemindAt());
					
					//Add alarm for notification
					AlarmService.addAlarm(getApplicationContext(), updated, group);
				}
				
				//Now insert (with a proper nid)
				db.insertNotification(updated);
			}
			
			public void updateNote() throws IOException, SnysException
			{
				//Change local status
				//Will not persist if subsequent operations fail.
				//Only if not on initial submit, handle differently otherwise
				db.insertNotification(updated);
				
				//Add alarm for notification (removes if wrong type)
				AlarmService.addAlarm(getApplicationContext(), updated, group);
				
				//First, handle note on server (if status changed, only)
				if ((updated.getStatus() != note.getStatus() || 
						updated.getRemindAt() != note.getRemindAt())
						&& updated.getStatus() != Notification.Status.UNHANDLED)
				{
					netMan.handleNote(updated.getId(), updated.getServerStatus(), updated.getRemindAt());
				}
				
				//Signal that the note has been handled, which will be reported in the error message.
				this.handledOnServer = true;
				
				//Check note changes and edit if appropriate
				boolean changeText = !updated.getText().equals(note.getText());
				boolean changeTime = updated.getTime() != note.getTime();
				
				//Now, edit note if permissions are there and changes have occurred.
				if (group.getPermissions() != Group.Permissions.MEMBER &&
						(changeText || changeTime))
				{
					netMan.editNote(updated.getGid(), updated.getId(), updated.getText(), updated.getTime());
				}
			}
			
		}.execute();
	}
	
	public void deleteNote()
	{		
		//Thread to remove from server (and report error if necessary)
		new DeleteNoteTask(this, netMan, db, getSupportFragmentManager(), group, note).execute();
	}
	
	@Override
	public void onNoteDeleted()
	{
		//Exit on note deleted
		this.finish();
	}

	@Override
	public void startProgress()
	{
		this.setProgressBarIndeterminateVisibility(true);
		
		//Disable the save button
		saveButton.setEnabled(false);
	}

	@Override
	public void endProgress()
	{
		this.setProgressBarIndeterminateVisibility(false);
		
		//Reenable the save button
		saveButton.setEnabled(true);
	}
	
	/**
	 * Get an intent to transition to this activity with a new note
	 * @param from Activity to start intent from
	 * @param toGroup Group to submit to
	 * @return
	 */
	public static void transitionToNewNote(Activity from, Group toGroup)
	{
		//Transition to the noteactivity
		Intent note = new Intent(from, NoteActivity.class);

		long refTime = System.currentTimeMillis();
		Notification nNote = new Notification(-1, toGroup.getId(), "", refTime,
				Notification.Status.UNHANDLED, refTime);

		// Put extras in bundle
		note.putExtra(NoteActivity.GROUP_KEY, toGroup);
		note.putExtra(NoteActivity.NOTE_KEY, nNote);
		note.putExtra(NoteActivity.EDIT_KEY, true);
		note.putExtra(NoteActivity.INITIAL_KEY, true);
		
		from.startActivity(note);
	}
}
