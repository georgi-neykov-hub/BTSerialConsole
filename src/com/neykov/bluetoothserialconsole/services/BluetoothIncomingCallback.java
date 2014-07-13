package com.neykov.bluetoothserialconsole.services;

import android.bluetooth.BluetoothSocket;

public interface BluetoothIncomingCallback
{
	void onIncomingRequestAccept(BluetoothSocket requestingDevice, boolean secure);
}
