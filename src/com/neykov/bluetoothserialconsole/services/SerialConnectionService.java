package com.neykov.bluetoothserialconsole.services;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;

import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.neykov.bluetoothserialconsole.connection.DeviceDriver;
import com.neykov.bluetoothserialconsole.connection.IDeviceDriverCallback;

public abstract class SerialConnectionService extends Service implements IDeviceDriverCallback
{
	// Intent actions.
	public static final String ACTION_DEVICE_CONNECTION_STATE_CHANGED = "Device connection state changed";
	public static final String ACTION_SERVICE_STOP = "Stop Service";
	public static final String ACTION_BROADCAST_SERVICE_STATE = "Send service state";
	
	// Intent extras.
	public static final String EXTRA_DEVICE_NAME = "Device name";
	public static final String EXTRA_DEVICE_CONNECTION_STATE = "Device connection state";
	public static final String EXTRA_STATE_HAS_CONNECTIONS = "Device active connection state";
	
	//Device connection state constants.
	public static final int CONNECTION_STATE_CONNECTING = 4001;
	public static final int CONNECTION_STATE_CONNECT_FAILED = 4002;
	public static final int CONNECTION_STATE_CONNECTED = 4003;
	public static final int CONNECTION_STATE_DISCONNECTING = 4004;
	public static final int CONNECTION_STATE_DISCONNECTED = 4005;
	
	// Notifications related data constants.
	private static final int NOTIFICATION_ID = 56;
	private static final int NOTIFICATION_ICON_ID = 0;
	private static final String NOTIFICATION_TITLE = "Connection manager";
	private static final String NOTIFICATION_CONTENT = "Running (Swipe to stop)";
	private static final String NOTIFICATION_CONTENT_INFO_FORMAT = "(%1s connections)";
	
	private static IntentFilter NOTIFICATION_STOP_SERVICE_FILTER = new IntentFilter(ACTION_SERVICE_STOP);
	
	protected HashMap<String, DeviceDriver> devices;
	
	protected Handler outputHandler;

	protected int connectedDevices;
	
	protected boolean hasActiveConnections = false;
	
	protected final IBinder serviceBinder;
	
	protected final BroadcastReceiver mReceiver = new BroadcastReceiver()
	{
		
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if(intent.getAction().equals(ACTION_SERVICE_STOP))
			{
				stopSelf();
			}	
		}
	};
	
	protected SerialConnectionService()
	{
		super();
		devices = new HashMap<String, DeviceDriver>();
		serviceBinder = new SerialConnectionServiceBinder(this);
		connectedDevices = 0;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		Log.d("SerialService", "New service created: " + this.toString());
		registerReceiver(mReceiver, NOTIFICATION_STOP_SERVICE_FILTER);
	}

	@Override
	public void onDestroy()
	{
		Log.d("SerialService", "Service destroyed: " + this.toString());

		closeAllConnections();
		
		RemoveNotification();
		unregisterReceiver(mReceiver);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return serviceBinder;
	}

	@Override
	public void onRebind (Intent intent)
	{
		RemoveNotification();
	}
	
	@Override
	public boolean onUnbind (Intent intent)
	{
		ShowNotification(devices.size());
		outputHandler = null;
		return true;
	}
	
	public final void broadcastServiceState()
	{
		Log.d("", "Service state sent");
		Intent intent = getServiceStateIntent();
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
	
	protected final void broadcastConnectionStateChange(DeviceDriver device, int connState)
	{
		if(device==null)
		{
			throw new NullPointerException("Provided DeviceDriver is null;");
		}

		//Check connection state for validity.
		switch(connState)
		{
			case CONNECTION_STATE_CONNECTING:
			case CONNECTION_STATE_CONNECT_FAILED:
			case CONNECTION_STATE_CONNECTED:
			case CONNECTION_STATE_DISCONNECTING:
			case CONNECTION_STATE_DISCONNECTED:
				break;
			default:
				throw new IllegalArgumentException("The provided connection state is invalid.");
		}
		Intent intent = getConnectionStateChangedIntent(device, connState);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
	
	protected Intent getServiceStateIntent()
	{
		Intent intent = new Intent(ACTION_BROADCAST_SERVICE_STATE);
			intent.putExtra(EXTRA_STATE_HAS_CONNECTIONS, hasActiveConnections);
		return intent;
	}
	
	protected Intent getConnectionStateChangedIntent(DeviceDriver device, int connState)
	{
		Intent intent = new Intent();
		intent.setAction(ACTION_DEVICE_CONNECTION_STATE_CHANGED)
			.putExtra(EXTRA_DEVICE_CONNECTION_STATE, connState)
			.putExtra(EXTRA_DEVICE_NAME, device.getDeviceName());
		return intent;
	}
	
  	public void setDataReceiver(Handler receiverHandler)
	{
		if (outputHandler == null)
		{
			outputHandler = receiverHandler;
		}
	}

	// Public methods for connecting / disconnecting device.
	public void connect(DeviceDriver device)
	{
		device.openConnection();
		onOpenConnectionAttempt(device);
	}

	public void disconnect(DeviceDriver device)
	{
		device.closeConnection();
		onCloseConnectionAttempt(device);
	}
	
	public void closeAllConnections()
	{
		Iterator<Entry<String, DeviceDriver>> it = devices.entrySet().iterator();
		while (it.hasNext())
		{
			Entry<String, DeviceDriver> entry = it.next();
			entry.getValue().closeConnection();
		}
		devices.clear();
	}
	
	public void writeTo(String deviceName, String writeString)
	{
		DeviceDriver target = devices.get(deviceName);
		if(target==null)
		{
			throw new IllegalArgumentException();
		}
		target.write(writeString);
	}
	
	public void writeTo(String deviceName, byte[] writeBuffer)
	{
		writeTo(deviceName,writeBuffer,writeBuffer.length);
	}
	
	public void writeTo(String deviceName, byte[] writeBuffer, int length)
	{
		DeviceDriver target = devices.get(deviceName);
		if(target==null)
		{
			throw new IllegalArgumentException();
		}
		target.write(writeBuffer, length);
	}
	
	public void writeToAll(String writeString)
	{
		if (hasActiveConnections)
		{
			Iterator<Entry<String, DeviceDriver>> it = devices.entrySet().iterator();
			while (it.hasNext())
			{
				Entry<String, DeviceDriver> entry = it.next();
				entry.getValue().write(writeString);
			}
		}
	}
	
	public void writeToAll(byte[] writeBuffer)
	{
		this.writeToAll(writeBuffer, writeBuffer.length);
	}

	public void writeToAll(byte[] writeBuffer, int length)
	{
		if (hasActiveConnections)
		{
			Iterator<Entry<String, DeviceDriver>> it = devices.entrySet().iterator();
			while (it.hasNext())
			{
				Entry<String, DeviceDriver> entry = it.next();
				entry.getValue().write(writeBuffer, length);
			}
		}
	}

	@Override
	public void onOpenConnectionAttempt(DeviceDriver device)
	{
		//Notify about connection state change
		broadcastConnectionStateChange(device, CONNECTION_STATE_CONNECTING);
	}

	@Override
	public void onOpenConnectionSuccess(DeviceDriver device)
	{
		hasActiveConnections = true;
		devices.put(device.getDeviceName(), device);
		
		//Notify about connection state change
		broadcastConnectionStateChange(device, CONNECTION_STATE_CONNECTED);
	}

	@Override
	public void onOpenConnectionFailed(DeviceDriver device)
	{
		//Clean-up.
		device.closeConnection();
		
		//Notify about connection state change
		broadcastConnectionStateChange(device, CONNECTION_STATE_CONNECT_FAILED);
	}

	@Override
	public void onCloseConnection(DeviceDriver device)
	{
		// Clean-up.
		devices.remove(device.getDeviceName());
		device.closeConnection();
		
		//Notify about connection state change
		broadcastConnectionStateChange(device, CONNECTION_STATE_DISCONNECTED);
	}

	public void onCloseConnectionAttempt(DeviceDriver device)
	{
		//Notify about connection state change
		broadcastConnectionStateChange(device, CONNECTION_STATE_DISCONNECTING);
	}

	@Override
	public void onReadFailed(DeviceDriver device, String errorMessage)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onWriteFailed(DeviceDriver device, String errorMessage)
	{
		// TODO Auto-generated method stub

	}

	public boolean hasActiveConnections()
	{
		return hasActiveConnections;
	}

	// Notification methods.
	protected void ShowNotification(int deviceCount)
	{
		Notification.Builder mBuilder = new Notification.Builder(SerialConnectionService.this)
				.setSmallIcon(NOTIFICATION_ICON_ID)
				.setContentTitle(NOTIFICATION_TITLE)
				.setContentText(NOTIFICATION_CONTENT)
				.setContentInfo(String.format(NOTIFICATION_CONTENT_INFO_FORMAT, deviceCount))
				.setAutoCancel(false)
				.setOngoing(true);
		
		Intent intent = new Intent(this, SerialConnectionService.class).setAction(ACTION_SERVICE_STOP);
		PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
		//mBuilder.addAction(0, "Stop", pendingIntent);
				
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		//mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}
	
	protected void RemoveNotification()
	{
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(NOTIFICATION_ID);
	}

}
