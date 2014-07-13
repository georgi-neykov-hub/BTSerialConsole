package com.neykov.bluetoothserialconsole.data;

import java.util.HashMap;

public abstract class DataClientManager
{
	protected HashMap<String, DataClient> mDataClients;
	
	public abstract void addClient();
}
