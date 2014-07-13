package com.neykov.bluetoothserialconsole.services;

import android.os.Binder;

public class SerialConnectionServiceBinder extends Binder
{
	private final SerialConnectionService service;
	
	public SerialConnectionServiceBinder(SerialConnectionService service)
	{
		this.service = service;
	}
	
	public SerialConnectionService getService()
	{
		return service;
	}
}
