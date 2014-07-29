package com.connorsapps.snys;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class NoteFragment extends Fragment implements ProgressCallback, DeleteNoteTask.Callback
{
	public static final String GID_KEY = "com.connorsapps.NoteFragment.gid";
	private int gid;
	private ListView list;
	private ViewGroup root, progress;
	private DatabaseClient db;
	private NetworkManager nm;
	private Map<Integer, Group> groupsMap;
	private MenuItem visibleToggle;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		//Get the group argument (determines which notifications to show)
		gid = this.getArguments().getInt(GID_KEY, -1);
		
		//Get reference to this application's database (poor design, but less wasted memory than opening a new one in each fragment)
		this.db = MainActivity.getDatabase();
		this.nm = MainActivity.getNetworkManager();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		this.setHasOptionsMenu(true);
		
		root = (ViewGroup)inflater.inflate(R.layout.fragment_group, container, false);
		progress = (ViewGroup)inflater.inflate(R.layout.fragment_progress, container, false);
		
		list = (ListView)root.findViewById(R.id.sexy_group_view);
		
		setupTouch(list);
		
		return root;
	}
	
	@Override
	public void onStart()
	{
		super.onStart();
	
		loadData();
	}
	
	public void loadData()
	{
		if (db == null)
			return;
		
		//Start data loading (now in onStart so data is refreshed each time fragment shows)
		LoadDataTask task = new LoadDataTask();
		task.execute();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflate)
	{
		//Add the "Show/hide" hidden option to this fragment after the last option
		visibleToggle = menu.add(Menu.NONE, Menu.NONE, menu.getItem(menu.size() - 1).getOrder() + 1, getToggleText());
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		//Listener for show/hide toggle
		if (item.getItemId() == visibleToggle.getItemId())
		{
			//Toggle 
			this.db.setShowHidden(!this.db.getShowHidden());
			
			//Set item's toggle text
			visibleToggle.setTitle(getToggleText());
			
			//Now start reloading
			LoadDataTask task = new LoadDataTask();
			task.execute();
			
			return true;
		}
		return false;
	}
	
	private String getToggleText()
	{
		return (db != null && db.getShowHidden() ? "Hide" : "Show") + " Hidden";
	}
	
	public void setupTouch(final ListView list)
	{
		final GestureDetector det = new GestureDetector(this.getActivity().getBaseContext(), 
				new FlipGestureListener(list, this.getResources().getDisplayMetrics(), 
				new FlipGestureListener.OnClickListener()
				{
					@Override
					public boolean onRowClick(int row)
					{
						//Handled by doing nothing for string
						if (list.getAdapter().getItemViewType(row) == NoteAdapter.TYPE_STRING)
							return true;
						
						//For a group/invitation row, pass it on to a notification list
						Notification n = (Notification)list.getAdapter().getItem(row);
						
						toNotificationActivity(n);
						
						return true;
					}
				}));
		list.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent arg1)
			{
				return det.onTouchEvent(arg1);
			}
			
		});
	}
	
	public void toNotificationActivity(Notification n)
	{
		Intent notifiableIntent = new Intent(this.getActivity(), NoteActivity.class);
		notifiableIntent.putExtra(NoteActivity.NOTE_KEY, n);
		notifiableIntent.putExtra(NoteActivity.GROUP_KEY, this.groupsMap.get(n.getGid()));
		this.startActivity(notifiableIntent);
	}
	
	/**
	 * Handle an existing notification
	 * @param n The notification
	 */
	public void handleNote(final Notification n)
	{
		//Do not continue if notification is unhandled.
		if (n.getStatus() == Notification.Status.UNHANDLED)
			return;
		
		new AsyncTask<Void, Void, Boolean>()
		{
			private String error;
			
			@Override
			public void onPreExecute()
			{
				startProgress();
			}
			
			@Override
			protected Boolean doInBackground(Void... ns)
			{
				try
				{
					//Put notification in database first so updates persist locally.
					db.insertNotification(n);
					
					//Update the alarm
					AlarmService.addAlarm(getActivity().getApplicationContext(), n, groupsMap.get(n.getGid()));
					
					//Now update on server
					nm.handleNote(n.getId(), n.getServerStatus(), n.getRemindAt());
					
					return true;
				}
				catch (IOException e)
				{
					e.printStackTrace();
					error = "Could not reach server! Changes were saved locally but will not persist.";
				}
				catch (SnysException e)
				{
					error = e.getMessage();
				}

				return false;
			}
			
			@Override
			public void onPostExecute(Boolean result)
			{
				endProgress();
				
				if (result)
				{
					//Simply refersh on success
					loadData();
				}
				else
				{
					new GenericDialogFragment("Failed to handle!", error, android.R.drawable.ic_dialog_alert, null)
							.show(getActivity().getSupportFragmentManager(), "BadFrag");
				}
			}
		}.execute();
	}
	
	@Override
	public void startProgress()
	{
		root.addView(progress);
	}

	@Override
	public void endProgress()
	{
		root.removeView(progress);
	}
	
	@Override
	public void onNoteDeleted()
	{
		//Refresh data on note deleted
		this.loadData();
	}
	
	private class LoadDataTask extends AsyncTask<Void, Void, List<Object>>
	{
		@Override
		protected void onPreExecute()
		{
			//startProgress();
		}

		@Override
		protected List<Object> doInBackground(Void... arg0)
		{		
			List<Notification> pending, handled;
			
			if (gid != -1)
			{
				pending = db.getUnhandledNotifications(gid);
				handled = db.getHandledNotifications(gid);
				
				//Build map of groupnames
				groupsMap = new TreeMap<Integer, Group>();
				Group g = db.getGroup(gid);
				groupsMap.put(g.getId(), g);
			}
			else
			{
				pending = db.getUnhandledNotifications();
				handled = db.getHandledNotifications();
				
				//Build map of groupnames
				groupsMap = new TreeMap<Integer, Group>();
				List<Group> groups = db.getGroups();
				for (Group group : groups)
					groupsMap.put(group.getId(), group);
			}
			
			List<Object> data = new ArrayList<Object>();
			data.add("Pending:");
			data.addAll(pending);
			data.add("Handled:");
			data.addAll(handled);
			
			return data;
		}
		
		@Override
		protected void onPostExecute(List<Object> data)
		{
			//endProgress();
			list.setAdapter(new NoteAdapter(root.getContext(), data));
		}
	}	
	
	private class NoteAdapter extends ArrayAdapter<Object>
	{
		private static final int TYPE_STRING = 0, TYPE_NOTE = 1;
		
		public NoteAdapter(Context context, List<Object> objects)
		{
			super(context, 0, objects);
		}	
		
		/**
		 * Two different types of view: String titles and Group rows
		 */
		@Override
		public int getViewTypeCount()
		{
			return 2;
		}
		
		/**
		 * @param pos
		 * @return 0 if String title, 1 if group row
		 */
		@Override
		public int getItemViewType(int pos)
		{
			Object item = this.getItem(pos);
			
			if (item instanceof String)
				return TYPE_STRING;
			else
				return TYPE_NOTE;
		}
		
		@Override
		public View getView(int pos, View convertView, ViewGroup parent)
		{
			Object itm = this.getItem(pos);
			int type = getItemViewType(pos);
			
			//Inflate the type appropriate to this value
			if (convertView == null)
			{
				convertView = inflateByType(type);
			}
			
			if (type == TYPE_STRING)
			{
				TextView title = (TextView)convertView.findViewById(R.id.section_title);
				title.setText((String)itm);
			}
			else
			{
				final Notification n = (Notification)itm;
				final Group g = groupsMap.get(n.getGid());
				TextView noteText = (TextView)convertView.findViewById(R.id.note_text);
				TextView noteGroup = (TextView)convertView.findViewById(R.id.note_group);
				TextView noteTime = (TextView)convertView.findViewById(R.id.note_time);
				TextView noteStatus = (TextView)convertView.findViewById(R.id.note_status);
				
				noteText.setText(n.getText());
				noteGroup.setText(g.getGroupname());
				noteTime.setText(Notification.getFormattedTime(n.getTime()));
				noteStatus.setText(n.getStatus().toString());
				
				//Now setup the buttons
				Button viewNote = (Button)convertView.findViewById(R.id.view_note_button);
				Button handleNote = (Button)convertView.findViewById(R.id.handle_note_button);
				Button editNote = (Button)convertView.findViewById(R.id.edit_note_button);
				Button deleteNote = (Button)convertView.findViewById(R.id.delete_note_button);
				
				//Disable edit and delete buttons if you can't do that.
				if (g.getPermissions() == Group.Permissions.MEMBER)
				{
					editNote.setEnabled(false);
					deleteNote.setEnabled(false);
				}
				
				//Listeners!
				viewNote.setOnClickListener(new View.OnClickListener()
				{
					
					@Override
					public void onClick(View v)
					{
						Intent viewIntent = new Intent(getContext(), NoteActivity.class);
						viewIntent.putExtra(NoteActivity.GROUP_KEY, g);
						viewIntent.putExtra(NoteActivity.NOTE_KEY, n);
						getActivity().startActivity(viewIntent);
					}
				});
				
				handleNote.setOnClickListener(new View.OnClickListener()
				{
					
					@Override
					public void onClick(View v)
					{
						new DialogFragment()
						{
							@Override
							public Dialog onCreateDialog(Bundle saved)
							{
								AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
								
								builder.setTitle("Change Reminder");
								
								ViewGroup root = (ViewGroup)getActivity().getLayoutInflater().inflate(R.layout.handle_dialog, null, false);
								
								//The spinner
								final Spinner status = (Spinner)root.findViewById(R.id.an_status_spinner);
								
								final List<Notification.Status> stats = 
										new ArrayList<Notification.Status>(Arrays.asList(Notification.Status.values()));
								
								if (n.getStatus() != Notification.Status.UNHANDLED)
									stats.remove(Notification.Status.UNHANDLED);
								
								
								ArrayAdapter<Notification.Status> ada = 
										new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, stats);
								
								status.setAdapter(ada);							
								
								final ViewGroup remindAtRow = (ViewGroup)root.findViewById(R.id.an_remind_at_row);
								final Button remindAt = (Button)root.findViewById(R.id.an_remind_time_selector_button);
								
								//Set button's initial text
								remindAt.setText(Notification.getFormattedTime(n.getRemindAt(), false));
								
								//Set button's listener to create datetime dialog
								remindAt.setOnClickListener(new View.OnClickListener() 
								{
									@Override
									public void onClick(View v)
									{
										DateTimeDialogFragment dial = new DateTimeDialogFragment((Button)v, false);
										dial.show(getActivity().getSupportFragmentManager(), "Selector");
									}
								});
								
								
								status.setOnItemSelectedListener(new OnItemSelectedListener() 
								{

									@Override
									public void onItemSelected(
											AdapterView<?> parent, View sel,
											int pos, long id)
									{
										Notification.Status nStat = stats.get(pos);
										
										if (nStat == Notification.Status.ALL ||
											nStat == Notification.Status.JUST_EMAIL ||
											nStat == Notification.Status.ALARM)
										{
											remindAtRow.setVisibility(View.VISIBLE);
										}
										else
										{
											remindAtRow.setVisibility(View.INVISIBLE);
										}
									}

									@Override
									public void onNothingSelected(
											AdapterView<?> arg0)
									{
										// Do nothing
									}
									
								});
								
								status.setSelection(stats.indexOf(n.getStatus()));
								
								builder.setView(root);
								
								builder.setPositiveButton("Save", new DialogInterface.OnClickListener()
								{
									@Override
									public void onClick(DialogInterface dialog,
											int which)
									{
										Notification.Status nStat = (Notification.Status)status.getSelectedItem();
										long time = Notification.fromFormattedTime(remindAt.getText().toString(), false);
										
										n.setStatus(nStat);
										n.setRemindAt(time);
										
										handleNote(n);
									}
								
								});
								
								builder.setNegativeButton("Cancel", null);
								
								return builder.create();
							}
						}.show(getActivity().getSupportFragmentManager(), "PromptFrag");
					}
				});
				
				editNote.setOnClickListener(new View.OnClickListener()
				{
					
					@Override
					public void onClick(View v)
					{
						Intent viewIntent = new Intent(getContext(), NoteActivity.class);
						viewIntent.putExtra(NoteActivity.GROUP_KEY, g);
						viewIntent.putExtra(NoteActivity.NOTE_KEY, n);
						viewIntent.putExtra(NoteActivity.EDIT_KEY, true);
						getActivity().startActivity(viewIntent);
					}
				});
				
				deleteNote.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						new DeleteNoteTask(NoteFragment.this, nm, db, getActivity().getSupportFragmentManager(), g, n).execute();
					}
				});
					
			}
			
			return convertView;
		}
		
		private View clearFromParent(ViewGroup parent, View child)
		{
			parent.removeView(child);
			return child;
		}
		
		private View inflateByType(int type)
		{
			LayoutInflater inflater = LayoutInflater.from(getContext());
			ViewGroup fragmentRows = (ViewGroup)inflater.inflate(R.layout.fragment_rows, null, false);
			View find;
			
			if (type == TYPE_STRING)
			{
				find = fragmentRows.findViewById(R.id.fragment_both_title_row);
				//Set each view to simply fill the parent
				find.setLayoutParams(new AbsListView.LayoutParams(find.getLayoutParams()));
			}
			else
			{
				ViewFlipper flipper = new ViewFlipper(getContext());
				flipper.addView(clearFromParent(fragmentRows, fragmentRows.findViewById(R.id.fragment_notification_info_row)));
				flipper.addView(clearFromParent(fragmentRows, fragmentRows.findViewById(R.id.fragment_notification_button_row)));			
				find = flipper;
			}
			
			return find;
		}
	}
}
