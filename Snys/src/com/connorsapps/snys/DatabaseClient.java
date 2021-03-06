package com.connorsapps.snys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.connorsapps.snys.SnysContract.Account;
import com.connorsapps.snys.SnysContract.Groups;
import com.connorsapps.snys.SnysContract.NewLog;
import com.connorsapps.snys.SnysContract.Notifications;

/**
 * Class to provide common database operations
 * @author connor
 *
 */
public class DatabaseClient
{
	private SQLiteDatabase db;
	private boolean showHidden;
	private static final String[] ACCOUNT_PROJECTION = {Account.COLUMN_EMAIL, Account.COLUMN_PASS},
			GROUPS_PROJECTION = {Groups._ID, Groups.COLUMN_GROUPNAME, Groups.COLUMN_PERMISSIONS, Groups.COLUMN_IS_INVITATION},
			NOTIFICATIONS_PROJECTION = {Notifications._ID, Notifications.COLUMN_GID, Notifications.COLUMN_TEXT,
				Notifications.COLUMN_TIME, Notifications.COLUMN_STATUS, Notifications.COLUMN_REMINDAT},
			NEWLOG_PROJECTION = {NewLog.COLUMN_NID};
	
	/**
	 * Set database and use default showHidden value (false)
	 * @param db
	 */
	public DatabaseClient(SQLiteDatabase db)
	{
		this(db, false);
	}
	
	/**
	 * @param db
	 * @param showHidden Show notifications with hidden status. If false, notification queries will not return these
	 */
	public DatabaseClient(SQLiteDatabase db, boolean showHidden)
	{
		setDatabase(db);
		setShowHidden(showHidden);
	}
	
	/**
	 * Convenience wrapper for delete
	 * @param tableName
	 * @param selection
	 * @param selectionArgs
	 */
	public void delete(String tableName, String selection, String... selectionArgs)
	{
		db.delete(tableName, selection, selectionArgs);
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
	
	public void insertGroups(List<Group> groups)
	{
		db.beginTransaction();
		
		for (Group group : groups)
		{
			ContentValues values = new ContentValues();
			values.put(Groups._ID, group.getId());
			values.put(Groups.COLUMN_GROUPNAME, group.getGroupname());
			values.put(Groups.COLUMN_PERMISSIONS, group.getPermissions().toString());
			values.put(Groups.COLUMN_IS_INVITATION, (group.isInvitation() ? 1 : 0));
			
			//Insert and replace if gid exists
			db.insertWithOnConflict(Groups.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
		}
		
		db.setTransactionSuccessful();
		db.endTransaction();
	}
	
	/**
	 * Update the group list and remove ones that are no longer present.
	 * MUST be done all at once (no separate queries for pending and handled)
	 * @param newGroups
	 */
	public void updateGroups(List<Group> newGroups)
	{
		db.beginTransaction();
		
		for (Group group : newGroups)
		{
			ContentValues values = new ContentValues();
			values.put(Groups._ID, group.getId());
			values.put(Groups.COLUMN_GROUPNAME, group.getGroupname());
			values.put(Groups.COLUMN_PERMISSIONS, group.getPermissions().toString());
			values.put(Groups.COLUMN_IS_INVITATION, (group.isInvitation() ? 1 : 0));
			
			//Insert and replace if gid exists
			db.insertWithOnConflict(Groups.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
		}
		
		//Clear out the old
		if (newGroups.size() > 0)
		{
			//Holder for new id's
			String[] groupIds = new String[newGroups.size()];
			
			int i = 0;
			for (Group group : newGroups)
				groupIds[i++] = String.valueOf(group.getId());
		
			//Now remove all those that were not in the previous list
			db.delete(Groups.TABLE_NAME, Groups._ID + " NOT IN (" + genPlaceholders(newGroups.size()) + ")", groupIds);
		}
		
		db.setTransactionSuccessful();
		db.endTransaction();
	}
	
	/**
	 * Insert a single group
	 * @param group
	 */
	public void insertGroup(Group group)
	{
		insertGroups(Collections.singletonList(group));
	}
	
	/**
	 * Insert an array of invitations (alias for insertGroups)
	 * @param invites
	 */
	public void insertInvitations(List<Group> invites)
	{
		this.insertGroups(invites);
	}
	
	/**
	 * Insert a single invitation into the database (alias for insertGroup)
	 * @param invite
	 */
	public void insertInvitation(Group invite)
	{
		insertGroup(invite);
	}
	
	private List<Group> getGroups(String selection, String[] selectionArgs)
	{
		Cursor curse = db.query(Groups.TABLE_NAME, 
				GROUPS_PROJECTION, 
				selection, 
				selectionArgs, 
				null, null, null);
		
		List<Group> groups = new ArrayList<Group>();
		
		if (curse.getCount() == 0)
			return groups;
		
		curse.moveToFirst();
		
		do
		{
			groups.add(this.getGroup(curse));
		} while (curse.moveToNext());
		
		return groups;
	}
	
	/**
	 * Get groups this user has the right to submit to
	 * @return
	 */
	public List<Group> getSubmittableGroups()
	{
		return this.getGroups(Groups.COLUMN_PERMISSIONS + " != ?", 
				new String[] {Group.Permissions.MEMBER.toString()});
	}
	
	/**
	 * Get a single group out of database
	 * @param gid
	 * @return Group if it exists, null otherwise
	 */
	public Group getGroup(int gid)
	{
		List<Group> groups = this.getGroups(Groups._ID + " = ?", new String[] {String.valueOf(gid)});
		
		if (groups.size() == 0)
			return null;
		return groups.get(0);
	}
	
	/**
	 * Get groups from database
	 * @return
	 */
	public List<Group> getGroups()
	{
		return this.getGroups(Groups.COLUMN_IS_INVITATION + " = ?", new String[] {String.valueOf(0)});
	}
	
	/**
	 * Get pending invitations from database
	 * @return
	 */
	public List<Group> getInvitations()
	{
		return this.getGroups(Groups.COLUMN_IS_INVITATION + " != ?", new String[] {String.valueOf(0)});
	}
	
	/**
	 * Delete all groups (and invitations!)
	 */
	public void deleteGroups()
	{
		db.delete(Groups.TABLE_NAME, null, null);
	}
	
	/**
	 * Delete a single group
	 * @param gid gid of group to delete
	 */
	public void deleteGroup(int gid)
	{
		db.delete(Groups.TABLE_NAME, Groups._ID + " = ?", new String[] {String.valueOf(gid)});
	}
	
	/**
	 * Accept an invitation to a group
	 * @param gid
	 */
	public void acceptInvitation(int gid)
	{
		//Values to update
		ContentValues values = new ContentValues();
		values.put(Groups.COLUMN_IS_INVITATION, 0);
		
		//Row
		String selection = Groups._ID + " = ?";
		String[] args = {String.valueOf(gid)};
		
		db.update(Groups.TABLE_NAME, values, selection, args);
	}
	
	/**
	 * Added for symmetry, simply deletes a group
	 * @param gid
	 */
	public void denyInvitation(int gid)
	{
		this.deleteGroup(gid);
	}
	
	/**
	 * Insert multiple notifications into the database
	 * @param notifications
	 */
	public void insertNotifications(List<Notification> notifications)
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
	 * Insert new notifications and remove all old ones
	 * @param newNotes
	 */
	public void updateNotifications(List<Notification> newNotes)
	{
		db.beginTransaction();
		
		for (Notification note : newNotes)
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
		
		//Clear out the old
		if (newNotes.size() > 0)
		{
			//Holder for new id's
			String[] noteIds = new String[newNotes.size()];
			
			int i = 0;
			for (Notification note : newNotes)
				noteIds[i++] = String.valueOf(note.getId());
		
			//Now remove all those that were not in the previous list
			db.delete(Notifications.TABLE_NAME, Notifications._ID + " NOT IN (" + genPlaceholders(newNotes.size()) + ")", noteIds);
		}
		
		db.setTransactionSuccessful();
		db.endTransaction();
	}
	
	private String genPlaceholders(int num)
	{
		StringBuilder buildy = new StringBuilder(num * 2 - 1);
		
		for (int i = 0; i < num; i++)
		{
			buildy.append("?");
			
			if (i != num - 1)
				buildy.append(",");
		}
		
		return buildy.toString();
	}
	
	/**
	 * Insert a single notification into the database
	 * @param note
	 */
	public void insertNotification(Notification note)
	{
		//If unhandled, make sure the background thread doesn't prompt us for it by inserting a NewLog entry
		if (note.getStatus() == Notification.Status.UNHANDLED)
		{
			ContentValues values = new ContentValues();
			values.put(NewLog.COLUMN_NID, note.getId());
			
			db.insert(NewLog.TABLE_NAME, null, values);
		}
		
		this.insertNotifications(Collections.singletonList(note));
	}
	
	private List<Notification> getNotifications(String selection, String[] selectionArgs)
	{
		Cursor curse = db.query(Notifications.TABLE_NAME, 
				NOTIFICATIONS_PROJECTION, 
				selection, 
				selectionArgs, 
				null, null, null);
		
		List<Notification> notes = new ArrayList<Notification>();
		
		if (curse.getCount() == 0)
			return notes;
		
		curse.moveToFirst();
		
		do
		{
			notes.add(this.getNotification(curse));
		} while (curse.moveToNext());
		
		return notes;
	}
	
	/**
	 * Get all notifications from database
	 * @return
	 */
	public List<Notification> getNotifications()
	{
		String selection = showHidden ? null : Notifications.COLUMN_STATUS + " != ?";
		String[] args = (showHidden ? null : new String[] {Notification.Status.HIDE.toString()});
		
		return getNotifications(selection, args);
	}
	
	private void insertNewLogEntries(List<Notification> notes)
	{
		db.beginTransaction();
		
		//And insert the nid's of these new notifications into the NewLog table so they won't be shown again
		for (Notification note : notes)
		{
			ContentValues values = new ContentValues();
			values.put(NewLog.COLUMN_NID, note.getId());
			
			db.insert(NewLog.TABLE_NAME, null, values);
		}
		
		db.setTransactionSuccessful();
		db.endTransaction();
	}
	
	/**
	 * Get the unhandled notifications that the app has not yet prompted for.
	 * @return
	 */
	public List<Notification> getNewUnhandledNotifications()
	{
		//The nids to not include
		Cursor nids = db.query(NewLog.TABLE_NAME, NEWLOG_PROJECTION, null, null, null, null, null);
		
		int count = nids.getCount();
		
		//Just do the standard return if no handled nids
		if (count == 0)
		{
			List<Notification> notes = getUnhandledNotifications();
			insertNewLogEntries(notes);
			return notes;
		}
		
		String where = Notifications.COLUMN_STATUS + " = ? AND " + Notifications._ID + " NOT IN (" + 
					genPlaceholders(count) + ")";
		String[] args = new String[count + 1];
		
		int i = 0;
		
		args[i++] = Notification.Status.UNHANDLED.toString();
		
		nids.moveToFirst();
		
		do
		{
			args[i++] = String.valueOf(nids.getInt(nids.getColumnIndexOrThrow(NewLog.COLUMN_NID)));
		} while (nids.moveToNext());
		
		//Now query
		List<Notification> notes = getNotifications(where, args);
		
		insertNewLogEntries(notes);
		
		return notes;
	}
	
	/**
	 * Get unhandled from db
	 * @return
	 */
	public List<Notification> getUnhandledNotifications()
	{
		return getNotifications(Notifications.COLUMN_STATUS + " = ?", 
				new String[] {Notification.Status.UNHANDLED.toString()});
	}
	
	/**
	 * Get the unhandled notifications associated with some group
	 * @param gid
	 * @return
	 */
	public List<Notification> getUnhandledNotifications(int gid)
	{
		return getNotifications(Notifications.COLUMN_STATUS + " = ? AND " + Notifications.COLUMN_GID + " = ?", 
				new String[] {Notification.Status.UNHANDLED.toString(), String.valueOf(gid)});
	}
	
	/**
	 * Get handled
	 * @return
	 */
	public List<Notification> getHandledNotifications()
	{
		String selection = Notifications.COLUMN_STATUS + " != ?" + (showHidden ? "" : " AND " + Notifications.COLUMN_STATUS + " != ?");
		String[] args = (showHidden ? new String[] {Notification.Status.UNHANDLED.toString()} : 
			new String[] {Notification.Status.UNHANDLED.toString(), Notification.Status.HIDE.toString()});
		
		return getNotifications(selection, args);
	}
	
	/**
	 * Get handled notifications associated with some group
	 * @param gid
	 * @return
	 */
	public List<Notification> getHandledNotifications(int gid)
	{
		String selection = Notifications.COLUMN_STATUS + " != ? AND " + 
				Notifications.COLUMN_GID + " = ?" + 
				(showHidden ? "" : " AND " + Notifications.COLUMN_STATUS + " != ?");
		
		String[] args = (showHidden ? new String[] {Notification.Status.UNHANDLED.toString(), String.valueOf(gid)} : 
			new String[] {Notification.Status.UNHANDLED.toString(), String.valueOf(gid), Notification.Status.HIDE.toString()});
		
		return getNotifications(selection, args);
	}
	
	/**
	 * Delete all notifications (handled and pending)
	 */
	public void deleteNotifications()
	{
		db.delete(Notifications.TABLE_NAME, null, null);
	}
	
	public void deleteNotification(int nid)
	{
		this.delete(Notifications.TABLE_NAME, Notifications._ID + " = ?", String.valueOf(nid));
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
		boolean isInvitation = curse.getInt(curse.getColumnIndexOrThrow(Groups.COLUMN_IS_INVITATION)) == 0 ? false : true;
		return new Group(id, groupname, permissions, isInvitation);
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

	public boolean getShowHidden()
	{
		return showHidden;
	}

	public void setShowHidden(boolean showHidden)
	{
		this.showHidden = showHidden;
	}
}
