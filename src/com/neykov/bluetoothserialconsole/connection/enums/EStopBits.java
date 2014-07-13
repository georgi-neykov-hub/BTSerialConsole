package com.neykov.bluetoothserialconsole.connection.enums;

import android.os.Parcel;
import android.os.Parcelable;

//Enumerate Stopbit constant
	public enum EStopBits implements Parcelable
	{
		S1, S2;

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

		public static final Creator<EStopBits> CREATOR = new Creator<EStopBits>()
		{
			@Override
			public EStopBits createFromParcel(final Parcel source)
			{
				return EStopBits.values()[source.readInt()];
			}

			@Override
			public EStopBits[] newArray(final int size)
			{
				return new EStopBits[size];
			}
		};
	}

	