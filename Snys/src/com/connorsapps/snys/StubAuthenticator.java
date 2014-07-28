package com.connorsapps.snys;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.os.Bundle;

public class StubAuthenticator extends AbstractAccountAuthenticator
{

	public StubAuthenticator(Context context)
	{
		super(context);
	}

	@Override
	public Bundle addAccount(AccountAuthenticatorResponse arg0, String arg1,
			String arg2, String[] arg3, Bundle arg4)
			throws NetworkErrorException
	{
		return null;
	}

	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse arg0,
			Account arg1, Bundle arg2) throws NetworkErrorException
	{
		return null;
	}

	@Override
	public Bundle editProperties(AccountAuthenticatorResponse arg0, String arg1)
	{
		return null;
	}

	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse arg0, Account arg1,
			String arg2, Bundle arg3) throws NetworkErrorException
	{
		return null;
	}

	@Override
	public String getAuthTokenLabel(String arg0)
	{
		return null;
	}

	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse arg0, Account arg1,
			String[] arg2) throws NetworkErrorException
	{
		return null;
	}

	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse arg0,
			Account arg1, String arg2, Bundle arg3)
			throws NetworkErrorException
	{
		return null;
	}

}
