package com.connorsapps.snys;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class GroupActivity extends ActionBarActivity
{
	public static final String GROUP_KEY = "com.connorsapps.snys.GroupActivity.myGroup";
	private Group myGroup;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_group);
		
		myGroup = this.getIntent().getExtras().getParcelable(GROUP_KEY);
		
		if (myGroup == null)
			throw new RuntimeException("Group MUST be passed in bundle!");
		
		if (savedInstanceState == null)
			loadNoteFragment();
	}
	
	public void loadNoteFragment()
	{
		//Create fragment with args
		NoteFragment frag = new NoteFragment();
		Bundle args = new Bundle();
		args.putInt(NoteFragment.GID_KEY, myGroup.getId());
		frag.setArguments(args);
		
		//Carry out transaction
		this.getSupportFragmentManager().beginTransaction().replace(R.id.group_activity_fragment_container, frag).commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.group, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings)
		{
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
