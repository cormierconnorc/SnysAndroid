package com.connorsapps.snys;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class NetworkManager
{
	//Temporary local address of server on my network. You'll need to change this.
	public static final String SERVER = "http://192.168.1.4:8005";
	private Credentials credentials;
	private Gson gson;
	
	/**
	 * Create manager without credentials.
	 * Must set credentials prior to attempting
	 * verified requests
	 */
	public NetworkManager()
	{
		this(null);
	}
	
	/**
	 * Create manager with credentials
	 * @param cred
	 */
	public NetworkManager(Credentials cred)
	{
		this.setCredentials(cred);
		this.gson = new Gson();
	}	
	
	public String doGet(String endpoint, String params) throws IOException
	{
		URL url = new URL(SERVER + endpoint + "?" + params);
		return readInputStream(url.openStream());
	}
	
	public String doPost(String endpoint, String params) throws IOException
	{
		URL url = new URL(SERVER + endpoint);
		
//		Log.d("devBug", "Posting to " + url.toString() + " with params " + params);
		
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("POST");
		con.setDoOutput(true);
		con.setUseCaches(false);
		con.setRequestProperty("Content-Length", String.valueOf(params.length()));
		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		
		DataOutputStream out = new DataOutputStream(con.getOutputStream());
		out.writeBytes(params);
		out.close();

//		int respCode = con.getResponseCode();
//		
//		Log.d("devBug", "Response code was " + respCode);
		
		return readInputStream(con.getInputStream());
	}
	
	private String readInputStream(InputStream in) throws IOException
	{
		BufferedReader read = null;
		
		try
		{
			StringBuilder str = new StringBuilder();
			read = new BufferedReader(new InputStreamReader(in));
		
			char[] buffer = new char[1024 * 16];
			int count;
		
			while ((count = read.read(buffer)) != -1)
				str.append(buffer, 0, count);
		
			return str.toString();
		}
		catch (IOException e)
		{
			throw e;
		}
		finally
		{
			if (read != null)
			{
				read.close();
			}
			else
			{
				in.close();
			}
		}
	}
	
	/**
	 * Register a new email and pass on server
	 * @param email
	 * @param pass
	 * @throws IOException
	 * @throws SnysException: Raised in case of failed account creation
	 * @return New credentials for account
	 */
	public Credentials register(String email, String pass) throws IOException, SnysException
	{
		Credentials nCreds = new Credentials(email, pass);
		String r = this.doPost("/register", nCreds.toQuery());
		GenericResponse resp = gson.fromJson(r, GenericResponse.class);
		
		if (!resp.getError().equals(""))
			throw new SnysException(resp.getError());
		
		return nCreds;
	}

	/**
	 * Check if current credentials are valid
	 * @return Are they?
	 * @throws IOException Hello server? Anyone there?
	 */
	public boolean checkValid() throws IOException
	{
		String resp = this.doGet("/checkValid", this.credentials.toQuery());
		GenericResponse gresp = gson.fromJson(resp, GenericResponse.class);
		Log.d("devBug", gresp.toString());
		
		//If no errors, return true
		return gresp.getError().equals("");
	}
	
	/**
	 * Access server and retrieve all information about user
	 * @param db
	 * @throws IOException
	 */
	public Information getInfo() throws IOException
	{
		String strInfo = this.doGet("/info", this.credentials.toQuery());		
		
//		Log.d("devBug", strInfo);
		
		Information info = new Information();
		
		JsonParser parse = new JsonParser();
		JsonArray array = parse.parse(strInfo).getAsJsonArray();
		
		Iterator<JsonElement> iter = array.iterator();
		
		while (iter.hasNext())
		{
			JsonObject obj = iter.next().getAsJsonObject();
			
			//All values should have tag field, skip if they don't
			if (!obj.has("tag"))
				continue;
			
			String tag = obj.get("tag").getAsString();
			
			switch (tag)
			{
			case "Notification":
				info.pending.add(gson.fromJson(obj, InternalNotification.class).toNotification());
				break;
			case "HandledNotification":
				info.handled.add(gson.fromJson(obj, InternalHandledNotification.class).toNotification());
				break;
			case "Invitation":
				info.invitations.add(gson.fromJson(obj, InternalMembership.class).toGroup(true));
				break;
			case "Membership":
				info.memberships.add(gson.fromJson(obj, InternalMembership.class).toGroup(false));
				break;
			default:
				throw new IOException("Invalid JSON element in server response!");
			
			}
		}
		
		return info;
	}
	
	/**
	 * Get notifications (handled and pending) from server
	 * @return
	 * @throws IOException
	 */
	public Notification[] getNotifications() throws IOException
	{
		String strInfo = this.doGet("/notifications", this.credentials.toQuery());		
		
		Log.d("devBug", strInfo);
		
		JsonParser parse = new JsonParser();
		JsonArray array = parse.parse(strInfo).getAsJsonArray();
		
		Iterator<JsonElement> iter = array.iterator();
		
		List<Notification> notes = new ArrayList<Notification>();
		
		while (iter.hasNext())
		{
			JsonObject obj = iter.next().getAsJsonObject();
			
			//All values should have tag field, skip if they don't
			if (!obj.has("tag"))
				continue;
			
			String tag = obj.get("tag").getAsString();
			
			switch (tag)
			{
			case "Notification":
				notes.add(gson.fromJson(obj, InternalNotification.class).toNotification());
				break;
			case "HandledNotification":
				notes.add(gson.fromJson(obj, InternalHandledNotification.class).toNotification());
				break;
			default:
				throw new IOException("Bad credentials!");
			}
		}
		
		//Move to static array for consistency
		Notification[] notifications = new Notification[notes.size()];
		notes.toArray(notifications);
		return notifications;
	}
	
	private Group[] toGroups(String str, boolean isInvitation)
	{
		InternalMembership[] groups = gson.fromJson(str, InternalMembership[].class);
		
		Group[] rGroups = new Group[groups.length];
		
		for (int i = 0; i < groups.length; i++)
			rGroups[i] = groups[i].toGroup(isInvitation);
		
		return rGroups;
	}
	
	/**
	 * Get the groups this user belongs to from the server
	 * @return
	 * @throws IOException
	 */
	public Group[] getGroups() throws IOException
	{
		String str = this.doGet("/groups", this.credentials.toQuery());
		
		return toGroups(str, false);
	}
	
	/**
	 * Get the groups this user has been invited to from the server
	 * @return
	 * @throws IOException
	 */
	public Group[] getInvitations() throws IOException
	{
		String str = this.doGet("/invitations", this.credentials.toQuery());
		
		return toGroups(str, true);
	}
	
	/**
	 * Create a group and return the object representing it
	 * @param groupname
	 * @return
	 * @throws IOException
	 */
	public Group createGroup(String groupname) throws IOException
	{
		String resp = this.doPost("/createGroup", this.credentials.toQuery() + "&groupname=" + urlEncode(groupname));
		
		return gson.fromJson(resp, InternalMembership.class).toGroup(false);
	}
	
	/**
	 * Try to delete a user
	 * @throws IOException on connection fail
	 * @throws SnysException if deletion fails
	 */
	public void deleteUser() throws IOException, SnysException
	{
		String resp = this.doPost("/deleteUser", this.credentials.toQuery());
		
		throwOnError(resp);
	}
	
	/**
	 * Update user account information
	 * @param newEmail New email. If no change, leave null
	 * @param newPass New pass. Leave null for no change
	 * @throws IOException
	 * @throws SnysException
	 */
	public void updateUser(String newEmail, String newPass) throws IOException, SnysException
	{
		//No need to waste precious data on no change:
		if (newEmail == null && newPass == null)
			return;
		
		String query = this.credentials.toQuery() +
				(newEmail != null ? "&newEmail=" + urlEncode(newEmail) : "") +
				(newPass != null ? "&newPass=" + urlEncode(newPass) : "");
		String resp = this.doPost("/updateUser", query);
		
		throwOnError(resp);
	}
	
	/**
	 * Handle a note on the server
	 * @param nid
	 * @param newStatus: One of All, JustEmail, Hide, Alarm, and NoRemind
	 * @param remindAt: Ignored unless newStatus is All, JustEmail, or Alarm
	 * @return Notification representing the handled notification from the server
	 * @throws IOExceptoin On connection fail
	 * @throws SnysException On generic error. Caused by failure to handle note (which shouldn't happen)
	 */
	public Notification handleNote(int nid, String newStatus, long remindAt) throws IOException, SnysException
	{
		String query = this.credentials.toQuery() +
				"&nid=" + nid +
				"&newStatus=" + urlEncode(newStatus) +
				"&remindAt=" + (remindAt / 1000);
		String resp = this.doPost("/handleNote", query);
		
		throwOnError(resp);
		
		return gson.fromJson(resp, InternalHandledNotification.class).toNotification();
	}
	
	
	/**
	 * Accept an invitation to a group
	 * @param gid
	 * @return the group you are now a member of. Yay for you!
	 * @throws IOException
	 * @throws SnysException On no group returned (incorrect gid, maybe?)
	 */
	public Group acceptInvite(int gid) throws IOException, SnysException
	{
		String query = this.credentials.toQuery() +
				"&gid=" + gid;
		String resp = this.doPost("/acceptInvite", query);
		
		InternalMembership[] groups = gson.fromJson(resp, InternalMembership[].class);
		
		if (groups.length == 0)
			throw new SnysException("Invalid invitation!");
		
		return groups[0].toGroup(false);
	}
	
	/**
	 * Deny an invitation
	 * Note: Does not throw exception on failure, as that
	 * can only happen when the invite did not exist in the
	 * first place. Resulting state is the same!
	 * @param gid
	 * @throws IOException
	 */
	public void denyInvite(int gid) throws IOException
	{
		String query = this.credentials.toQuery() + "&gid=" + gid;
		this.doPost("/denyInvite", query);
	}
	
	/**
	 * Invite a user with given email to the group with given gid
	 * @param gid
	 * @param invite
	 * @param permissions
	 * @throws IOException
	 * @throws SnysException On bad request (permissions too high, for example)
	 */
	public void inviteUser(int gid, String invite, String permissions) throws IOException, SnysException
	{
		String query = this.credentials.toQuery() +
				"&gid=" + gid +
				"&invite=" + urlEncode(invite) +
				"&permissions=" + urlEncode(permissions);
		String resp = this.doPost("/inviteUser", query);
		
		throwOnError(resp);
	}
	
	/**
	 * Leave a group. No exception on fail, since the state is the same 
	 * @param gid
	 * @throws IOException On connect fail
	 */
	public void leaveGroup(int gid) throws IOException
	{
		String query = this.getCredentials().toQuery() + "&gid=" + gid;
		this.doPost("/leaveGroup", query);
	}
	
	/**
	 * Delete a group. Requires Admin permissions!
	 * @param gid
	 * @throws IOException
	 * @throws SnysException
	 */
	public void deleteGroup(int gid) throws IOException, SnysException
	{
		String query = this.getCredentials().toQuery() + "&gid=" + gid;
		throwOnError(this.doPost("/deleteGroup", query));
	}
	
	/**
	 * Create a note
	 * @param gid
	 * @param text
	 * @param time
	 * @return
	 * @throws IOException
	 * @throws SnysException On note creation error
	 */
	public Notification createNote(int gid, String text, long time) throws IOException, SnysException
	{
		String query = this.getCredentials().toQuery() + 
				"&gid=" + gid +
				"&text=" + urlEncode(text) +
				"&time=" + (time / 1000);
		String resp = this.doPost("/createNote", query);
		
		throwOnError(resp);
		
		return gson.fromJson(resp, InternalNotification.class).toNotification();
	}
	
	
	/**
	 * Create and handle a notification with given info
	 * @param gid
	 * @param text
	 * @param time
	 * @param newStatus
	 * @param remindAt
	 * @return Notification created by server
	 * @throws IOException
	 * @throws SnysException
	 */
	public Notification createAndHandleNote(int gid, String text, long time, String newStatus, long remindAt) throws IOException, SnysException
	{
		String query = this.credentials.toQuery() +
				"&gid=" + gid +
				"&text=" + urlEncode(text) +
				"&time=" + (time / 1000) +
				"&newStatus=" + urlEncode(newStatus) +
				"&remindAt=" + (remindAt / 1000);
		String resp = this.doPost("/createAndHandleNote", query);
		
		throwOnError(resp);
		
		return gson.fromJson(resp, InternalHandledNotification.class).toNotification();
	}
	
	/**
	 * Edit a note on the server
	 * @param gid
	 * @param nid
	 * @param text Optional. Set to null to ignore and keep current.
	 * @param time Optional. Set to null to ignore and keep current.
	 * @return Notification after editing
	 * @throws IOException
	 * @throws SnysException On nonexistent note
	 */
	public Notification editNote(int gid, int nid, String text, Long time) throws IOException, SnysException
	{		
		String query = this.credentials.toQuery() +
				"&gid=" + gid +
				"&nid=" + nid +
				(text == null ? "" : "&text=" + urlEncode(text)) +
				(time == null ? "" : "&time=" + (time / 1000));
		String resp = this.doPost("/editNote", query);
		
		throwOnError(resp);
		
		return gson.fromJson(resp, InternalNotification.class).toNotification();
	}
	
	/**
	 * Delete a note on the server. Must at least
	 * be a contributor in the relevant group
	 * @param gid
	 * @param nid
	 * @throws IOException
	 * @throws SnysException on note deletion failure (improper permissions, for example)
	 */
	public void deleteNote(int gid, int nid) throws IOException, SnysException
	{
		String query = this.credentials.toQuery() + 
				"&gid=" + gid +
				"&nid=" + nid;
		
		String resp = this.doPost("/deleteNote", query);
		
		throwOnError(resp);
	}
	
	private void throwOnError(String genericResponseJson) throws SnysException
	{
		String error = toGenericError(genericResponseJson);
		
		if (error != null && !error.equals(""))
			throw new SnysException(error);
	}
	
	private String toGenericError(String genericResponseJson)
	{
		try
		{
			return gson.fromJson(genericResponseJson, GenericResponse.class).getError();
		}
		catch (JsonSyntaxException e)
		{
			return null;
		}
	}
	
	/**
	 * Shortcut to use url encoder in utf-8 mode
	 * @param val
	 * @return
	 */
	private static String urlEncode(String val)
	{
		try
		{
			return URLEncoder.encode(val, "utf-8");
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public Credentials getCredentials()
	{
		return credentials;
	}

	public void setCredentials(Credentials credentials)
	{
		this.credentials = credentials;
	}
	
	/**
	 * Generic response sent by server.
	 * @author connor
	 *
	 */
	private static class GenericResponse
	{
		private String error, response;

		public String getError()
		{
			return error;
		}
		
		public String toString()
		{
			return "GenericResponse {error = \"" + error + "\", response = \"" + response + "\"}";
		}
	}
	
	/**
	 * Internal representation of a group
	 * Used for JSON deserialization
	 * @author connor
	 *
	 */
	private static class InternalGroup
	{
		private int gid;
		private String groupname;
	}
	
	/**
	 * Internal representation of a membership,
	 * used in JSON deserialization. Converted
	 * to a Group.
	 * @author connor
	 *
	 */
	private static class InternalMembership
	{
		private InternalGroup group;
		private String permissions;
		
		public Group toGroup(boolean isInvitation)
		{
			return new Group(group.gid,
					group.groupname,
					permissions,
					isInvitation);
		}
	}
	
	/**
	 * Internal representation of (unhandled) notification.
	 * Converted to a Notification object.
	 * @author connor
	 *
	 */
	private static class InternalNotification
	{
		private int nid;
		private int associatedGid;
		private String text;
		private long time;

		public Notification toNotification()
		{
			return new Notification(nid,
					associatedGid,
					text,
					time * 1000,
					Notification.Status.UNHANDLED,
					0);
		}
	}
	
	/**
	 * Internal representation of a handled notification.
	 * Converted to a notification before leaving this class
	 * @author connor
	 *
	 */
	private static class InternalHandledNotification
	{
		private InternalNotification notification;
		private String status;
		private long remindAt;

		/**
		 * Get the external representation of this
		 * object.
		 * @return
		 */
		public Notification toNotification()
		{
			return new Notification(notification.nid,
					notification.associatedGid,
					notification.text,
					notification.time * 1000,
					this.status,
					this.remindAt * 1000);
		}
	}
	
	/**
	 * A class used to group together information query values.
	 * @author connor
	 *
	 */
	public static class Information
	{
		public final List<Group> memberships, invitations;
		public final List<Notification> pending, handled;
		
		public Information()
		{
			this.memberships = new ArrayList<Group>();
			this.invitations = new ArrayList<Group>();
			this.handled = new ArrayList<Notification>();
			this.pending = new ArrayList<Notification>();
		}
		
		public String toString()
		{
			return "Information {memberships = " + memberships + ", invitations = " + invitations + ", handled = " + handled + ", pending = " + pending + "}";
		}
	}
}
