package com.neykov.bluetoothserialconsole.fragments;

import java.util.Set;

import com.neykov.bluetoothserialconsole.R;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class DeviceListAdapter extends ArrayAdapter<DeviceListAdapter.DeviceListItem>
{
	private final int rowLayoutResId;
	private final int nameResId;
	private final int adressResId;
	private final int iconResId;
	private final int bondIconResId;
	private final int secureIconResId;

	private static Drawable incomingIcon;
	private static Drawable discoveredIcon;
	private static Drawable secureIcon;
	private static Drawable insecureIcon;
	private static Drawable newDeviceIcon;
	private static Drawable bondedDeviceIcon;
	
	
	public DeviceListAdapter(Context context, int rowLayoutResId, int nameResId, int adressResId, int iconResId, int bondIconResId, int secureIconResId)
	{
		super(context, rowLayoutResId);
		
		incomingIcon = context.getResources().getDrawable(R.drawable.ic_device_list_incoming);
		discoveredIcon = context.getResources().getDrawable(R.drawable.ic_device_list_discovered);
		secureIcon = context.getResources().getDrawable(R.drawable.ic_action_secure);
		insecureIcon = context.getResources().getDrawable(R.drawable.ic_action_insecure);
		newDeviceIcon = context.getResources().getDrawable(R.drawable.ic_action_devices);
		bondedDeviceIcon = context.getResources().getDrawable(R.drawable.ic_action_save);
		
		this.rowLayoutResId = rowLayoutResId;
		this.nameResId = nameResId;
		this.adressResId = adressResId;
		this.iconResId = iconResId;
		this.bondIconResId = bondIconResId;
		this.secureIconResId = secureIconResId;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		// Get recycled view or inflate a new one.
		View rowView = convertView;
		if (rowView == null)
		{
			LayoutInflater inflater =  (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(rowLayoutResId, parent, false);
		}

		//Set view properties.
		DeviceListAdapter.DeviceListItem item = super.getItem(position);
		((TextView) rowView.findViewById(nameResId)).setText(item.getDevice().getName());
		((TextView) rowView.findViewById(adressResId)).setText(item.getDevice().getAddress());
		
		ImageView icon = (ImageView)rowView.findViewById(iconResId);
		if(item.isIncoming()) icon.setImageDrawable(incomingIcon);
		else icon.setImageDrawable(discoveredIcon);
		
		ImageView securityIcon = (ImageView)rowView.findViewById(secureIconResId);
		if(item.isSecure) securityIcon.setImageDrawable(secureIcon);
		else securityIcon.setImageDrawable(insecureIcon);
		
		ImageView bondedIcon = (ImageView)rowView.findViewById(bondIconResId);
		if(item.isBonded) bondedIcon.setImageDrawable(bondedDeviceIcon);
		else bondedIcon.setImageDrawable(newDeviceIcon);
		
		return rowView;
	}

	public void addBondedDevices(Set<BluetoothDevice> bondedSet)
	{
		BluetoothDevice[] bondedDevices = bondedSet.toArray(new BluetoothDevice[0]);
		DeviceListItem[] items = new DeviceListItem[bondedDevices.length];
		for(int i = 0;i<items.length; i++)
		{
			items[i] = new DeviceListItem(bondedDevices[i], false, true, false);
		}
		this.addAll(items);
	}
	
	public enum DeviceListItemType
	{
		DISCOVERED,
		INCOMING;
	}
	
	public class DeviceListItem
	{
		private final BluetoothDevice device;
		private final boolean isSecure;
		private final boolean isIncoming;
		private boolean isBonded;
	
		public DeviceListItem(BluetoothDevice device, boolean isIncoming, boolean isBonded, boolean isSecure)
		{
			this.device = device;
			this.isIncoming = isIncoming;
			this.setBonded(isBonded);
			this.isSecure = isSecure;
		}

		public BluetoothDevice getDevice()
		{
			return device;
		}

		public boolean isSecure()
		{
			return isSecure;
		}

		public boolean isIncoming()
		{
			return isIncoming;
		}

		public boolean isBonded()
		{
			return isBonded;
		}

		public void setBonded(boolean isBonded)
		{
			this.isBonded = isBonded;
		}

		
		
	}
}