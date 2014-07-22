package com.connorsapps.snys;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Credentials used for user-verified requests
 * @author connor
 *
 */
public class Credentials
{
	private String email;
	private String pass;
	
	public Credentials(String email, String pass)
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
	
	public String toQuery()
	{
		try
		{
			return "email=" + URLEncoder.encode(email, "utf-8") + "&pass=" + URLEncoder.encode(pass, "utf-8");
		} 
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public String toString()
	{
		return "Credentials {email = \"" + email + "\",pass = \"" + pass + "\"}";
	}
}
