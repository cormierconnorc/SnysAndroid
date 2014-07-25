package com.connorsapps.snys;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
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

public class GroupFragment extends Fragment implements ProgressCallback
{
	private ListView myList;
	private ViewGroup root, progress;
	private DatabaseClient db;
	
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved)
	{	
		root = (ViewGroup)inflater.inflate(R.layout.fragment_group, container, false);
		progress = (ViewGroup)inflater.inflate(R.layout.fragment_progress, container, false);
		
		//Set the adapter for the listview
		myList = (ListView)root.findViewById(R.id.sexy_group_view);
		
		setupTouch(myList);
		
		//Open database in background
		new Thread(new Runnable() 
		{
			public void run()
			{
				//Open the database
				db = new DatabaseClient(new SnysDbHelper(GroupFragment.this.getActivity()).getWritableDatabase());
			}
		}).start();
		
		return root;
	}
	
	@Override
	public void onStart()
	{
		super.onStart();
		
		//Start data loading (now in onStart so data is refreshed each time fragment shows)
		LoadDataTask task = new LoadDataTask();
		task.execute();
	}
	
	public void setupTouch(ListView list)
	{
		final GestureDetector det = new GestureDetector(this.getActivity().getBaseContext(), 
				new FlipGestureListener(list, this.getResources().getDisplayMetrics(), 
				new FlipGestureListener.OnClickListener()
				{
					@Override
					public boolean onRowClick(int row)
					{
						//Handled by doing nothing for string
						if (myList.getAdapter().getItemViewType(row) == GroupAdapter.TYPE_STRING)
							return true;
						
						//For a group/invitation row, pass it on to a notification list
						Group g = (Group)myList.getAdapter().getItem(row);
						
						toGroupActivity(g);
						
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
	
	/**
	 * Transition to the group activity showing a certain group.
	 * @param g
	 */
	public void toGroupActivity(Group g)
	{
		Intent intentional = new Intent(this.getActivity(), GroupActivity.class);
		intentional.putExtra(GroupActivity.GROUP_KEY, g);
		this.startActivity(intentional);
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
		root.addView(myList);
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
			//Wait for database to open (just a precaution)
			while (db == null)
			{
				try
				{
					Thread.sleep(50);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			
			//Read data from database
			List<Object> data = new ArrayList<Object>();
			data.add("Groups:");
			data.addAll(db.getGroups());
			data.add("Invitations:");
			data.addAll(db.getInvitations());
			
			return data;
		}
		
		@Override
		protected void onPostExecute(List<Object> data)
		{
			//endProgress();
			GroupAdapter ada = new GroupAdapter(root.getContext(), data);
			myList.setAdapter(ada);
		}
	}
	
	private class GroupAdapter extends ArrayAdapter<Object>
	{
		private static final int TYPE_STRING = 0, TYPE_GROUP = 1, TYPE_INVITATION = 2;
		
		public GroupAdapter(Context context, List<Object> objects)
		{
			super(context, 0, objects);
		}	
		
		/**
		 * Two different types of view: String titles and Group rows
		 */
		@Override
		public int getViewTypeCount()
		{
			return 3;
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
			else if (((Group)item).isInvitation())
				return TYPE_INVITATION;
			else
				return TYPE_GROUP;
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
			//TODO split up (when button row and touch added)
			else
			{
				Group g = (Group)itm;
				TextView gName = (TextView)convertView.findViewById(R.id.groupname);
				TextView gPerm = (TextView)convertView.findViewById(R.id.group_permissions);
				
				gName.setText(g.getGroupname());
				gPerm.setText(g.getPermissions().toString());
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
				flipper.addView(clearFromParent(fragmentRows, fragmentRows.findViewById(R.id.fragment_group_info_row)));
				
				if (type == TYPE_INVITATION)
					flipper.addView(clearFromParent(fragmentRows, fragmentRows.findViewById(R.id.fragment_group_invite_button_row)));
				else
					flipper.addView(clearFromParent(fragmentRows, fragmentRows.findViewById(R.id.fragment_group_group_button_row)));
				
				find = flipper;
			}
			
			return find;
		}
	}
}
