package com.neykov.bluetoothserialconsole.fragments;

import com.neykov.bluetoothserialconsole.R;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link android.support.v4.app.Fragment} subclass. Use the {@link NoConnectionsFragment#newInstance} factory
 * method to create an instance of this fragment.
 * 
 */
public class NoConnectionsFragment extends Fragment
{
	public static final String TAG = "NoConnectionsFragment";
	
	/**
	 * Use this factory method to create a new instance of this fragment using the provided parameters.
	 * 
	 * @param param1
	 *            Parameter 1.
	 * @param param2
	 *            Parameter 2.
	 * @return A new instance of fragment NoConnectionsFragment.
	 */
	// TODO: Rename and change types and number of parameters
	public static NoConnectionsFragment newInstance()
	{
		NoConnectionsFragment fragment = new NoConnectionsFragment();
		Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

	public NoConnectionsFragment()
	{
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_no_connections, container, false);
	}

}
