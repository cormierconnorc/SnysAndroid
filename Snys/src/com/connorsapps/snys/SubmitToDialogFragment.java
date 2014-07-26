package com.connorsapps.snys;

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class SubmitToDialogFragment extends DialogFragment
{
	private final List<Group> groups;
	
	public SubmitToDialogFragment(List<Group> groups)
	{
		this.groups = groups;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle saved)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		builder.setTitle("Select a group to submit to:");
		
		String[] gnames = new String[groups.size()];
		int i = 0;
		for (Group g : groups)
			gnames[i++] = g.getGroupname();
		
		builder.setItems(gnames, new OnClickListener() 
		{
			@Override
			public void onClick(DialogInterface dialog, int pos)
			{
				//Transition to the noteactivity
				NoteActivity.transitionToNewNote(getActivity(), groups.get(pos));
			}
			
		});
		
		return builder.create();
	}
}
