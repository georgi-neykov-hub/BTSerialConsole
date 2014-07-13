package com.neykov.bluetoothserialconsole.data;

import com.neykov.bluetoothserialconsole.connection.DeviceDriver;
import com.neykov.bluetoothserialconsole.connection.IDeviceDriverCallback;

public abstract class DataClient implements IDeviceDriverCallback
{
	protected final int mId;
	protected final DeviceDriver mDeviceDriver;
	
	
	
	protected DataClient(int id, DeviceDriver deviceDriver)
	{
		this.mId = id;
		this.mDeviceDriver = deviceDriver;
	}


	/*
	 * 
	 * */
	
	@Override
	public void onOpenConnectionAttempt(DeviceDriver device)
	{
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onOpenConnectionSuccess(DeviceDriver device)
	{
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onOpenConnectionFailed(DeviceDriver device)
	{
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onCloseConnection(DeviceDriver device)
	{
		// TODO Auto-generated method stub
		
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
	
	
}
