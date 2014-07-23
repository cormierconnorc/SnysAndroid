package com.connorsapps.snys;

import java.util.Locale;

public class Notification
{
	public static enum Status {ALL, JUST_EMAIL, HIDE, ALARM, NO_REMIND, UNHANDLED};
	
	private int id;
	private int gid;
	private String text;
	private long time;
	private Status status;
	private long remindAt;
	
	public Notification(int id, int gid, String text, long time)
	{
		this(id, gid, text, time, Status.UNHANDLED, 0);
	}
	
	public Notification(int id, int gid, String text, long time,
			String stat, long remindAt)
	{
		this(id, gid, text, time, toStatus(stat), remindAt);
	}
	
	public Notification(int id, int gid, String text, long time,
			Status stat, long remindAt)
	{
		this.id = id;
		this.gid = gid;
		this.text = text;
		this.time = time;
		this.status = stat;
		this.remindAt = remindAt;
	}
	
	/**
	 * Convert between server's/database's permissions representation
	 * and the one used by the app.
	 * @param stat
	 * @return
	 */
	public static Status toStatus(String stat)
	{
		String upStat = stat.toUpperCase(Locale.ENGLISH);
		
		switch (upStat)
		{
		case "JUSTEMAIL":
			return Status.JUST_EMAIL;
		case "NOREMIND":
			return Status.NO_REMIND;
		default:
			try
			{
				return Status.valueOf(upStat);
			}
			catch (IllegalArgumentException e)
			{
				return Status.UNHANDLED;
			}
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

	public int getGid()
	{
		return gid;
	}

	public void setGid(int gid)
	{
		this.gid = gid;
	}

	public String getText()
	{
		return text;
	}

	public void setText(String text)
	{
		this.text = text;
	}

	public long getTime()
	{
		return time;
	}

	public void setTime(long time)
	{
		this.time = time;
	}

	public Status getStatus()
	{
		return status;
	}

	public void setStatus(Status status)
	{
		this.status = status;
	}

	public long getRemindAt()
	{
		return remindAt;
	}

	public void setRemindAt(long remindAt)
	{
		this.remindAt = remindAt;
	}
	
	public String toString()
	{
		return "Notification {id = " + id + ", gid = " + gid + ", text = \"" + text + "\", time = " + time + ", status = " + status + ", remindAt = " + remindAt + "}";
	}
}
