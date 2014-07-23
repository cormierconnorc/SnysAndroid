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
	private static final String[] ACCOUNT_PROJECTION = {Account.COLUMN_EMAIL, Account.COLUMN_PASS},
			GROUPS_PROJECTION = {Groups._ID, Groups.COLUMN_GROUPNAME, Groups.COLUMN_PERMISSIONS},
			NOTIFICATIONS_PROJECTION = {Notifications._ID, Notifications.COLUMN_GID, Notifications.COLUMN_TEXT,
				Notifications.COLUMN_TIME, Notifications.COLUMN_STATUS, Notifications.COLUMN_REMINDAT};
	
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
		Cursor res = db.query(Account.TABLE_NAME, ACCOUNT_PROJECTION, null, null, null, null, null);
		
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
	
	private void insertGroups(Group[] groups, boolean isInvitation)
	{
		db.beginTransaction();
		
		for (Group group : groups)
		{
			ContentValues values = new ContentValues();
			values.put(Groups._ID, group.getId());
			values.put(Groups.COLUMN_GROUPNAME, group.getGroupname());
			values.put(Groups.COLUMN_PERMISSIONS, group.getPermissions().toString());
			
			//Insert and replace if gid exists
			db.insertWithOnConflict(Groups.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
		}
		
		db.setTransactionSuccessful();
		db.endTransaction();
	}
	
	/**
	 * Insert an array of groups
	 * @param groups
	 */
	public void insertGroups(Group[] groups)
	{
		this.insertGroups(groups, false);
	}
	
	/**
	 * Insert a single group
	 * @param group
	 */
	public void insertGroup(Group group)
	{
		insertGroups(new Group[]{group});
	}
	
	/**
	 * Insert an array of invitations
	 * @param invites
	 */
	public void insertInvitations(Group[] invites)
	{
		this.insertGroups(invites, true);
	}
	
	/**
	 * Insert a single invitation into the database
	 * @param invite
	 */
	public void insertInvitation(Group invite)
	{
		insertInvitations(new Group[]{invite});
	}
	
	private Group[] getGroups(String selection, String[] selectionArgs)
	{
		Cursor curse = db.query(Groups.TABLE_NAME, 
				GROUPS_PROJECTION, 
				selection, 
				selectionArgs, 
				null, null, null);
		
		Group[] groups = new Group[curse.getCount()];
		
		curse.moveToFirst();
		
		int i = 0;
		
		do
		{
			groups[i++] = this.getGroup(curse);
		} while (curse.moveToNext());
		
		return groups;
	}
	
	/**
	 * Get groups from database
	 * @return
	 */
	public Group[] getGroups()
	{
		return this.getGroups(Groups.COLUMN_IS_INVITATION + " = ?", new String[] {String.valueOf(0)});
	}
	
	/**
	 * Get pending invitations from database
	 * @return
	 */
	public Group[] getInvitations()
	{
		return this.getGroups(Groups.COLUMN_IS_INVITATION + " != ?", new String[] {String.valueOf(0)});
	}
	
	/**
	 * Insert multiple notifications into the database
	 * @param notifications
	 */
	public void insertNotifications(Notification[] notifications)
	{
		db.beginTransaction();
		
		for (Notification note : notifications)
		{
			ContentValues values = new ContentValues();
			values.put(Notifications._ID, note.getId());
			values.put(Notifications.COLUMN_GID, note.getGid());
			values.put(Notifications.COLUMN_TEXT, note.getText());
			values.put(Notifications.COLUMN_TIME, note.getTime());
			values.put(Notifications.COLUMN_STATUS, note.getStatus().toString());
			values.put(Notifications.COLUMN_REMINDAT, note.getRemindAt());
			
			//Insert and replace if gid exists
			db.insertWithOnConflict(Notifications.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
		}
		
		db.setTransactionSuccessful();
		db.endTransaction();
	}
	
	/**
	 * Insert a single notification into the database
	 * @param note
	 */
	public void insertNotification(Notification note)
	{
		this.insertNotifications(new Notification[]{note});
	}
	
	private Notification[] getNotifications(String selection, String[] selectionArgs)
	{
		Cursor curse = db.query(Notifications.TABLE_NAME, 
				NOTIFICATIONS_PROJECTION, 
				selection, 
				selectionArgs, 
				null, null, null);
		
		Notification[] notes = new Notification[curse.getCount()];
		
		curse.moveToFirst();
		
		int i = 0;
		
		do
		{
			notes[i++] = this.getNotification(curse);
		} while (curse.moveToNext());
		
		return notes;
	}
	
	/**
	 * Get all notifications from database
	 * @return
	 */
	public Notification[] getNotifications()
	{
		return getNotifications(null, null);
	}
	
	/**
	 * Get unhandled from db
	 * @return
	 */
	public Notification[] getUnhandledNotifications()
	{
		return getNotifications(Notifications.COLUMN_STATUS + " = ?", new String[] {Notification.Status.UNHANDLED.toString()});
	}
	
	/**
	 * Get handled
	 * @return
	 */
	public Notification[] getHandledNotifications()
	{
		return getNotifications(Notifications.COLUMN_STATUS + " != ?", new String[] {Notification.Status.UNHANDLED.toString()});
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
