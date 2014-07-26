package com.connorsapps.snys;

import android.support.v7.app.ActionBarActivity;
import android.content.Intent;
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
		
		this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		myGroup = this.getIntent().getExtras().getParcelable(GROUP_KEY);
		
		this.getSupportActionBar().setTitle(myGroup.getGroupname());
		
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
		
		MenuItem accept = menu.findItem(R.id.option_invite_accept);
		MenuItem deny = menu.findItem(R.id.option_invite_deny);
		MenuItem leave = menu.findItem(R.id.option_group_leave);
		MenuItem submit = menu.findItem(R.id.option_group_submit);
		MenuItem invite = menu.findItem(R.id.option_group_invite);
		MenuItem delete = menu.findItem(R.id.option_group_delete);
		
		//Set visibility of options menu items based on whether this is an invite or group
		if (this.myGroup.isInvitation())
		{
			accept.setVisible(true);
			deny.setVisible(true);
			leave.setVisible(false);
			submit.setVisible(false);
			invite.setVisible(false);
			delete.setVisible(false);
		}
		else
		{
			accept.setVisible(false);
			deny.setVisible(false);
			
			leave.setVisible(true);
			invite.setVisible(true);
			
			if (myGroup.getPermissions() != Group.Permissions.MEMBER)
			{
				submit.setVisible(true);
				
				if (myGroup.getPermissions() != Group.Permissions.CONTRIBUTOR)
				{
					delete.setVisible(true);
				}
				else
					delete.setVisible(false);
			}
			else
				submit.setVisible(false);
		}
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case android.R.id.home:
			this.finish();
			return true;
		//TODO proper option handling
		case R.id.option_invite_deny:
			return true;
		case R.id.option_invite_accept:
			return true;
		case R.id.option_group_leave:
			return true;
		case R.id.option_group_submit:
			submitToGroup();
			return true;
		case R.id.option_group_delete:
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * Submit a note to this group
	 */
	public void submitToGroup()
	{
		NoteActivity.transitionToNewNote(this, myGroup);
	}
}
