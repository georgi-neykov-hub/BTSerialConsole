package com.neykov.bluetoothserialconsole;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.neykov.bluetoothserialconsole.connection.BluetoothDriver;
import com.neykov.bluetoothserialconsole.datalog.EEntryType;
import com.neykov.bluetoothserialconsole.dialogs.ConnectWaitDialogFragment;
import com.neykov.bluetoothserialconsole.fragments.DataLogFragment;
import com.neykov.bluetoothserialconsole.fragments.ConnectionFragment;
import com.neykov.bluetoothserialconsole.fragments.DataLogListFragment;
import com.neykov.bluetoothserialconsole.fragments.NoConnectionsFragment;
import com.neykov.bluetoothserialconsole.fragments.DataLogFragment.OnDataLogFragmentInteractionListener;
import com.neykov.bluetoothserialconsole.fragments.DataLogListFragment.DataLogListListener;
import com.neykov.bluetoothserialconsole.fragments.ConnectionFragment.ConnectionFragmentListener;
import com.neykov.bluetoothserialconsole.services.BluetoothSerialService;
import com.neykov.bluetoothserialconsole.services.SerialConnectionService;
import com.neykov.bluetoothserialconsole.services.SerialConnectionServiceBinder;

public class MainActivity extends Activity implements OnDataLogFragmentInteractionListener, ConnectionFragmentListener, DataLogListListener
{
	private static final String TAG = "MainActivity";

	public static final int CONNECT_TO_DEVICE_REQUEST = 1000;
	private static final int CONNECTION_PROGRESS_HIDE_DELAY = 1000;

	public static final String EXTRA_DEVICE_ADRESS = "device adress";
	public static final String EXTRA_HANDLER = "handler";
	public static final String ACTIVITY_ID = "activity_id";
	
	private static final String EXTRA_DATALOG_FRAGMENT_KEYS = "datalog fragments keys";
	
	private static final int FRAGMENT_CONTAINER_RESID = R.id.activity_main_fragment_container;
	private BluetoothSerialService mBtService;

	// Fragments.
	private FragmentManager fragmentManager;
	private NoConnectionsFragment mNoConnFragment;
	private HashMap<String, DataLogFragment> dataLogFragments;
	private Fragment displayedFragment;
	private DataLogListFragment dataLogListFragment;
	
	private ConnectionFragment mConnFragment;

	private ConnectWaitDialogFragment connStatusDialogFragment;
	
	private static final IntentFilter SERVICE_INTENT_FILTER = new IntentFilter()
	{
		{
			addAction(SerialConnectionService.ACTION_DEVICE_CONNECTION_STATE_CHANGED);
			addAction(SerialConnectionService.ACTION_BROADCAST_SERVICE_STATE);
			addAction(BluetoothSerialService.ACTION_INCOMING_ACCEPTED);
		}
	};
	private final BroadcastReceiver serviceIntentReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();

			if (SerialConnectionService.ACTION_DEVICE_CONNECTION_STATE_CHANGED.equals(action))
			{
				//Get Extras.
				int connState = intent.getIntExtra(SerialConnectionService.EXTRA_DEVICE_CONNECTION_STATE, -1);
				String deviceName = intent.getStringExtra(SerialConnectionService.EXTRA_DEVICE_NAME);
				//BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothSerialService.EXTRA_BLUETOOTH_DEVICE);
				
				switch(connState)
				{
					case SerialConnectionService.CONNECTION_STATE_CONNECTED:
						onServiceConnectSuccess(deviceName);
						break;
					case SerialConnectionService.CONNECTION_STATE_DISCONNECTED:
						onServiceDisconnectSuccess(deviceName);
						break;
					case SerialConnectionService.CONNECTION_STATE_CONNECTING:
						onServiceConnect(deviceName);
						break;
					case SerialConnectionService.CONNECTION_STATE_CONNECT_FAILED:
						onServiceConnectFail(deviceName);
						break;
					case SerialConnectionService.CONNECTION_STATE_DISCONNECTING:
						onServiceDisconnect(deviceName);
					default:
						throw new IllegalArgumentException("The provided connection state extra was invalid or there was none.");
				}
			}
			else if(SerialConnectionService.ACTION_BROADCAST_SERVICE_STATE.equals(action))
			{
				onServiceStateReceived(intent.getExtras());
			}
			else if(BluetoothSerialService.ACTION_INCOMING_ACCEPTED.equals(action))
			{
				BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothSerialService.EXTRA_BLUETOOTH_DEVICE);
				onServiceIncomingAccepted(device);
			}
		}

		private void onServiceStateReceived(Bundle extras)
		{
			// TODO Auto-generated method stub
			
		}
	};

	private final MyHandler mHandler = new MyHandler(this);
	private final ServiceConnection mConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName className, IBinder binder)
		{
			SerialConnectionServiceBinder b = (SerialConnectionServiceBinder) binder;
			mBtService = (BluetoothSerialService) b.getService();
			mBtService.setDataReceiver(MainActivity.this.mHandler);
			mBtService.broadcastServiceState();
			Log.i(TAG, "Bound successfully to service: " + mBtService.toString());
		}

		@Override
		public void onServiceDisconnected(ComponentName className)
		{
			Log.i(TAG, "Unbound successfully from service: " + mBtService.toString());
			mBtService = null;
		}
	};
	
	/**
	 * Lifecycle callback methods
	 */

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle presses on the action bar items
		switch (item.getItemId())
		{
			case R.id.action_open_connect:
				showConnectionFragment();
				return true;
			case R.id.action_devices:
				showDataLogListFragment();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		Log.d(TAG, "Activity onStart() called: " + this.toString());

		// Start the service.
		Intent intent = new Intent(this, BluetoothSerialService.class);
		getApplicationContext().startService(intent);

		// Bind to again to service
		LocalBroadcastManager.getInstance(this).registerReceiver(serviceIntentReceiver, SERVICE_INTENT_FILTER);
		bindToBluetoothService();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Instantiate fragments.
		dataLogFragments = new HashMap<String, DataLogFragment>();

		// Get the Fragment manager associated to this activity.
		fragmentManager = getFragmentManager();

		if (savedInstanceState == null)
		{
			showEmptyConnectionsFragmetns();
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		Log.d(TAG, "Activity onResume() called: " + this.toString());
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		Log.d(TAG, "Activity onPause() called: " + this.toString());
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		Log.d(TAG, "Activity onStop() called: " + this.toString());
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		Log.d(TAG, "Activity onDestroy() called: " + this.toString());
		
		// Unbind from service.
		unbindService(mConnection);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceIntentReceiver);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		//Connection state dialog
		
		
		// Put ConnectionFragment if active.
		if(mConnFragment!=null && !mConnFragment.isDetached())
		fragmentManager.putFragment(outState, ConnectionFragment.TAG, mConnFragment);
		
		// Put DataLog fragments.
		ArrayList<String> fragmentKeys = new ArrayList<String>(10);
		Iterator<Entry<String, DataLogFragment>> it = dataLogFragments.entrySet().iterator();
		while (it.hasNext())
		{
			Entry<String, DataLogFragment> entry = it.next();
			DataLogFragment fragment = entry.getValue();
			String key = fragment.getDeviceName();
			fragmentManager.putFragment(outState, key, fragment);
			fragmentKeys.add(key);
		}
		String[] keys = fragmentKeys.toArray(new String[fragmentKeys.size()]);
		outState.putStringArray(EXTRA_DATALOG_FRAGMENT_KEYS, keys);
		
		//Put the ProgressDialog
		
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onSaveInstanceState(savedInstanceState);
		
		// Get the ConnectionFragment.
		mConnFragment = (ConnectionFragment) fragmentManager.getFragment(savedInstanceState, ConnectionFragment.TAG);
		
		//Get the DataLog fragments.
		String[] fragmentkeys = savedInstanceState.getStringArray(EXTRA_DATALOG_FRAGMENT_KEYS);
		for(int index = 0; index<fragmentkeys.length; index++)
		{
			String key = fragmentkeys[index];
			DataLogFragment fragment = (DataLogFragment) fragmentManager.getFragment(savedInstanceState, key);
			dataLogFragments.put(fragmentkeys[index], fragment);
		}
	}

	public BluetoothSerialService getConnectionService()
	{
		return mBtService;
	}

	/**
	 * Fragment transaction methods
	 * @param tag TODO
	 */
	// Action on action bar "Connect" button click.

	private void showFragment(Fragment fragment, boolean addToBackStack, String tag)
	{
		if(fragment == null)
		{
			throw new NullPointerException();
		}
		
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		transaction.setCustomAnimations(R.animator.fade_in, R.animator.fade_out, R.animator.fade_in,
				R.animator.fade_out);
		if (addToBackStack)
		{
			transaction.addToBackStack(tag);
		}
		
		
		//If the currently displayed fragment is a DataLogFragment, just hide its views instead of destroying them.
		if(displayedFragment != null)
		{
			if (displayedFragment instanceof DataLogFragment)
			{
				transaction.hide(displayedFragment);
			}
			else
			{
				transaction.remove(displayedFragment);
			}
		}
			
		if(fragment instanceof DataLogFragment)
		{
			if(fragment.isDetached()) transaction.attach(fragment);
			transaction.show(fragment);
		}
		else
		{
			transaction.add(FRAGMENT_CONTAINER_RESID, fragment, tag);
		}
		transaction.commit();
		fragmentManager.executePendingTransactions();
		displayedFragment = fragment;
	}

	private void showEmptyConnectionsFragmetns()
	{
		if (mNoConnFragment == null)
		{
			mNoConnFragment = NoConnectionsFragment.newInstance();
		}
		showFragment(mNoConnFragment, false, NoConnectionsFragment.TAG);
	}

	private void showConnectionFragment()
	{
		if (mConnFragment == null)
		{
			mConnFragment = ConnectionFragment.newInstance();
		}
		showFragment(mConnFragment, false, null);
	}

	private void showDataLogFragment(DataLogFragment fragment)
	{
		if (fragment == null)
		{
			throw new NullPointerException();
		}
		showFragment(fragment, false, fragment.getTag());
	}

	private void showDataLogListFragment()
	{
		int fragmentCount = dataLogFragments.entrySet().size();
		BluetoothDevice[] fragmentDevices = new BluetoothDevice[fragmentCount];
		Iterator<Entry<String, DataLogFragment>> iter = dataLogFragments.entrySet().iterator();
		for(int i = 0;iter.hasNext(); i++)
		{
			fragmentDevices[i] = iter.next().getValue().getDevice();
		}
		
		if(dataLogListFragment == null)
		{
			dataLogListFragment = DataLogListFragment.newInstance(fragmentDevices);
		}
		else if(dataLogListFragment.isAdded())
		{
			dataLogListFragment.setListItems(fragmentDevices);
		}
		showFragment(dataLogListFragment, false, DataLogListFragment.TAG);
	}

	private void showConnWaitFragment(String initMessage)
	{
		if(connStatusDialogFragment==null || !connStatusDialogFragment.isVisible())
		{
			//Instantiate fragment.
			connStatusDialogFragment = ConnectWaitDialogFragment.newInstance(initMessage);
			connStatusDialogFragment.setRetainInstance(true);
			
			//Get a transaction and set it up.
			FragmentTransaction transaction = fragmentManager.beginTransaction();
			transaction.setCustomAnimations(R.animator.fade_in, R.animator.fade_out, R.animator.fade_in,
					R.animator.fade_out);
			transaction.addToBackStack(ConnectWaitDialogFragment.TAG);
			transaction.add(FRAGMENT_CONTAINER_RESID, connStatusDialogFragment, ConnectWaitDialogFragment.TAG);
			
			//Commit transaction and make sure it executes immediately.
			transaction.commit();
			fragmentManager.executePendingTransactions();
		}
	}
	
	private void dismissConnWaitFragment(String message, int delayMillis)
	{
		if(connStatusDialogFragment!=null && connStatusDialogFragment.isAdded())
		{
			//Set optional message to be displayed
			if(message != null)
			{
				connStatusDialogFragment.setMessage(message);
			}
			
			//Get a transaction and set it up.
			final FragmentTransaction transaction = fragmentManager.beginTransaction();
			transaction.setCustomAnimations(R.animator.fade_in, R.animator.fade_out, R.animator.fade_in,
					R.animator.fade_out);
			transaction.remove(connStatusDialogFragment);
			
			//Commit transaction and make sure it executes immediately.
			Handler handler = new Handler();
			handler.postDelayed(new Runnable()
			{
				public void run()
				{
					transaction.commit();
					fragmentManager.executePendingTransactions();
				}
			}, delayMillis);
			
			connStatusDialogFragment = null;
		}
	}
	
	
	private void bindToBluetoothService()
	{
		Intent intent = new Intent(this, BluetoothSerialService.class);
		intent.putExtra(EXTRA_HANDLER, new Messenger(mHandler));
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}

	private void onDataReceived(String data, String deviceName, EEntryType type)
	{
		DataLogFragment deviceFragment = getDataLogFragment(deviceName);
		deviceFragment.onDataReceive(data, type);
	}

	private DataLogFragment getDataLogFragment(String deviceName)
	{
		DataLogFragment fragment = dataLogFragments.get(deviceName);
		
		if(fragment==null)
		{
			fragment = createDataLogFragmentFor(deviceName);
		}
		
		return fragment;
	}
	
	private DataLogFragment createDataLogFragmentFor(String deviceName)
	{
		BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceName);
		DataLogFragment fragment = DataLogFragment.newInstance(device);
		dataLogFragments.put(deviceName, fragment);
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		transaction.add(FRAGMENT_CONTAINER_RESID, fragment, deviceName);
		transaction.commit();
		return fragment;
	}
	
	 /**
	 *****************************| Service Callback Methods |******************************* 
	 */

	private void onServiceConnect(String deviceName)
	{
		Log.i(TAG, "Attempting connection to device: " + deviceName);
		showConnWaitFragment(this.getString(R.string.progress_dialog_connecting));
	}

	private void onServiceIncomingAccepted(BluetoothDevice device)
	{
		mConnFragment.onIncomingRequest(device);
	}
	
 	private void onServiceConnectSuccess(String deviceName)
	{
		Log.i(TAG, "Connected successfully to device: " + deviceName);
		dismissConnWaitFragment(getString(R.string.progress_dialog_success), CONNECTION_PROGRESS_HIDE_DELAY);
		
		DataLogFragment deviceFragment = getDataLogFragment(deviceName);
		showDataLogFragment(deviceFragment);
		deviceFragment.onConnect();
	}

	private void onServiceConnectFail(String deviceName)
	{
		Log.i(TAG, "Failed connecting to device: " + deviceName);
		dismissConnWaitFragment(getString(R.string.progress_dialog_failed), CONNECTION_PROGRESS_HIDE_DELAY);
	}

	private void onServiceDisconnect(String stringExtra)
	{
	}

	private void onServiceDisconnectSuccess(String deviceName)
	{
		Log.w(TAG, "Disconnected from device: " + deviceName);
		DataLogFragment deviceFragment = dataLogFragments.get(deviceName);
		deviceFragment.onDisconnect();
	}


	 /**
	 ************************| ConnectionFragment Callback Methods |*************************** 
	 */

	@Override
	public void onConnFragmentDeviceSelected(BluetoothDevice device, boolean incoming)
	{
		Toast.makeText(this, "Picked device " + device.getAddress(), Toast.LENGTH_SHORT).show();

		mBtService.stopListenForIncoming(BluetoothSerialService.CONNECT_INSECURE);
		
		
		if(incoming)
		{
			mBtService.connectToIncoming(device, BluetoothSerialService.CONNECT_INSECURE);
		}
		else
		{
			mBtService.connect(device, BluetoothSerialService.CONNECT_INSECURE);
		}
		
		mBtService.clearAcceptedIncoming();
		Log.d(TAG, "Service connect called: " + mBtService.toString());
	}

	@Override
	public void onConnFragmentRefresh()
	{
		mBtService.startListenForIncoming(BluetoothSerialService.CONNECT_INSECURE);
	}
	
	// Called when an active DataLogFragment wants to send data.
	@Override
	public void onFragmentDataSend(DataLogFragment sender, String data)
	{
		if (mBtService != null)
		{
			mBtService.writeTo(sender.getDeviceName(), data);
		}
	}

	@Override
	public void onDataLogListSelected(String deviceId)
	{
		this.showDataLogFragment(dataLogFragments.get(deviceId));
	}
	
	private static class MyHandler extends Handler
	{
		private final WeakReference<MainActivity> mActivity;

		public MyHandler(MainActivity activity)
		{
			super();
			mActivity = new WeakReference<MainActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg)
		{
			
			Bundle data = msg.getData();
			BluetoothDevice device = data.getParcelable(BluetoothDriver.EXTRA_BLUETOOTH_DEVICE);
			boolean securityOption = data.getBoolean(BluetoothDriver.EXTRA_SECURITY_OPTION);
			String deviceName = device.getAddress();
			
			MainActivity activity = mActivity.get();
			if (activity == null)
			{
				return;
			}

			switch (msg.what)
			{
				case BluetoothDriver.MESSAGE_READ:
					activity.onDataReceived((String) msg.obj, deviceName, EEntryType.RECEIVE);
					break;
				case BluetoothDriver.MESSAGE_WRITE:
					activity.onDataReceived((String) msg.obj, deviceName, EEntryType.SEND);
					break;
			}
		}
	}

	
}
