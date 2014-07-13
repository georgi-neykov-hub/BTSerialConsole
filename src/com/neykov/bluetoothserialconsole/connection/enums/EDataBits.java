package com.neykov.bluetoothserialconsole.connection.enums;

import android.os.Parcel;
import android.os.Parcelable;

//Enumerate Databit constant
	public enum EDataBits implements Parcelable
	{
		D5, D6, D7, D8;

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

		public static final Creator<EDataBits> CREATOR = new Creator<EDataBits>()
		{
			@Override
			public EDataBits createFromParcel(final Parcel source)
			{
				return EDataBits.values()[source.readInt()];
			}

			@Override
			public EDataBits[] newArray(final int size)
			{
				return new EDataBits[size];
			}
		};
	}

	