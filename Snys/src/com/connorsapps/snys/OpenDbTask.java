package com.connorsapps.snys;

import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

public class OpenDbTask extends AsyncTask<SnysDbHelper, Boolean, SQLiteDatabase>
{
	private MainActivity callback;
	
	public OpenDbTask(MainActivity callback)
	{
		this.callback = callback;
	}

	@Override
	protected SQLiteDatabase doInBackground(SnysDbHelper... helpers)
	{
		SnysDbHelper helper = helpers[0];
		return helper.getWritableDatabase();
	}
	
	@Override
	protected void onPreExecute()
	{
		callback.setProgressBarIndeterminateVisibility(true);
	}
	
	@Override
	protected void onPostExecute(SQLiteDatabase database)
	{
		callback.setProgressBarIndeterminateVisibility(false);
		callback.onDatabaseOpened(database);
	}

}
