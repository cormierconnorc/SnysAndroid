package com.connorsapps.snys;

import java.io.IOException;

import android.os.AsyncTask;
import android.support.v4.app.FragmentManager;

public class DeleteNoteTask extends AsyncTask<Void, Void, Boolean>
{
	private String error;
	private Callback callback;
	private NetworkManager netMan;
	private DatabaseClient db;
	private FragmentManager fm;
	private Group group;
	private Notification note;
	
	public DeleteNoteTask(Callback callback, NetworkManager netMan, DatabaseClient db, FragmentManager fm, 
			Group group, Notification note)
	{
		this.callback = callback;
		this.netMan = netMan;
		this.db = db;
		this.fm = fm;
		this.group = group;
		this.note = note;
	}
	
	@Override
	protected void onPreExecute()
	{
		callback.startProgress();
	}
	
	@Override
	protected Boolean doInBackground(Void... params)
	{
		try
		{
			netMan.deleteNote(group.getId(), note.getId());
			//Only remove locally if it could be removed on server
			//Allows us to keep the state consistent
			db.deleteNotification(note.getId());
			return true;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			error = "Could not connect to server to delete note.";
		}
		catch (SnysException e)
		{
			error = e.getMessage();
		}
		
		return false;
	}
	
	@Override
	protected void onPostExecute(Boolean success)
	{
		callback.endProgress();
		
		if (!success)
		{
			new GenericDialogFragment("Failed to delete note", error, android.R.drawable.ic_dialog_alert, null)
					.show(fm, "BadError");
		}
		else
		{
			//Finish the activity if we deleted it successfully.
			callback.onNoteDeleted();
		}
	}
	
	public static interface Callback extends ProgressCallback
	{
		/**
		 * Callback for completion
		 */
		public void onNoteDeleted();
	}
}
