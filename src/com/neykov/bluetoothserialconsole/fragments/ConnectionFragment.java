package com.neykov.bluetoothserialconsole.fragments;

import com.neykov.bluetoothserialconsole.R;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import com.neykov.bluetoothserialconsole.fragments.DeviceListAdapter.DeviceListItem;

/**
 * A simple {@link android.app.Fragment} subclass. Activities that contain this fragment must implement the
 * {@link ConnectionFragment.OnDeviceListFragmentListener} interface to handle interaction events. Use the
 * {@link ConnectionFragment#newInstance} factory method to create an instance of this fragment.
 */
public class ConnectionFragment extends Fragment
{
	public static final String TAG = "DeviceListFragment";

	private static final int DEFAULT_DISCOVERY_TIME = 60;
	private static final IntentFilter BLUETOOTH_ADAPTER_INTENT_FILTER = new IntentFilter()
	{
		{
			// Bluetooth related actions.
			addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
			addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
			addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
			addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
			addAction(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED);

		}
	};
	private static final IntentFilter BLUETOOTH_DEVICE_INTENT_FILTER = new IntentFilter()
	{
		{
			// Bluetooth related actions.
			addAction(BluetoothDevice.ACTION_FOUND);
			//addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
			//addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
			addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		}
	};
	
	private final BroadcastReceiver bluetoothDeviceIntentReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			// Get the action and the BluetoothDevice object from the Intent
			String action = intent.getAction();
			BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			
			if (BluetoothDevice.ACTION_FOUND.equals(action))
			{
				onRemoteDeviceFound(device);
			}
			else if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(action))
			{
				//onIncomingRequest(device);
			}
			else if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action))
			{
				//onIncomingRequestCanceled(device);
			}
			else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action))
			{
				int currState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
				int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
				onDeviceBondStateChanged(device, currState, prevState);
			}
		}
	};
	
	private final BroadcastReceiver bluetoothIntentReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action))
			{
				int currState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
				int prevState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, 0);
				onBluetoothStateChanged(currState, prevState);
			}
			else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
			{
				onDiscoveryStarted();
			}
			else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
			{
				onDiscoveryFinished();
			}
			else if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action))
			{
				int prevState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, -1);
				int currState = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1);
				onDiscoverabilityChanged(currState, prevState);
			}
			else if (BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED.equals(action))
			{
				String newName = intent.getStringExtra(BluetoothAdapter.EXTRA_LOCAL_NAME);
				onLocalNameChanged(newName);
			}
		}
	};
	
	private final OnItemClickListener mDeviceClickListener = new OnItemClickListener()
	{
		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			ListAdapter adapter = devicesListView.getAdapter();
			int viewCount = adapter.getCount();
			
			
			//Check if the footer view was clicked and act accordingly.
			if(position==(viewCount - 1))
			{
				onRefreshClicked();
				return;
			}

			DeviceListItem item  = (DeviceListAdapter.DeviceListItem)adapter.getItem(position);
			BluetoothDevice device = item.getDevice();
			onDeviceListItemSelected(device , item.isIncoming(), item.isSecure());
		}
	};
	private final OnCheckedChangeListener bluetoothSwitchCheckedListener = new OnCheckedChangeListener()
	{
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			if (isChecked)
			{
				mBtAdapter.enable();
			}
			else
			{
				mBtAdapter.cancelDiscovery();
				mBtAdapter.disable();
			}
		}
	};
	private final OnCheckedChangeListener discoverabilityOnCheckedChangeListener = new OnCheckedChangeListener()
	{
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			if(isChecked) makeDiscoverable(DEFAULT_DISCOVERY_TIME);
		}
	};
	private final Runnable ScrollDownRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			devicesListView.smoothScrollToPosition(devicesListView.getCount()-1);
		}
	};
	// Member fields.

	private BluetoothAdapter mBtAdapter;
	private DeviceListAdapter deviceAdapter;

	private ConnectionFragmentListener mListener;

	private ListView devicesListView;
	private Switch bluetoothSwitch;
	private TextView bluetoothStatusView;
	private Switch discoverableSwitch;
	private TextView discoverableStatusView;
	private TextView localNameTextView;
	private TextView localAdressTextView;
	private View refreshDevicesLayout;
	private TextView refreshDevicesTextView;
	private ProgressBar refreshDevicesProgressBar;

	public static ConnectionFragment newInstance()
	{
		ConnectionFragment fragment = new ConnectionFragment();
		return fragment;
	}

	public ConnectionFragment()
	{
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.setHasOptionsMenu(true);
		this.setRetainInstance(true);
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();

		// Initialize array adapters.
		deviceAdapter = new DeviceListAdapter(getActivity(), R.layout.connection_list_item, R.id.device_list_item_name,
				R.id.device_list_item_adress, R.id.device_list_item_icon, R.id.device_list_item_icon_bond, R.id.device_list_item_icon_secure);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// Inflate layouts and get View for main container.
		View fragmentLayout = inflater.inflate(R.layout.fragment_connection_list, container, false);
		View homeLayout = inflater.inflate(R.layout.connection_list_home, null, false);
		refreshDevicesLayout = inflater.inflate(R.layout.connection_list_refresh_button, null);

		devicesListView = (ListView) fragmentLayout.findViewById(R.id.device_list_devices);
		devicesListView.setEmptyView(fragmentLayout.findViewById(R.id.device_list_devices_empty));
		devicesListView.addHeaderView(homeLayout, null, false);
		devicesListView.addFooterView(refreshDevicesLayout);
		devicesListView.setOnItemClickListener(mDeviceClickListener);
		devicesListView.setAdapter(deviceAdapter);

		// Get the views for bluetooth on/off control.
		bluetoothSwitch = (Switch) fragmentLayout.findViewById(R.id.device_list_bt_switch);
		bluetoothStatusView = (TextView) fragmentLayout.findViewById(R.id.device_list_bt_status);
		bluetoothSwitch.setOnCheckedChangeListener(bluetoothSwitchCheckedListener);

		// Get the views from the home device layout and initialize them.
		localAdressTextView = (TextView) homeLayout.findViewById(R.id.device_list_home_adress);
		localAdressTextView.setText(mBtAdapter.getAddress());
		localNameTextView = (TextView) homeLayout.findViewById(R.id.device_list_home_name);

		discoverableSwitch = (Switch) homeLayout.findViewById(R.id.device_list_discoverable_switch);
		discoverableStatusView = (TextView) homeLayout.findViewById(R.id.device_list_discoverable_status);
		discoverableSwitch.setOnCheckedChangeListener(discoverabilityOnCheckedChangeListener);

		refreshDevicesTextView = (TextView) refreshDevicesLayout.findViewById(R.id.device_list_refresh_label);
		refreshDevicesProgressBar = (ProgressBar) refreshDevicesLayout.findViewById(R.id.device_list_refresh_spinner);
		
		return fragmentLayout;
	}

	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		try
		{
			mListener = (ConnectionFragmentListener) activity;
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
	public void onResume()
	{
		super.onResume();
		
		//Refresh state of views.
		onLocalNameChanged(mBtAdapter.getName());
		
		if (mBtAdapter.isEnabled())	
			onBluetoothStateChanged(BluetoothAdapter.STATE_ON, -1);
		else		    			
			onBluetoothStateChanged(BluetoothAdapter.STATE_OFF, -1);
		
		onDiscoverabilityChanged(mBtAdapter.getScanMode(), -1);
		if (mBtAdapter.isDiscovering())
			onDiscoveryStarted();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		
	}

	public void onStart()
	{
		super.onStart();
		getActivity().registerReceiver(bluetoothIntentReceiver, BLUETOOTH_ADAPTER_INTENT_FILTER);
		getActivity().registerReceiver(bluetoothDeviceIntentReceiver, BLUETOOTH_DEVICE_INTENT_FILTER);
	}
	
	@Override
	public void onStop()
	{
		super.onStop();
		getActivity().unregisterReceiver(bluetoothIntentReceiver);
		getActivity().unregisterReceiver(bluetoothDeviceIntentReceiver);
	}
	
	
	
	// Start device discover with the BluetoothAdapter
	private void startDiscovery()
	{
		// If we're already discovering, stop it
		if (mBtAdapter.isDiscovering())
		{
			finishDiscovery();
		}
		mBtAdapter.startDiscovery();
	}

	private void finishDiscovery()
	{
		mBtAdapter.cancelDiscovery();
	}

	private void makeDiscoverable(int periodSecs)
	{
		Log.d(TAG, "makeDiscoverable() called.");
		Intent intent = new Intent();
		intent.setAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).putExtra(
				BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, periodSecs);
		getActivity().sendBroadcast(intent);
	}

	private void refreshDevices()
	{
		deviceAdapter.clear();
		if(mBtAdapter.isEnabled())
		{
			deviceAdapter.addBondedDevices(mBtAdapter.getBondedDevices());
		}
	}

	/*
	 * **********************| Callback Methods |****************************
	 */
	
	public void onRemoteDeviceFound(BluetoothDevice device)
	{
		boolean isBonded = (device.getBondState() == BluetoothDevice.BOND_BONDED)? true: false;
		
		DeviceListItem item = deviceAdapter.new DeviceListItem(device, false, isBonded, false);
		deviceAdapter.add(item);
		devicesListView.post(ScrollDownRunnable);
	}

	public void onIncomingRequest(BluetoothDevice device)
	{
		Log.d(TAG, "Incoming connection request:\n"+device.toString());
		
		boolean isBonded = (device.getBondState() == BluetoothDevice.BOND_BONDED)? true: false;
		
		DeviceListItem item = deviceAdapter.new DeviceListItem(device, true, isBonded, false);
		deviceAdapter.add(item);
		devicesListView.post(ScrollDownRunnable);
	}

	public void onRefreshClicked()
	{
		if (!mBtAdapter.isDiscovering())
		{
			startDiscovery();
			refreshDevices();
		}
		mListener.onConnFragmentRefresh();
	}
	

	public void onDeviceBondStateChanged(BluetoothDevice device, int currState, int prevState)
	{
		//TODO:
	}
	
	public void onDeviceListItemSelected(BluetoothDevice device, boolean incoming, boolean isSecure)
	{
		mListener.onConnFragmentDeviceSelected(device, incoming);
	}
	
 	public void onDiscoveryFinished()
	{
		refreshDevicesTextView.setText(R.string.device_list_refresh);
		refreshDevicesTextView.setEnabled(true);
		refreshDevicesProgressBar.setVisibility(View.GONE);
	}

	public void onDiscoveryStarted()
	{
		refreshDevicesTextView.setText(R.string.device_list_refresh_running);
		refreshDevicesTextView.setEnabled(false);
		refreshDevicesProgressBar.setVisibility(View.VISIBLE);
		devicesListView.post(ScrollDownRunnable);
	}

	public void onDiscoverabilityChanged(int currState, int prevState)
	{
		discoverableSwitch.setOnCheckedChangeListener(null);
		switch (currState)
		{
			case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
				discoverableStatusView.setText(R.string.device_list_discoverability_status_discoverable);
				discoverableSwitch.setEnabled(false);
				discoverableSwitch.setChecked(true);
				break;
			case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
			case BluetoothAdapter.SCAN_MODE_NONE:
				discoverableStatusView.setText(R.string.device_list_discoverability_status);
				discoverableSwitch.setEnabled(true);
				discoverableSwitch.setChecked(false);
				break;
			default:
				throw new IllegalArgumentException();
		}
		discoverableSwitch.setOnCheckedChangeListener(discoverabilityOnCheckedChangeListener);
	}

	public void onLocalNameChanged(String newName)
	{
		localNameTextView.setText(newName);
	}

	public void onBluetoothStateChanged(int currState, int prevState)
	{
		Log.d(TAG, String.format("onBluetoothStateChanged() called States: %1s ---> %2s ", prevState, currState));
		bluetoothSwitch.setOnCheckedChangeListener(null);
		switch (currState)
		{
			case BluetoothAdapter.STATE_OFF:
				refreshDevices();
				bluetoothStatusView.setText(R.string.device_list_bluetooth_disabled);
				bluetoothSwitch.setEnabled(true);
				bluetoothSwitch.setChecked(false);
				break;
			case BluetoothAdapter.STATE_TURNING_ON:
				bluetoothSwitch.setEnabled(false);
				bluetoothStatusView.setText(R.string.device_list_bluetooth_enabling);
				break;
			case BluetoothAdapter.STATE_ON:
				refreshDevices();
				bluetoothStatusView.setText(R.string.device_list_bluetooth_enabled);
				bluetoothSwitch.setEnabled(true);
				bluetoothSwitch.setChecked(true);
				break;
			case BluetoothAdapter.STATE_TURNING_OFF:
				bluetoothStatusView.setEnabled(false);
				bluetoothStatusView.setText(R.string.device_list_bluetooth_disabling);
				break;
		}
		bluetoothSwitch.setOnCheckedChangeListener(bluetoothSwitchCheckedListener);
	}

	/**
	 * This interface must be implemented by activities that contain this fragment to allow an interaction in this
	 * fragment to be communicated to the activity and potentially other fragments contained in that activity.
	 * <p>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html" >Communicating with Other
	 * Fragments</a> for more information.
	 */
	public interface ConnectionFragmentListener
	{
		public void onConnFragmentDeviceSelected(BluetoothDevice device, boolean incoming);
	
		public void onConnFragmentRefresh();
	}

	
}
