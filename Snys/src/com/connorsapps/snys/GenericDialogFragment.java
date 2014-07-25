package com.connorsapps.snys;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class GenericDialogFragment extends DialogFragment
{
	public static final int NO_ICON = -1;
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
		
		if (title != null && !title.equals(""))
			builder.setTitle(title);
		
		if (msg != null && !msg.equals(""))
			builder.setMessage(msg);
		
		if (icon != NO_ICON)
			builder.setIcon(icon);
		
		if (button != null && !button.equals(""))
			builder.setPositiveButton(button, null);
		
		return builder.create();
	}
}
