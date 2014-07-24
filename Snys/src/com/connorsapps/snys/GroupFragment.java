package com.connorsapps.snys;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class GroupFragment extends Fragment
{
	private List<Object> data;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved)
	{
		View root = inflater.inflate(R.layout.fragment_group, container, false);
		
		//Set the adapter for the listview
		GroupAdapter ada = new GroupAdapter(root.getContext(), data);
		ListView list = (ListView)root.findViewById(R.id.sexy_group_view);
		list.setAdapter(ada);
		
		return root;
	}
	
	public void setListData(List<Object> data)
	{
		this.data = data;
	}
	
	public static class GroupAdapter extends ArrayAdapter<Object>
	{
		private static final int TYPE_STRING = 0, TYPE_GROUP = 1;
		
		public GroupAdapter(Context context, List<Object> objects)
		{
			super(context, 0, objects);
		}	
		
		/**
		 * Two different types of view: String titles and Group rows
		 */
		@Override
		public int getViewTypeCount()
		{
			return 2;
		}
		
		/**
		 * @param pos
		 * @return 0 if String title, 1 if group row
		 */
		@Override
		public int getItemViewType(int pos)
		{
			return this.getItem(pos) instanceof String ? TYPE_STRING : TYPE_GROUP;
		}
		
		@Override
		public View getView(int pos, View convertView, ViewGroup parent)
		{
			Object itm = this.getItem(pos);
			int type = getItemViewType(pos);
			
			if (convertView == null)
			{
				convertView = inflateByType(type);
			}
			
			if (type == TYPE_STRING)
			{
				((TextView)convertView).setText((String)itm);
			}
			else
				setupGroupView((TextView)convertView, (Group)itm);
			
			return convertView;
		}
		
		private void setupGroupView(TextView convertView, Group g)
		{
			convertView.setText(g.getGroupname());
		}
		
		private View inflateByType(int type)
		{
			switch (type)
			{
			case TYPE_STRING:
				TextView nView = new TextView(getContext());
				nView.setBackgroundColor(Color.RED);
				return nView;
			case TYPE_GROUP:
				return new TextView(getContext());
			default:
				return null;
			}
		}
	}
}
