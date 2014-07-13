package com.neykov.bluetoothserialconsole.connection.enums;

import android.os.Parcel;
import android.os.Parcelable;

//Enumerate Parity constant
	public enum EParity implements Parcelable
	{
		NONE, ODD, EVEN;

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

		public static final Creator<EParity> CREATOR = new Creator<EParity>()
		{
			@Override
			public EParity createFromParcel(final Parcel source)
			{
				return EParity.values()[source.readInt()];
			}

			@Override
			public EParity[] newArray(final int size)
			{
				return new EParity[size];
			}
		};
	}

	