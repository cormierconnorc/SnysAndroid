package com.connorsapps.snys;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;

import com.google.gson.Gson;

public class NetworkManager
{
	//Temporary local address of server on my network. You'll need to change this.
	public static final String SERVER = "http://192.168.43.110:8005";
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
		
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("POST");
		con.setDoOutput(true);
		con.setUseCaches(false);
		con.setRequestProperty("Content-Length", String.valueOf(params.length()));
		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		
		DataOutputStream out = new DataOutputStream(con.getOutputStream());
		out.writeBytes(params);
		out.close();
		
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
	 * Check if current credentials are valid
	 */
	public boolean checkValid() throws IOException
	{
		String resp = this.doGet("/checkValid", this.credentials.toString());
		GenericResponse gresp = gson.fromJson(resp, GenericResponse.class);
		Log.d("devBug", gresp.toString());
		
		//If no errors, return true
		return gresp.getError().equals("");
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
	 * Login credentials. Used in all verified requests
	 * @author connor
	 *
	 */
	public static class Credentials
	{
		private String email, pass;
		
		Credentials(String email, String pass)
		{
			setEmail(email);
			setPass(pass);
		}

		public String getEmail()
		{
			return email;
		}

		public void setEmail(String email)
		{
			this.email = email;
		}

		public String getPass()
		{
			return pass;
		}

		public void setPass(String pass)
		{
			this.pass = pass;
		}
		
		public String toString()
		{
			return "email=" + email + "&pass=" + pass;
		}
	}
	
	/**
	 * Generic response sent by server.
	 * @author connor
	 *
	 */
	public static class GenericResponse
	{
		private String error, response;

		public String getError()
		{
			return error;
		}

		public void setError(String error)
		{
			this.error = error;
		}

		public String getResponse()
		{
			return response;
		}

		public void setResponse(String response)
		{
			this.response = response;
		}
		
		public String toString()
		{
			return "GenericResponse {error = \"" + error + "\", response = \"" + response + "\"}";
		}
	}
}
