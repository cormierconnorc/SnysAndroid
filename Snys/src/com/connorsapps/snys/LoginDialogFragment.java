package com.connorsapps.snys;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class LoginDialogFragment extends DialogFragment
{
	private MainActivity callback;
	private String title;
	
	public LoginDialogFragment(MainActivity callback, String title)
	{
		this.callback = callback;
		this.title = title;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		builder.setTitle(title);
		
		LayoutInflater inflater = getActivity().getLayoutInflater();
		
		//Load the view
		View root = inflater.inflate(R.layout.login_dialog, null);
		final EditText emailInput = (EditText)root.findViewById(R.id.email);
		final EditText passInput = (EditText)root.findViewById(R.id.password);
		builder.setView(root);
		
		//If login credentials exist, fill in the fields
		NetworkManager netMan = callback.getNetworkManager();
		if (netMan.getCredentials() != null)
		{
			emailInput.setText(netMan.getCredentials().getEmail());
			passInput.setText(netMan.getCredentials().getPass());
		}
		
		builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int id)
			{
				String email = emailInput.getText().toString();
				String pass = passInput.getText().toString();
				
				callback.onUpdateCredentials(email, pass);
			}
			
		});
		
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
		{
			
			@Override
			public void onClick(DialogInterface dialogo, int arg1)
			{
				dialogo.cancel();
			}
		});
		
		return builder.create();
	}
	
	@Override
	public void onCancel(DialogInterface dialog)
	{
		super.onCancel(dialog);
		
		//And close the app
		callback.finish();
	}
}
