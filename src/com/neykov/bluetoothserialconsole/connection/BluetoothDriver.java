package com.neykov.bluetoothserialconsole.connection;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;

public class BluetoothDriver extends DeviceDriver
{
	private static final String TAG = "BluetoothDriver";

	private static final int READ_BUFFER_SIZE = 1024;
	private static final Charset READ_CHARSET = Charset.forName("US-ASCII");

	public static final String EXTRA_BLUETOOTH_DEVICE = "Driver Bluetooth device Driver";
	public static final String EXTRA_SECURITY_OPTION = "Driver Bluetooth device security";

	// Constants that indicate the current connection state
	public static final int STATE_NONE = 0; // we're doing nothing
	public static final int STATE_LISTEN = 1; // now listening for incoming connections
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
	public static final int STATE_CONNECTED = 3; // now connected to a remote device
	public static final int STATE_CONNECTED_PAUSED = 4; // now connected to a remote device

	public static final int MESSAGE_READ = 50;
	public static final int MESSAGE_WRITE = 51;

	private final BluetoothDevice mBluetoothDevice;
	private final BluetoothAdapter mBluetoothAdapter;
	private final String mDeviceName;
	private final String mDeviceAdress;
	private final Handler mHandler;
	private final boolean mSecureConnection;
	private final UUID mSdpUuid;
	private final String mSdpName;
	private final Bundle mDeviceDriverProperties;

	private ParcelUuid[] mSupportedUUIDs;

	private ConnectionListenThread mListeningThread;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;

	private int mState;
	private InputStream mInputStream;
	private OutputStream mOutputStream;

	public BluetoothDriver(Context mContext, BluetoothDevice device, IDeviceDriverCallback callbackObject,
			Handler incomingHandler, UUID sdpUuid, String sdpName, String àppName, boolean secureConnection)
	{
		super(mContext, callbackObject, àppName);
		this.mHandler = incomingHandler;
		this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		this.mBluetoothDevice = device;
		this.mDeviceAdress = mBluetoothDevice.getAddress();
		this.mDeviceName = mBluetoothDevice.getName();
		this.mSecureConnection = secureConnection;
		this.mSdpUuid = sdpUuid;
		this.mSdpName = sdpName;
		this.mSupportedUUIDs = mBluetoothDevice.getUuids();

		this.mDeviceDriverProperties = new Bundle();
		mDeviceDriverProperties.putParcelable(EXTRA_BLUETOOTH_DEVICE, getBluetoothDevice());
		mDeviceDriverProperties.putBoolean(EXTRA_SECURITY_OPTION, isSecureConnecting());

		String uuids = "";
		for (ParcelUuid uuid : mSupportedUUIDs)
		{
			uuids += String.format("%1s\n", uuid.toString());
		}
		Log.d(TAG,
				String.format("Created new Bluetooth device Instance:\n %1s\nSupportedUUID:\n%2s", mDeviceName, uuids));

		mState = STATE_NONE;
	}

	private synchronized void setState(int state)
	{
		Log.d(TAG, "Changing state: " + mState + " -> " + state);
		mState = state;
	}
	
	public synchronized int getState()
	{
		return mState;
	}

	public boolean isSecureConnecting()
	{
		return mSecureConnection;
	}

	// Attempt to connect to the device.
	@Override
	public synchronized void openConnection()
	{
		startListening();
		startConnecting();
	}

	public synchronized void openConnection(BluetoothSocket socket)
	{
		if(socket.getRemoteDevice() != this.mBluetoothDevice)
		{
			throw new IllegalArgumentException("The provided socket's BluetoothDevice and the driver's BluetoothDevice do not match.");
		}
		startConnecting(socket);
	}
	
	private synchronized void startListening()
	{
		if (getState() == STATE_CONNECTED)
		{
			return;
		}

		Log.d(TAG, "Start listening for connection request.");

		// Cancel any thread attempting to make a connection
		if (mConnectThread != null)
		{
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Start the thread to listen on a BluetoothServerSocket
		if (mListeningThread == null)
		{
			mListeningThread = new ConnectionListenThread(mSecureConnection);
			mListeningThread.start();
		}
		setState(STATE_LISTEN);
	}

	private synchronized void startConnecting()
	{
		if (getState() == STATE_CONNECTED)
		{
			return;
		}

		Log.d(TAG, "connect to: " + mBluetoothDevice);

		// Cancel any thread attempting to make a connection
		if (mState == STATE_CONNECTING)
		{
			if (mConnectThread != null)
			{
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}

		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(BluetoothDriver.this, mBluetoothDevice, mSecureConnection);
		mConnectThread.start();

		setState(STATE_CONNECTING);
	}

	private synchronized void startConnecting(BluetoothSocket socket)
	{
		if (getState() == STATE_CONNECTED)
		{
			return;
		}

		Log.d(TAG, "connect to: " + mBluetoothDevice);

		// Cancel any thread attempting to make a connection
		if (mState == STATE_CONNECTING)
		{
			if (mConnectThread != null)
			{
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}

		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(BluetoothDriver.this, socket, mSecureConnection);
		mConnectThread.start();

		setState(STATE_CONNECTING);
	}
	
	protected synchronized void onConnected(BluetoothSocket socket, final String socketType)
	{
		Log.d(TAG, "connected, Socket Type:" + socketType);

		// Cancel the accept thread because we only want to connect to one device
		if (mListeningThread != null)
		{
			mListeningThread.cancel();
			mListeningThread = null;
		}

		// Cancel the thread that completed the connection
		if (mConnectThread != null)
		{
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null)
		{
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Start the thread to manage the connection and perform transmissions
		mConnectedThread = new ConnectedThread(this, socket, socketType);
		mConnectedThread.start();

		setState(STATE_CONNECTED);
	}

	// Stop all threads
	public synchronized void stop()
	{
		Log.d(TAG, "stop");

		if (mConnectThread != null)
		{
			mConnectThread.cancel();
			mConnectThread = null;
		}

		if (mConnectedThread != null)
		{
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		if (mListeningThread != null)
		{
			mListeningThread.cancel();
			mListeningThread = null;
		}

		setState(STATE_NONE);
	}

	@Override
	public synchronized void closeConnection()
	{
		stop();
	}

	@Override
	public void setup(ConnectionParameters connParams) throws IOException
	{
		if (getState() != STATE_CONNECTED)
		{
			return;
		}
	}

	@Override
	public boolean isConnected()
	{
		return ((getState() == STATE_CONNECTED)) ? true : false;
	}

	@Override
	public int write(byte[] writeBuffer)
	{
		return write(writeBuffer, writeBuffer.length);
	}

	@Override
	public int write(String writeString)
	{
		byte[] writeBuffer = writeString.getBytes(READ_CHARSET);
		return write(writeBuffer, writeBuffer.length);
	}

	@Override
	public int write(byte[] writeBuffer, int length)
	{
		// Create temporary object
		ConnectedThread r;
		// Synchronize a copy of the ConnectedThread
		synchronized (this)
		{
			if (mState != STATE_CONNECTED)
				return 0;
			r = mConnectedThread;
		}
		// Perform the write unsynchronized
		r.write(writeBuffer, length);
		return length;
	}

	@Override
	public InputStream getInputStream()
	{
		return mInputStream;
	}

	@Override
	public OutputStream getOutputStream()
	{
		return mOutputStream;
	}

	public BluetoothDevice getBluetoothDevice()
	{
		return mBluetoothDevice;
	}

	@Override
	public String getDeviceName()
	{
		return mDeviceAdress;
	}

	public Bundle getDeviceDriverProperties()
	{
		return mDeviceDriverProperties;
	}

	@Override
	public int hashCode()
	{
		return mDeviceAdress.hashCode();
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof BluetoothDriver)
		{
			return mDeviceAdress.equals(((BluetoothDriver) o).mDeviceAdress);
		}
		return false;
	}

	@Override
	public String toString()
	{
		return String.format("%1s %2s", mDeviceName, mDeviceAdress);
	}

	/**
	 * This thread runs while listening for incoming connections. It behaves like a server-side client. It runs until a
	 * connection is accepted (or until cancelled).
	 */
	private class ConnectionListenThread extends Thread
	{
		// The local server socket
		private final BluetoothServerSocket mmServerSocket;
		private String mSocketType;

		public ConnectionListenThread(boolean secure)
		{
			BluetoothServerSocket tmp = null;
			mSocketType = secure ? "Secure" : "Insecure";

			// Create a new listening server socket
			try
			{
				if (secure)
				{
					tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(mSdpName, mSdpUuid);
				}
				else
				{
					tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(mSdpName, mSdpUuid);
				}
			} catch (IOException e)
			{
				Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
			}
			mmServerSocket = tmp;
		}

		public void run()
		{
			Log.d(TAG, "Socket Type: " + mSocketType + "BEGIN mAcceptThread" + this);
			setName("AcceptThread" + mSocketType);

			BluetoothSocket socket = null;

			// Listen to the server socket if we're not connected
			while (mState != STATE_CONNECTED)
			{
				try
				{
					// This is a blocking call and will only return on a
					// successful connection or an exception
					socket = mmServerSocket.accept();
				} catch (IOException e)
				{
					break;
				}

				// If a connection was accepted
				Log.i(TAG, "Connection accepted!");
				if (socket != null)
				{
					synchronized (BluetoothDriver.this)
					{
						switch (mState)
						{
							case STATE_LISTEN:
							case STATE_CONNECTING:
								// Situation normal. Start the connected thread.
								onConnected(socket, mSocketType);

								break;
							case STATE_NONE:
							case STATE_CONNECTED:
								// Either not ready or already connected. Terminate new socket.
								try
								{
									socket.close();
								} catch (IOException e)
								{
									Log.e(TAG, "Could not close unwanted socket", e);
								}
								break;
						}
					}

				}
			}
			Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

		}

		public void cancel()
		{
			Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
			try
			{
				mmServerSocket.close();
			} catch (IOException e)
			{
				Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
			}
		}
	}

	/*
	 * This thread runs while attempting to make an outgoing connection with a device. It runs straight through; the
	 * connection either succeeds or fails.
	 */
	private class ConnectThread extends Thread
	{
		private final BluetoothSocket mmSocket;
		private String mSocketType;
		private final BluetoothDriver mDriver;

		public ConnectThread(BluetoothDriver driver, BluetoothDevice device, boolean secure)
		{
			BluetoothSocket tmp = null;
			mDriver = driver;
			mSocketType = secure ? "Secure" : "Insecure";

			// Get a BluetoothSocket for a connection with the given BluetoothDevice
			try
			{
				if (secure)
				{
					tmp = device.createRfcommSocketToServiceRecord(mSdpUuid);
				}
				else
				{
					tmp = device.createInsecureRfcommSocketToServiceRecord(mSdpUuid);
				}
			} catch (IOException e)
			{
				Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
			}
			Log.i(TAG, "Socket Type: " + mSocketType + " created successfully.");
			mmSocket = tmp;
		}
		
		public ConnectThread(BluetoothDriver driver, BluetoothSocket socket, boolean secure)
		{
			mDriver = driver;
			mmSocket = socket;
			mSocketType = secure ? "Secure" : "Insecure";
		}

		public void run()
		{
			Log.i(TAG, "BEGIN mConnectThread:\n" + mDriver.getDeviceName() + "\nSocketType: " + mSocketType);
			setName("ConnectThread" + mSocketType);

			// Always cancel discovery because it will slow down a connection
			mBluetoothAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			if(!mmSocket.isConnected())
			{
				try
				{
					// This is a blocking call and will only return on a
					// successful connection or an exception
					mmSocket.connect();
				} catch (IOException e)
				{
					Log.i(TAG, "Unable to connect. " + mSocketType + e.getMessage());
					// Close the socket
					try
					{
						mmSocket.close();
					} catch (IOException e2)
					{
						Log.i(TAG, "Unable to close() " + mSocketType + " socket during connection failure", e2);
					}
					mDeviceCallback.onOpenConnectionFailed(mDriver);
					setState(STATE_NONE);
					return;
				}
			}
			// Reset the ConnectThread because we're done
			synchronized (BluetoothDriver.this)
			{
				mConnectThread = null;
			}

			// Start the connected thread
			onConnected(mmSocket, mSocketType);
		}

		public void cancel()
		{
			try
			{
				mmSocket.close();
			} catch (IOException e)
			{
				Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
			}
		}
	}

	/*
	 * This thread runs during a connection with a remote device. It handles all incoming and outgoing transmissions.
	 */
	private class ConnectedThread extends Thread
	{
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;
		private final BluetoothDriver mDriver;

		public ConnectedThread(BluetoothDriver driver, BluetoothSocket socket, String socketType)
		{
			Log.d(TAG, "create ConnectedThread: " + socketType);
			mDriver = driver;
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try
			{
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e)
			{
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = new BufferedInputStream(tmpIn);
			mmOutStream = new BufferedOutputStream(tmpOut);
			BluetoothDriver.this.mInputStream = tmpIn;
			BluetoothDriver.this.mOutputStream = tmpOut;
		}

		public void run()
		{
			mDeviceCallback.onOpenConnectionSuccess(mDriver);

			Log.i(TAG, "BEGIN mConnectedThread");
			byte[] buffer = new byte[READ_BUFFER_SIZE];
			int readBytesCount;

			Bundle driverProperties = getDeviceDriverProperties();
			final int driverHash = hashCode();
			// Keep listening to the InputStream while connected
			while (true)
			{
				try
				{
					// Read from the InputStream
					readBytesCount = mmInStream.read(buffer);
					String readChars = new String(buffer, 0, readBytesCount, READ_CHARSET);
					Log.d(TAG, "Read " + readBytesCount + " bytes: " + readChars);

					// Send the obtained bytes to the UI Activity
					Message msg = mHandler.obtainMessage(MESSAGE_READ, readBytesCount, driverHash, readChars);
					msg.setData(driverProperties);
					mHandler.sendMessage(msg);

				} catch (IOException e)
				{
					Log.e(TAG, "disconnected", e);
					
					try
					{
						mmInStream.close();
						mmOutStream.close();
					} catch (IOException e1)
					{
						e1.printStackTrace();
					}
					
					mDeviceCallback.onCloseConnection(mDriver);
					break;
				}
			}
		}

		/**
		 * Write to the connected OutStream.
		 * 
		 * @param buffer
		 *            The bytes to write
		 */
		public void write(byte[] buffer, int length)
		{
			try
			{
				mmOutStream.write(buffer, 0, length);
				mmOutStream.flush();
				Log.d(TAG, BluetoothDriver.this + "\nSent " + buffer.length + " bytes of data.");
				String writtenChars = new String(buffer, READ_CHARSET);

				// Share the sent message back to the UI Activity
				Message msg = mHandler.obtainMessage(MESSAGE_WRITE, length, BluetoothDriver.this.hashCode(),
						writtenChars);
				msg.setData(mDeviceDriverProperties);
				mHandler.sendMessage(msg);
			} catch (IOException e)
			{
				Log.e(TAG, "Exception during write", e);
			}
		}

		public void cancel()
		{
			try
			{
				mmSocket.close();
			} catch (IOException e)
			{
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

}
