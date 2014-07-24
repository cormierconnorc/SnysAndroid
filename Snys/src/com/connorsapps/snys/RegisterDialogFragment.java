package com.connorsapps.snys;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

/**
 * Handle registration
 * @author connor
 *
 */
public class RegisterDialogFragment extends DialogFragment
{
	private MainActivity callback;
	
	public RegisterDialogFragment(MainActivity callback)
	{
		this.callback = callback;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
		
		builder.setTitle("Please register");
		
		LayoutInflater inflate = this.getActivity().getLayoutInflater();
		
		View root = inflate.inflate(R.layout.register_dialog, null);
		final EditText emailInput = (EditText)root.findViewById(R.id.new_email);
		final EditText passInput = (EditText)root.findViewById(R.id.new_pass);
		final EditText confirmPassInput = (EditText)root.findViewById(R.id.confirm_pass);
		
		builder.setView(root);
		
		//Set up actual listener below
		builder.setPositiveButton("Register", null);
		
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.cancel();
			}
		});
		
		
		AlertDialog dial = builder.create();
		
		
		dial.setOnShowListener(new DialogInterface.OnShowListener()
		{
			
			@Override
			public void onShow(DialogInterface dialog)
			{
				((AlertDialog)dialog).getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						String email = emailInput.getText().toString();
						String pass = passInput.getText().toString();
						String confirm = confirmPassInput.getText().toString();
						
						if (pass.equals(confirm))
						{
							callback.onRegister(email, pass);
							RegisterDialogFragment.this.dismiss();
						}
						else
						{
							//Indicate mismatch
							passInput.setBackgroundColor(Color.RED);
							confirmPassInput.setBackgroundColor(Color.RED);
						}
					}
				});
			}
		});
	
		return dial;
		
	}
	
	@Override
	public void onCancel(DialogInterface dialog)
	{
		super.onCancel(dialog);
		
		//Close app on cancel
		callback.tryLogin();
	}
}
