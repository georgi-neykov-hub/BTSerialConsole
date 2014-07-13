package com.neykov.bluetoothserialconsole.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface IDeviceDriver
{
	public void openConnection();

	public void closeConnection();

	public void setup(ConnectionParameters connParams) throws IOException;

	public boolean isConnected();

	public int write(byte[] writeBuffer, int length);

	public int write(byte[] writeBuffer);
	
	public int write(String writeString);
	
	public InputStream getInputStream();
	
	public OutputStream getOutputStream();
	
	public String getDeviceName();
}
