package com.connorsapps.snys;

import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import android.os.Parcel;
import android.os.Parcelable;

public class Notification implements Parcelable
{
	public static enum Status {ALL, JUST_EMAIL, HIDE, ALARM, NO_REMIND, UNHANDLED};
	public static final Parcelable.Creator<Notification> CREATOR =
			new Parcelable.Creator<Notification>()
			{
				@Override
				public Notification createFromParcel(Parcel source)
				{
					return new Notification(source);
				}

				@Override
				public Notification[] newArray(int size)
				{
					return new Notification[size];
				}
		
			};
	
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
	
	public Notification(Parcel in)
	{
		this(in.readInt(),
			in.readInt(),
			in.readString(),
			in.readLong(),
			(Status)in.readSerializable(),
			in.readLong());
	}
	
	@Override
	public int describeContents()
	{
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeInt(id);
		dest.writeInt(gid);
		dest.writeString(text);
		dest.writeLong(time);
		dest.writeSerializable(status);
		dest.writeLong(remindAt);
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
	
	public static String getFormattedTime(long time, boolean longFormat)
	{
		String format = (longFormat ? "h:mm a 'on' EEE, M/dd/yyyy" : "h:mm a 'on' M/dd/yyyy");
		return new SimpleDateFormat(format, Locale.getDefault()).format(new Date(time));
	}
	
	public static String getFormattedTime(long time)
	{
		return getFormattedTime(time, false);
	}
	
	public static long fromFormattedTime(String time, boolean longFormat)
	{
		String format = (longFormat ? "h:mm a 'on' EEE, M/dd/yyyy" : "h:mm a 'on' M/dd/yyyy");
		
		try
		{
			return new SimpleDateFormat(format, Locale.getDefault()).parse(time).getTime();
		} 
		catch (ParseException e)
		{
			e.printStackTrace();
			return -1;
		}
	}
	
	public static long fromFromattedTime(String time)
	{
		return fromFormattedTime(time, false);
	}

	public void setTime(long time)
	{
		this.time = time;
	}

	public Status getStatus()
	{
		return status;
	}
	
	/**
	 * Get the status in the server's format
	 * @return
	 */
	public String getServerStatus()
	{
		switch (status)
		{
		case ALL:
			return "All";
		case JUST_EMAIL:
			return "JustEmail";
		case HIDE:
			return "Hide";
		case ALARM:
			return "Alarm";
		case NO_REMIND:			
			return "NoRemind";
		default:
			throw new RuntimeException("Cannot send UNHANDLED to server!");
		}
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
	
	/**
	 * Determine equality solely by id
	 */
	public boolean equals(Object other)
	{
		return other instanceof Notification && this.id == ((Notification)other).id;
	}
}
