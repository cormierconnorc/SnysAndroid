package com.connorsapps.snys;

import android.provider.BaseColumns;

/**
 * Database schema for Snys
 * @author connor
 *
 */
public final class SnysContract
{
	//No instantiation
	public SnysContract() {}
	
	public static abstract class Account implements BaseColumns 
	{
		public static final String TABLE_NAME = "Account";
		public static final String COLUMN_EMAIL = "Email";
		public static final String COLUMN_PASS = "Pass";
	}
	
	public static abstract class Groups implements BaseColumns
	{
		public static final String TABLE_NAME = "Groups";
		public static final String COLUMN_GROUPNAME = "Groupname";
		public static final String COLUMN_PERMISSIONS = "Permissions";
		public static final String COLUMN_IS_INVITATION = "IsInvitation";
	}

	public static abstract class Notifications implements BaseColumns
	{
		public static final String TABLE_NAME = "Notifications";
		public static final String COLUMN_GID = "Gid";
		public static final String COLUMN_TEXT = "Text";
		public static final String COLUMN_TIME = "Time";
		public static final String COLUMN_STATUS = "Status";
		public static final String COLUMN_REMINDAT = "RemindAt";
	}

}
