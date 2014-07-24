package com.connorsapps.snys;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class LoginDialogFragment extends DialogFragment
{
	private MainActivity callback;
	private String title;
	private boolean closeOnDismiss;
	
	public LoginDialogFragment(MainActivity callback, String title)
	{
		this.callback = callback;
		this.title = title;
		this.closeOnDismiss = true;
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
		final Button register = (Button)root.findViewById(R.id.reg_button);
		
		register.setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				//Dismiss this dialog and open up the registration one
				closeOnDismiss = false;
				LoginDialogFragment.this.dismiss();
				callback.createRegistrationDialog();
			}
		});
		
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
		
		builder.setNegativeButton("Quit", new DialogInterface.OnClickListener()
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
		if (closeOnDismiss)
			callback.finish();
	}
}
