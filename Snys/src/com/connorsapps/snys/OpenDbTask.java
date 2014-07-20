package com.connorsapps.snys;

import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

public class OpenDbTask extends AsyncTask<SnysDbHelper, SQLiteDatabase, Boolean>
{
	private MainActivity callback;
	
	public OpenDbTask(MainActivity callback)
	{
		this.callback = callback;
	}

	@Override
	protected Boolean doInBackground(SnysDbHelper... helpers)
	{
		SnysDbHelper helper = helpers[0];
		this.publishProgress(helper.getWritableDatabase());
		return true;
	}
	
	@Override
	protected void onProgressUpdate(SQLiteDatabase... databases)
	{
		callback.onDatabaseOpened(databases[0]);
	}

}
