package com.connorsapps.snys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class NoteFragment extends Fragment implements ProgressCallback
{
	public static final String GID_KEY = "com.connorsapps.NoteFragment.gid";
	private int gid;
	private ListView list;
	private ViewGroup root, progress;
	private DatabaseClient db;
	private Map<Integer, String> groupNames;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		//Get the group argument (determines which notifications to show)
		gid = this.getArguments().getInt(GID_KEY, -1);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		root = (ViewGroup)inflater.inflate(R.layout.fragment_group, container, false);
		progress = (ViewGroup)inflater.inflate(R.layout.fragment_progress, container, false);
		
		list = (ListView)root.findViewById(R.id.sexy_group_view);
		
		setupTouch(list);
		
		LoadDataTask task = new LoadDataTask();
		task.execute();
		
		return root;
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
		//TODO transition to NoteActivity
		
	}
	
	@Override
	public void startProgress()
	{
		root.removeAllViews();
		root.addView(progress);
	}

	@Override
	public void endProgress()
	{
		root.removeAllViews();
		root.addView(list);
	}
	
	private class LoadDataTask extends AsyncTask<Void, Void, List<Object>>
	{
		@Override
		protected void onPreExecute()
		{
			startProgress();
		}

		@Override
		protected List<Object> doInBackground(Void... arg0)
		{
			db = new DatabaseClient(new SnysDbHelper(root.getContext()).getWritableDatabase());
			
			List<Notification> pending, handled;
			
			if (gid != -1)
			{
				pending = db.getUnhandledNotifications(gid);
				handled = db.getHandledNotifications(gid);
				
				//Build map of groupnames
				groupNames = new TreeMap<Integer, String>();
				Group g = db.getGroup(gid);
				groupNames.put(g.getId(), g.getGroupname());
			}
			else
			{
				pending = db.getUnhandledNotifications();
				handled = db.getHandledNotifications();
				
				//Build map of groupnames
				groupNames = new TreeMap<Integer, String>();
				List<Group> groups = db.getGroups();
				for (Group group : groups)
					groupNames.put(group.getId(), group.getGroupname());
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
			endProgress();
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
				Notification n = (Notification)itm;
				TextView noteText = (TextView)convertView.findViewById(R.id.note_text);
				TextView noteGroup = (TextView)convertView.findViewById(R.id.note_group);
				TextView noteTime = (TextView)convertView.findViewById(R.id.note_time);
				TextView noteStatus = (TextView)convertView.findViewById(R.id.note_status);
				
				noteText.setText(n.getText());
				noteGroup.setText(groupNames.get(n.getGid()));
				noteTime.setText(n.getFormattedTime());
				noteStatus.setText(n.getStatus().toString());
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
