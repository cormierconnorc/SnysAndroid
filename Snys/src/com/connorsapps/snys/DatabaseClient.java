package com.connorsapps.snys;

import com.connorsapps.snys.SnysContract.Account;
import com.connorsapps.snys.SnysContract.Groups;
import com.connorsapps.snys.SnysContract.Notifications;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Class to provide common database operations
 * @author connor
 *
 */
public class DatabaseClient
{
	private SQLiteDatabase db;
	
	public DatabaseClient(SQLiteDatabase db)
	{
		setDatabase(db);
	}
	
	/**
	 * Get the credentials from the database if they exist, null otherwise
	 * @return
	 */
	public Credentials getCredentials()
	{
		String[] projection = {
				Account.COLUMN_EMAIL,
				Account.COLUMN_PASS
			};
		
		Cursor res = db.query(Account.TABLE_NAME, projection, null, null, null, null, null);
		
		//If cursor is empty (no account info!)
		if (res.getCount() == 0)
		{
			res.close();
			return null;
		}
		
		//Now get it
		res.moveToFirst();
		String email = res.getString(res.getColumnIndexOrThrow(Account.COLUMN_EMAIL));
		String pass = res.getString(res.getColumnIndexOrThrow(Account.COLUMN_PASS));
		res.close();
		
		return new Credentials(email, pass);
	}
	
	/**
	 * Update account information (insert or replace)
	 * @param email
	 * @param password
	 */
	public void updateCredentials(String email, String password)
	{
		//Values to insert
		ContentValues values = new ContentValues();
		values.put(Account._ID, 1);
		values.put(Account.COLUMN_EMAIL, email);
		values.put(Account.COLUMN_PASS, password);
		
		//Run insert
		db.insert(Account.TABLE_NAME, null, values);
		
	}
	
	/**
	 * Delete account from database
	 */
	public void deleteCredentials()
	{
		db.delete(Account.TABLE_NAME, null, null);
	}
	
	/**
	 * Return a group from a cursor row
	 * @param curse
	 * @return
	 */
	private Group getGroup(Cursor curse)
	{
		int id = curse.getInt(curse.getColumnIndexOrThrow(Groups._ID));
		String groupname = curse.getString(curse.getColumnIndexOrThrow(Groups.COLUMN_GROUPNAME));
		String permissions = curse.getString(curse.getColumnIndexOrThrow(Groups.COLUMN_PERMISSIONS));
		return new Group(id, groupname, permissions);
	}
	
	/**
	 * Return a notification from a cursor row
	 * @param curse
	 * @return
	 */
	private Notification getNotification(Cursor curse)
	{
		int id = curse.getInt(curse.getColumnIndexOrThrow(Notifications._ID));
		int gid = curse.getInt(curse.getColumnIndexOrThrow(Notifications.COLUMN_GID));
		String text = curse.getString(curse.getColumnIndexOrThrow(Notifications.COLUMN_TEXT));
		long time = curse.getLong(curse.getColumnIndexOrThrow(Notifications.COLUMN_TIME));
		String status = curse.getString(curse.getColumnIndexOrThrow(Notifications.COLUMN_STATUS));
		long remindAt = curse.getLong(curse.getColumnIndexOrThrow(Notifications.COLUMN_REMINDAT));
		return new Notification(id, gid, text, time, status, remindAt);
	}
	
	public void setDatabase(SQLiteDatabase db)
	{
		this.db = db;
	}
	
	public SQLiteDatabase getDatabase()
	{
		return db;
	}
}
