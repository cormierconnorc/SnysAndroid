package com.connorsapps.snys;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class GenericDialogFragment extends DialogFragment
{
	private String title, msg, button;
	private int icon;
	
	public GenericDialogFragment(String title, String msg, int icon, 
			String button)
	{
		this.title = title;
		this.msg = msg;
		this.icon = icon;
		this.button = button;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		builder.setTitle(title);
		
		builder.setMessage(msg);
		
		builder.setIcon(icon);
		
		if (button != null && !button.equals(""))
			builder.setPositiveButton(button, null);
		
		return builder.create();
	}
}
