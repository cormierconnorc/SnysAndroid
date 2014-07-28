package com.connorsapps.snys;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.EditText;

public class CreateGroupDialogFragment extends DialogFragment
{
	private Callback mCall;
	
	public CreateGroupDialogFragment(Callback call)
	{
		this.mCall = call;
	}
	
	public Dialog onCreateDialog(Bundle savedInstanceShit)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		builder.setTitle("Enter name for group");
		
		final EditText texty = new EditText(getActivity());
		texty.setHint("Enter name for group");
		
		builder.setView(texty);
		
		builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1)
			{
				String gName = texty.getText().toString();
				mCall.onCreateGroup(gName);
			}
			
		});
		
		builder.setNegativeButton("Cancel", null);
		
		return builder.create();
	}
	
	public static interface Callback
	{
		public void onCreateGroup(String name);
	}
}
