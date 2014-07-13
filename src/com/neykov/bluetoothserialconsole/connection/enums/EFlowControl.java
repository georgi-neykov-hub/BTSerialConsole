package com.neykov.bluetoothserialconsole.connection.enums;

import android.os.Parcel;
import android.os.Parcelable;

//Enumerate Flowcontrol constant
	public enum EFlowControl implements Parcelable
	{
		OFF, RTSCTS, RFRCTS, // not yet implemented
		DTRDSR, // not yet implemented
		XONXOFF;// not yet implemented;

		@Override
		public int describeContents()
		{
			return 0;
		}

		@Override
		public void writeToParcel(final Parcel dest, final int flags)
		{
			dest.writeInt(ordinal());
		}

		public static final Creator<EFlowControl> CREATOR = new Creator<EFlowControl>()
		{
			@Override
			public EFlowControl createFromParcel(final Parcel source)
			{
				return EFlowControl.values()[source.readInt()];
			}

			@Override
			public EFlowControl[] newArray(final int size)
			{
				return new EFlowControl[size];
			}
		};
	}