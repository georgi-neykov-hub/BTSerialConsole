package com.neykov.bluetoothserialconsole.fragments;

import com.neykov.bluetoothserialconsole.R;
import com.neykov.bluetoothserialconsole.R.id;
import com.neykov.bluetoothserialconsole.R.layout;

import android.app.Activity;
import android.app.ListFragment;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * A simple {@link android.support.v4.app.Fragment} subclass. Activities that contain this fragment must implement the
 * {@link DataLogListFragment.OnFragmentInteractionListener} interface to handle interaction events. Use the
 * {@link DataLogListFragment#newInstance} factory method to create an instance of this fragment.
 * 
 */
public class DataLogListFragment extends ListFragment
{
	public static final String TAG = "DataLogListFragment";
	
	private static final String ARGS_BLUETOOTH_DEVICES = "device names";

	private DataLogListListener mListener;
	private DataLogListAdapter mDataLogListAdapter;
	
	/**
	 * Use this factory method to create a new instance of this fragment using the provided parameters.
	 * 
	 * @param param1
	 *            Parameter 1.
	 * @param param2
	 *            Parameter 2.
	 * @return A new instance of fragment DataLogListFragment.
	 */
	// TODO: Rename and change types and number of parameters
	public static DataLogListFragment newInstance(BluetoothDevice[] dataLogDevices)
	{
		DataLogListFragment fragment = new DataLogListFragment();
		Bundle args = new Bundle();
		args.putSerializable(ARGS_BLUETOOTH_DEVICES,dataLogDevices);
		fragment.setArguments(args);
		return fragment;
	}

	public DataLogListFragment()
	{
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.setRetainInstance(true);
		
		this.mDataLogListAdapter = new DataLogListAdapter(getActivity());
		
		Bundle args = getArguments();
		if (args != null)
		{
			BluetoothDevice[] devices = (BluetoothDevice[]) args.getSerializable(ARGS_BLUETOOTH_DEVICES);
			mDataLogListAdapter.addAll(devices);
		}
		
		this.setListAdapter(mDataLogListAdapter);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_data_log_list, container, false);
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		try
		{
			mListener = (DataLogListListener) activity;
		} catch (ClassCastException e)
		{
			throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach()
	{
		super.onDetach();
		mListener = null;
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		this.mDataLogListAdapter.clear();
	}
	
	public void setListItems(BluetoothDevice[] devices)
	{
		if(devices==null)
		{
			throw new NullPointerException();
		}
		mDataLogListAdapter.clear();
		mDataLogListAdapter.addAll(devices);
	}
	
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		mListener.onDataLogListSelected(((BluetoothDevice)l.getItemAtPosition(position)).getAddress());
	}

	/**
	 * This interface must be implemented by activities that contain this fragment to allow an interaction in this
	 * fragment to be communicated to the activity and potentially other fragments contained in that activity.
	 * <p>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html" >Communicating with Other
	 * Fragments</a> for more information.
	 */
	public interface DataLogListListener
	{
		public void onDataLogListSelected(String deviceId);
	}

	private class DataLogListAdapter extends ArrayAdapter<BluetoothDevice>
	{
		private static final int ID_ROW_LAYOUT = R.layout.data_log_list_item;
		private static final int ID_TEXT_NAME = R.id.datalog_list_item_name;
		private static final int ID_TEXT_ADRESS = R.id.datalog_list_item_adress;
				
		public DataLogListAdapter(Context context)
		{
			super(context, ID_ROW_LAYOUT);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View rowView = convertView;
			if (rowView == null)
			{
				LayoutInflater inflater =  (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				rowView = inflater.inflate(ID_ROW_LAYOUT,parent,false);
			}
			
			// Set text of views.
			((TextView) rowView.findViewById(ID_TEXT_NAME)).setText(getItem(position).getName());
			((TextView) rowView.findViewById(ID_TEXT_ADRESS)).setText(getItem(position).getAddress());
			
			return rowView;
		}
		
	}
	
}
