package com.neykov.bluetoothserialconsole.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.content.Context;

public abstract class DeviceDriver implements IDeviceDriver
{
	
	protected final IDeviceDriverCallback mDeviceCallback;
	
	protected DeviceDriver(Context context, IDeviceDriverCallback callbackObject, String sAppName)
	{
		this.mDeviceCallback = callbackObject;
	}
		
	abstract public void openConnection();
	
	abstract public void closeConnection();
	
	abstract public void setup(ConnectionParameters connParams) throws IOException;

	abstract public boolean isConnected();
	
	abstract public int write(byte[] writeBuffer);
	
	abstract public int write(byte[] writeBuffer, int length);
	
	public int write(String writeString)
	{
		return write(writeString.getBytes());
		
	}
	
	
	public abstract OutputStream getOutputStream();

	public abstract InputStream getInputStream();
	
	public abstract String getDeviceName();
}
