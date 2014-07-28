package com.connorsapps.snys;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.connorsapps.snys.SnysContract.Account;
import com.connorsapps.snys.SnysContract.Groups;
import com.connorsapps.snys.SnysContract.Notifications;

public class SnysDbHelper extends SQLiteOpenHelper
{
	public static final int DATABASE_VERSION = 2;
	public static final String DATABASE_NAME = "Snys.db";
	
	private static final String CREATE_TABLE_ACCOUNT = 
			"CREATE TABLE " + Account.TABLE_NAME + " (" +
			Account._ID + " INTEGER PRIMARY KEY ON CONFLICT REPLACE," + 
			Account.COLUMN_EMAIL + " TEXT," +
			Account.COLUMN_PASS + " TEXT )";
	
	private static final String CREATE_TABLE_GROUPS =
			"CREATE TABLE " + Groups.TABLE_NAME + " (" +
			Groups._ID + " INTEGER PRIMARY KEY ON CONFLICT IGNORE," + //Keep current value. No invitations replacing memberships. Must leave and be reinvited to change permissions.
			Groups.COLUMN_GROUPNAME + " TEXT," +
			Groups.COLUMN_PERMISSIONS + " TEXT," +
			Groups.COLUMN_IS_INVITATION + " TEXT )";
	
	private static final String CREATE_TABLE_NOTIFICATIONS = 
			"CREATE TABLE " + Notifications.TABLE_NAME + " (" +
			Notifications._ID + " INTEGER PRIMARY KEY," + 
			Notifications.COLUMN_GID + " INTEGER," +
			Notifications.COLUMN_TEXT + " TEXT," +
			Notifications.COLUMN_TIME + " INTEGER," +
			Notifications.COLUMN_STATUS + " TEXT," +
			Notifications.COLUMN_REMINDAT + " INTEGER," +
			"FOREIGN KEY (" + Notifications.COLUMN_GID + ") REFERENCES " +
			Groups.TABLE_NAME + "(" + Groups._ID + ") ON DELETE CASCADE " +
			" ON UPDATE CASCADE )";
	
	private static final String DROP_TABLE = "DROP TABLE IF EXISTS ";
	
	private static final String DROP_TABLE_ACCOUNT = DROP_TABLE + Account.TABLE_NAME;
	private static final String DROP_TABLE_GROUPS = DROP_TABLE + Groups.TABLE_NAME;
	private static final String DROP_TABLE_NOTIFICATIONS = DROP_TABLE + Notifications.TABLE_NAME;
	
	public SnysDbHelper(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		db.execSQL(CREATE_TABLE_ACCOUNT);
		db.execSQL(CREATE_TABLE_GROUPS);
		db.execSQL(CREATE_TABLE_NOTIFICATIONS);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		db.execSQL(DROP_TABLE_ACCOUNT);
		db.execSQL(DROP_TABLE_GROUPS);
		db.execSQL(DROP_TABLE_NOTIFICATIONS);
		
		onCreate(db);
	}
}
