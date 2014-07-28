package com.connorsapps.snys;

import java.util.Locale;

import android.os.Parcel;
import android.os.Parcelable;

public class Group implements Parcelable
{
	public static enum Permissions { MEMBER, CONTRIBUTOR, ADMIN };
	public static final Parcelable.Creator<Group> CREATOR =
			new Parcelable.Creator<Group>()
			{

				@Override
				public Group createFromParcel(Parcel source)
				{
					return new Group(source);
				}

				@Override
				public Group[] newArray(int size)
				{
					return new Group[size];
				}
		
			};
	
	private int id;
	private String groupname;
	private Permissions permissions;
	private boolean invitation;
	
	public Group(int id, String groupname, String permissions, boolean isInvitation)
	{
		this(id, groupname, toPermissions(permissions), isInvitation);
	}
	
	public Group(int id, String groupname, Permissions perm, boolean isInvitation)
	{
		this.setId(id);
		this.setGroupname(groupname);
		this.setPermissions(perm);
		this.setInvitation(isInvitation);
	}
	
	public Group(Parcel in)
	{
		this(in.readInt(),
			in.readString(),
			(Permissions)in.readSerializable(),
			in.readByte() != 0);
	}
	
	@Override
	public int describeContents()
	{
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel out, int flags)
	{
		out.writeInt(id);
		out.writeString(groupname);
		out.writeSerializable(permissions);
		out.writeByte((byte)(invitation ? 1 : 0));
	}
	
	/**
	 * Convert between server's representation and app's
	 * @param perm
	 * @return
	 */
	public static Permissions toPermissions(String perm)
	{
		try
		{
			return Permissions.valueOf(perm.toUpperCase(Locale.ENGLISH));
		}
		catch (IllegalArgumentException e)
		{
			return Permissions.MEMBER;
		}
	}
	
	/**
	 * Convert from local representation to server's
	 * @param perm
	 * @return
	 */
	public static String fromPermissions(Permissions perm)
	{
		switch (perm)
		{
		case ADMIN:
			return "Admin";
		case CONTRIBUTOR:
			return "Contributor";
		default:
			return "Member";
		}
	}

	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public String getGroupname()
	{
		return groupname;
	}

	public void setGroupname(String groupname)
	{
		this.groupname = groupname;
	}

	public Permissions getPermissions()
	{
		return permissions;
	}

	public void setPermissions(Permissions permissions)
	{
		this.permissions = permissions;
	}
	
	public boolean isInvitation()
	{
		return invitation;
	}

	public void setInvitation(boolean isInvitation)
	{
		this.invitation = isInvitation;
	}

	public String toString()
	{
		return "Group {id = " + id + ", groupname = \"" + groupname + "\", permissions = " + permissions + "}";
	}
}
