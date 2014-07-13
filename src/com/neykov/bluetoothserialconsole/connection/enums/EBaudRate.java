package com.neykov.bluetoothserialconsole.connection.enums;

import android.os.Parcel;
import android.os.Parcelable;

	// Enumerate Baudrate constant
	public enum EBaudRate implements Parcelable {
		B75, B150, B300, B600, B1200, B1800, B2400, B4800, B9600, B19200, B38400, B57600, B115200, B230400, B460800, B614400, B921600, B1228800, B2457600, B3000000, B6000000;



	    @Override
	    public int describeContents() {
	        return 0;
	    }

	    @Override
	    public void writeToParcel(final Parcel dest, final int flags) {
	        dest.writeInt(ordinal());
	    }

	    public static final Creator<EBaudRate> CREATOR = new Creator<EBaudRate>() {
	        @Override
	        public EBaudRate createFromParcel(final Parcel source) {
	            return EBaudRate.values()[source.readInt()];
	        }

	        @Override
	        public EBaudRate[] newArray(final int size) {
	            return new EBaudRate[size];
	        }
	    };
	}
