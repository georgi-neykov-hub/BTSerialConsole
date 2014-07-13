package com.neykov.bluetoothserialconsole.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.neykov.bluetoothserialconsole.connection.BluetoothDriver;
import com.neykov.bluetoothserialconsole.connection.DeviceDriver;

public class BluetoothSerialService extends SerialConnectionService
{
	public static final String SERVICE_NAME = "BluetoothSerialService";
	public static final String TAG = SERVICE_NAME;

	// Name for the SDP record when creating server socket
	private static final UUID MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
	private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
	private static final String NAME_SECURE = "BluetoothConnSecure";
	private static final String NAME_INSECURE = "BluetoothConntInsecure";

	// Intent action fields
	public static final String ACTION_INCOMING_LISTEN_STATE_CHANGED = "Incoming listen state changed";
	public static final String ACTION_INCOMING_ACCEPTED = "Incoming connection accepted";
	
	// Intent extra field flags.
	public static final String EXTRA_CONNECTION_SECURITY = "Secure extra";
	public static final String EXTRA_BLUETOOTH_DEVICE = "Accepted connection request BluetoothDevice";
	public static final String EXTRA_INCOMING_STATE = "Incoming connection accept status.";

	// Flags used to indicate the type of connection.
	public static final int CONNECT_SECURE = 1001;
	public static final int CONNECT_INSECURE = 1002;
	public static final int CONNECT_SECURE_INSECURE = 1003;
	public static final int STATE_LISTENING = 1004;
	public static final int STATE_NOT_LISTENING = 1005;
	
	// Constants for connection limits and timeouts.
	private static final int CONNECTED_LIMIT_SECURE = 10;
	private static final int CONNECTED_LIMIT_INSECURE = 10;
	public static final int SERVICE_ACCEPTING_TIMELIMIT = 500;
	public static final int SERVICE_ACCEPTING_LIMIT = 5;
	
	private int secureConnectionCount, insecureConnectionCount;
	private final HashMap<String, BluetoothSocket> acceptedSecureIncomingConnections;
	private final HashMap<String, BluetoothSocket> acceptedInsecureIncomingConnections;
	
	private ConnectionListenThread listenSecureIncomingThread;
	private ConnectionListenThread listenInsecureIncomingThread;
	
	private static BluetoothSerialService currInstance;

	public static BluetoothSerialService getService()
	{
		return currInstance;
	}

	public BluetoothSerialService()
	{
		super();

		secureConnectionCount = 0;
		insecureConnectionCount = 0;
		acceptedSecureIncomingConnections = new HashMap<String, BluetoothSocket>(SERVICE_ACCEPTING_LIMIT);
		acceptedInsecureIncomingConnections = new HashMap<String, BluetoothSocket>(SERVICE_ACCEPTING_LIMIT);
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		BluetoothSerialService.currInstance = this;
	}

	@Override
	public void onDestroy()
	{
		BluetoothSerialService.currInstance = null;

		// Clear other resources.
		super.onDestroy();
	}

	@Override
	public void connect(DeviceDriver device)
	{
		// Left empty on purpose, instead use connect(String, int) or connect(BluetoothDevice, int;
	}

	public void connect(BluetoothDevice device, int securityOption) throws IllegalStateException
	{
		Log.d(SERVICE_NAME, "Service connect called: " + this.toString());

		BluetoothDriver newDevice = null;
		switch (securityOption)
		{
			case CONNECT_SECURE:
				if (secureConnectionCount == CONNECTED_LIMIT_SECURE)
				{
					throw new IllegalStateException();
				}
				newDevice = new BluetoothDriver(this, device, this, outputHandler, MY_UUID_SECURE, NAME_SECURE, SERVICE_NAME, true);
				break;
			case CONNECT_INSECURE:
				if (insecureConnectionCount == CONNECTED_LIMIT_INSECURE)
				{
					throw new IllegalStateException();
				}
				newDevice = new BluetoothDriver(this, device, this, outputHandler, MY_UUID_INSECURE, NAME_INSECURE, SERVICE_NAME, false);
				break;
			default:
				throw new IllegalArgumentException("Provided argument was not a valid security option.");
		}

		super.connect(newDevice);
	}

	public void connectToIncoming(BluetoothDevice device, int securityOption)
	{
		//Stop listening for incoming.
		stopListenForIncoming(securityOption);
		
		String deviceAdress = device.getAddress();
		BluetoothSocket socket = null;
		
		switch(securityOption)
		{
			case CONNECT_SECURE:
				synchronized(acceptedSecureIncomingConnections)
				{
					socket = acceptedSecureIncomingConnections.get(deviceAdress);
				}
				break;
			case CONNECT_INSECURE:
				synchronized(acceptedInsecureIncomingConnections)
				{
					socket = acceptedInsecureIncomingConnections.get(deviceAdress);
				}
				break;
			default:
				throw new IllegalArgumentException("Provided argument was not a valid security option.");
		}
		
		if(socket==null)
		{
			throw new IllegalArgumentException("The provided BluetoothDevice is not valid.");
		}
		
		BluetoothDriver newDevice = null;
		switch (securityOption)
		{
			case CONNECT_SECURE:
				if (secureConnectionCount == CONNECTED_LIMIT_SECURE)
				{
					throw new IllegalStateException();
				}
				newDevice = new BluetoothDriver(this, device, this, outputHandler, MY_UUID_SECURE, NAME_SECURE, SERVICE_NAME, true);
				acceptedSecureIncomingConnections.remove(deviceAdress);
				break;
			case CONNECT_INSECURE:
				if (insecureConnectionCount == CONNECTED_LIMIT_INSECURE)
				{
					throw new IllegalStateException();
				}
				newDevice = new BluetoothDriver(this, device, this, outputHandler, MY_UUID_INSECURE, NAME_INSECURE, SERVICE_NAME, false);
				acceptedInsecureIncomingConnections.remove(deviceAdress);
				break;
			default:
				throw new IllegalArgumentException("Provided argument was not a valid security option.");
		}
		newDevice.openConnection(socket);
		
		super.onOpenConnectionAttempt(newDevice);
	}
	
	public void broadcastIncomingListenStateChange(int currState, int securityOption)
	{
		Intent intent = new Intent(ACTION_INCOMING_LISTEN_STATE_CHANGED);
		intent.putExtra(EXTRA_INCOMING_STATE, currState);
		intent.putExtra(EXTRA_CONNECTION_SECURITY, securityOption);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
	
	public void startListenForIncoming(int securityOption)
	{
		switch(securityOption)
		{
			case CONNECT_SECURE:
				if(listenSecureIncomingThread!=null)
				{
					listenSecureIncomingThread.cancel();
					listenSecureIncomingThread = null;
				}
				listenSecureIncomingThread = new ConnectionListenThread(MY_UUID_SECURE, NAME_SECURE, true);
				listenSecureIncomingThread.start();
				break;
			case CONNECT_INSECURE:
				if(listenInsecureIncomingThread!=null)
				{
					listenInsecureIncomingThread.cancel();
					listenInsecureIncomingThread = null;
				}
				listenInsecureIncomingThread = new ConnectionListenThread(MY_UUID_INSECURE, NAME_INSECURE, false);
				listenInsecureIncomingThread.start();
				break;
			case CONNECT_SECURE_INSECURE:
				startListenForIncoming(CONNECT_SECURE);
				startListenForIncoming(CONNECT_INSECURE);
				break;
				
			default:
				throw new IllegalArgumentException("Provided argument was not a valid security option.");
		}
	}
	
	public void stopListenForIncoming(int securityOption)
	{
		switch(securityOption)
		{
			case CONNECT_SECURE:
				if(listenSecureIncomingThread!=null)
				{
					listenSecureIncomingThread.cancel();
					listenSecureIncomingThread = null;
				}
				break;
			case CONNECT_INSECURE:
				if(listenInsecureIncomingThread!=null)
				{
					listenInsecureIncomingThread.cancel();
					listenInsecureIncomingThread = null;
				}
				break;
			case CONNECT_SECURE_INSECURE:
				stopListenForIncoming(CONNECT_SECURE);
				stopListenForIncoming(CONNECT_INSECURE);

				break;
			default:
				throw new IllegalArgumentException("Provided argument was not a valid security option.");
		}
	}
	
	public void clearAcceptedIncoming()
	{
		acceptedSecureIncomingConnections.clear();
		acceptedInsecureIncomingConnections.clear();
	}
	
	public int getSecureConnectionCount()
	{
		return secureConnectionCount;
	}

	public int getInsecureConnectionCount()
	{
		return insecureConnectionCount;
	}

	@Override
	protected Intent getConnectionStateChangedIntent(DeviceDriver device, int connState)
	{
		BluetoothDriver driver = (BluetoothDriver)device;
		Intent intent = super.getConnectionStateChangedIntent(device, connState)
				.putExtra(EXTRA_BLUETOOTH_DEVICE, driver.getBluetoothDevice());
		return intent;
	}
	
	/*
	 * ****************** Callback Methods * *****************
	 */

	@Override
	public void onOpenConnectionSuccess(DeviceDriver device)
	{
		BluetoothDriver connDriver = (BluetoothDriver) device;
		if (connDriver.isSecureConnecting())
		{
			secureConnectionCount++;
		}
		else
		{
			insecureConnectionCount++;
		}

		super.onOpenConnectionSuccess(device);
	}

	@Override
	public void onOpenConnectionFailed(DeviceDriver device)
	{
		
		super.onOpenConnectionFailed(device);
	}

	public void onIncomingListenStateChanged(int currState, int securityOption)
	{
		broadcastIncomingListenStateChange(currState, securityOption);
	}
	
	public void onIncomingConnectionAccepted(BluetoothSocket socket, int securityOption)
	{
		switch(securityOption)
		{
			case CONNECT_SECURE:
				synchronized(acceptedSecureIncomingConnections)
				{
					acceptedSecureIncomingConnections.put(socket.getRemoteDevice().getAddress(), socket);
				}	
				break;
			case CONNECT_INSECURE:
				synchronized(acceptedInsecureIncomingConnections)
				{
					acceptedInsecureIncomingConnections.put(socket.getRemoteDevice().getAddress(), socket);
				}	
				break;
			default:
				throw new IllegalArgumentException("Provided argument was not a valid security option.");
		}
		
		Intent intent = new Intent(ACTION_INCOMING_ACCEPTED);
		intent.putExtra(EXTRA_DEVICE_NAME, socket.getRemoteDevice().getAddress());
		intent.putExtra(EXTRA_BLUETOOTH_DEVICE, socket.getRemoteDevice());
		intent.putExtra(EXTRA_CONNECTION_SECURITY, securityOption);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
	
	private class ConnectionListenThread extends Thread
	{
		public static final String THREAD_NAME = "BTServiceListenIncomingThread";
		
		private final BluetoothServerSocket btServerSocket;
		private final String mSocketType;
		private final BluetoothAdapter bluetoothAdapter;
		private final UUID sdpUuid;
		private final String sdpName;
		private final boolean secure;
		
		public ConnectionListenThread(UUID sdpUuid, String sdpName, boolean secure)
		{
			this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			this.sdpUuid = sdpUuid;
			this.sdpName = sdpName;
			this.secure = secure;
			this.mSocketType = secure ? "Secure" : "Insecure";
			
			
			// Create a new listening server socket
			BluetoothServerSocket tmp = null;
			try
			{
				if (secure)
				{
					tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(sdpName, sdpUuid);
				}
				else
				{
					tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(sdpName, sdpUuid);
				}
			} catch (IOException e)
			{
				Log.e(TAG, "Listening failed. Unable to get a BluetoothServerSocket.\nSocket Type: " + mSocketType + "\n", e);
			}
			btServerSocket = tmp;
		}

		public void run()
		{
			Log.i(TAG, "Socket Type: " + mSocketType + "BEGIN IncomingThread" + this);
			this.setName(THREAD_NAME);

			onIncomingListenStateChanged(STATE_LISTENING, (secure? CONNECT_SECURE: CONNECT_INSECURE));

			// Listen to the server socket if we're not connected
			while (true)
			{
				BluetoothSocket socket = null;
				try
				{
					socket = btServerSocket.accept();
				} catch (IOException e)
				{
					break;
				}

				// If a connection was accepted
				Log.i(TAG, "Connection accepted!");
				if (socket != null)
				{
					BluetoothSerialService.this.onIncomingConnectionAccepted(socket, (secure? CONNECT_SECURE: CONNECT_INSECURE));
				}
			}
			onIncomingListenStateChanged(STATE_NOT_LISTENING, (secure? CONNECT_SECURE: CONNECT_INSECURE));

			Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

		}

		public void cancel()
		{
			Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
			try
			{
				btServerSocket.close();
			} catch (IOException e)
			{
				Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
			}
		}
	}

	
}
