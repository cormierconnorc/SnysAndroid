package com.connorsapps.snys;

import java.util.Locale;

public class Group
{
	public static enum Permissions { MEMBER, CONTRIBUTOR, ADMIN };
	
	private int id;
	private String groupname;
	private Permissions permissions;
	
	public Group(int id, String groupname, String permissions)
	{
		this(id, groupname, toPermissions(permissions));
	}
	
	public Group(int id, String groupname, Permissions perm)
	{
		this.setId(id);
		this.setGroupname(groupname);
		this.setPermissions(perm);
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
	
	public String toString()
	{
		return "Group {id = " + id + ", groupname = \"" + groupname + "\", permissions = " + permissions + "}";
	}
}
