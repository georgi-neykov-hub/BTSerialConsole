package com.neykov.bluetoothserialconsole.connection;

public interface IDeviceDriverCallback
{

	public abstract void onOpenConnectionAttempt(DeviceDriver device);
	
	public abstract void onOpenConnectionSuccess(DeviceDriver device);

	public abstract void onOpenConnectionFailed(DeviceDriver device);

	public abstract void onCloseConnection(DeviceDriver device);

	public abstract void onReadFailed(DeviceDriver device, String errorMessage);

	public abstract void onWriteFailed(DeviceDriver device, String errorMessage);

}