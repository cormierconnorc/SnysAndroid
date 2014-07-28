package com.connorsapps.snys;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

public class GroupUtils
{
	private Callback callback;
	private FragmentManager man;
	private NetworkManager nm;
	private DatabaseClient db;
	
	public GroupUtils(Callback callback, FragmentManager man)
	{
		this.callback = callback;
		this.man = man;
		this.nm = MainActivity.getNetworkManager();
		this.db = MainActivity.getDatabase();
	}
	
	public void deny(final Group myGroup)
	{
		new AsyncTask<Void, Void, Boolean>()
		{
			@Override
			protected void onPreExecute()
			{
				callback.startProgress();
			}
			
			@Override
			protected Boolean doInBackground(Void... args)
			{
				try
				{
					nm.denyInvite(myGroup.getId());
					db.denyInvitation(myGroup.getId());
					
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
				callback.endProgress();
				
				if (result)
				{
					//Nothing left to do here, group no longer exists
					callback.onInviteDenied();
				}
				else
				{
					new GenericDialogFragment("Could not deny invite", "Server couldn't be reached!", android.R.drawable.ic_dialog_alert, null)
							.show(man, "Badcallback");
				}
			}
		}.execute();
	}
	
	public void accept(final Group myGroup)
	{
		new AsyncTask<Void, Void, Boolean>()
		{
			private String error;
			
			@Override
			protected void onPreExecute()
			{
				callback.startProgress();
			}
			
			@Override
			protected Boolean doInBackground(Void... args)
			{
				try
				{
					nm.acceptInvite(myGroup.getId());
					db.acceptInvitation(myGroup.getId());
					
					//Now insert new notifications into database
					db.insertNotifications(nm.getNotifications());
					
					return true;
				}
				catch (IOException e)
				{
					e.printStackTrace();
					error = "Could not reach server!";
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
				callback.endProgress();
				
				if (result)
				{
					callback.onInviteAccepted();
				}
				else
				{
					new GenericDialogFragment("Could not accept invite", error, android.R.drawable.ic_dialog_alert, null)
							.show(man, "Badcallback");
				}
			}
		}.execute();
	}
	
	public void leave(final Group myGroup)
	{
		new AsyncTask<Void, Void, Boolean>()
		{
			@Override
			protected void onPreExecute()
			{
				callback.startProgress();
			}
			
			@Override
			protected Boolean doInBackground(Void... args)
			{
				try
				{
					nm.leaveGroup(myGroup.getId());
					db.deleteGroup(myGroup.getId());
					
					//Refresh notifications to exclude this group
					db.deleteNotifications();
					db.insertNotifications(nm.getNotifications());
					
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
				callback.endProgress();
				
				if (result)
				{
					//Nothing left to do here, group no longer exists
					callback.onGroupLeft();
				}
				else
				{
					new GenericDialogFragment("Could not leave group", "Server couldn't be reached!", android.R.drawable.ic_dialog_alert, null)
							.show(man, "Badcallback");
				}
			}
		}.execute();
	}
	
	public void delete(final Group myGroup)
	{
		new AsyncTask<Void, Void, Boolean>()
		{
			private String error;
			
			@Override
			protected void onPreExecute()
			{
				callback.startProgress();
			}
			
			@Override
			protected Boolean doInBackground(Void... args)
			{
				try
				{
					nm.deleteGroup(myGroup.getId());
					db.deleteGroup(myGroup.getId());
					
					//Now refersh notes to not have this
					db.deleteNotifications();
					db.insertNotifications(nm.getNotifications());
					
					return true;
				}
				catch (IOException e)
				{
					e.printStackTrace();
					error = "Server couldn't be reached!";
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
				callback.endProgress();
				
				if (result)
				{
					//Nothing left to do here, group no longer exists
					callback.onGroupDeleted();
				}
				else
				{
					new GenericDialogFragment("Could not delete group", error, android.R.drawable.ic_dialog_alert, null)
							.show(man, "BadFrag");
				}
			}
		}.execute();
	}
	
	public void invite(final Group g)
	{
		new DialogFragment()
		{
			@Override
			public Dialog onCreateDialog(Bundle saved)
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				
				builder.setTitle("Invite");
				
				View layout = (View)getActivity().getLayoutInflater().inflate(R.layout.invite_dialog, null, false);
				
				final EditText email = (EditText)layout.findViewById(R.id.invite_email_address);
				final Spinner perm = (Spinner)layout.findViewById(R.id.invite_group_permissions);
				
				//Set up the permissions list for invitations (with permissions less than or equal to the sender's)
				List<Group.Permissions> perms = new ArrayList<Group.Permissions>();
				
				perms.add(Group.Permissions.MEMBER);
				
				if (g.getPermissions() != Group.Permissions.MEMBER)
				{
					perms.add(Group.Permissions.CONTRIBUTOR);
					
					if (g.getPermissions() != Group.Permissions.CONTRIBUTOR)
					{
						perms.add(Group.Permissions.ADMIN);
					}
				}
				
				//Now set up adapter
				perm.setAdapter(new ArrayAdapter<Group.Permissions>(perm.getContext(), android.R.layout.simple_list_item_1, perms));
				
				//Add the view to the builder
				builder.setView(layout);
				
				builder.setPositiveButton("Send", new DialogInterface.OnClickListener()
				{
					
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						sendInvites(g, email.getText().toString(), (Group.Permissions)perm.getSelectedItem());
					}
				});
				
				builder.setNegativeButton("Cancel", null);
				
				return builder.create();
			}
		}.show(man, "Prompter");
	}
	
	private void sendInvites(final Group g, final String email, final Group.Permissions permission)
	{
		new AsyncTask<Void, Void, Boolean>()
		{
			String error;
			
			@Override
			protected void onPreExecute()
			{
				callback.startProgress();
			}
			
			@Override
			protected Boolean doInBackground(Void... unused)
			{
				try
				{
					nm.inviteUser(g.getId(), email, Group.fromPermissions(permission));
					
					return true;
				} 
				catch (IOException e)
				{
					e.printStackTrace();
					
					error = "Server unreachable! Shit!";
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
				callback.endProgress();
				
				if (result)
					callback.onInviteSent();
				else
				{
					new GenericDialogFragment("Failed to send invite", error, android.R.drawable.ic_dialog_alert, null)
							.show(man, "BadFrag");
				}
			}
			
		}.execute();
	}
	
	public static interface Callback extends ProgressCallback
	{
		/**
		 * Callbacks for successful executions
		 */
		public void onInviteAccepted();
		public void onInviteDenied();
		public void onGroupLeft();
		public void onGroupDeleted();
		public void onInviteSent();
	}
}
