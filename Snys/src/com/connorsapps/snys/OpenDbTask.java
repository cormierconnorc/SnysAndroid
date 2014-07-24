package com.connorsapps.snys;

import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

public class OpenDbTask extends AsyncTask<SnysDbHelper, Boolean, SQLiteDatabase>
{
	private DbCallback callback;
	
	public OpenDbTask(DbCallback callback)
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
		callback.startProgress();
	}
	
	@Override
	protected void onPostExecute(SQLiteDatabase database)
	{
		callback.endProgress();
		callback.onDatabaseOpened(database);
	}
	
	/**
	 * Callback interface for database open task
	 * @author connor
	 *
	 */
	public static interface DbCallback extends ProgressCallback
	{
		/**
		 * Respond to open database
		 * @param db
		 */
		public void onDatabaseOpened(SQLiteDatabase db);
	}

}
