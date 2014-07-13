package com.neykov.bluetoothserialconsole.connection;

import android.os.Parcel;
import android.os.Parcelable;

import com.neykov.bluetoothserialconsole.connection.enums.*;

public class ConnectionParameters implements Parcelable
{
	private EBaudRate baudRate;
	private EDataBits dataBits;
	private EStopBits stopBits;
	private EParity parity;
	private EFlowControl flowControl;

	public ConnectionParameters()
	{
		this.baudRate = EBaudRate.B9600;
		this.dataBits = EDataBits.D8;
		this.stopBits = EStopBits.S1;
		this.flowControl = EFlowControl.OFF;
		this.parity = EParity.EVEN;
	}
	public ConnectionParameters(EBaudRate baud, EDataBits databits, EStopBits stopbits, 
			EFlowControl flowcontrol, EParity parity)
	{
		this.baudRate = baud;
		this.dataBits = databits;
		this.stopBits = stopbits;
		this.flowControl = flowcontrol;
		this.parity = parity;
	}
	public ConnectionParameters(ConnectionParameters params)
	{
		this.baudRate = params.getBaudRate();
		this.dataBits = params.getDataBits();
		this.stopBits = params.getStopBits();
		this.flowControl = params.getFlowControl();
		this.parity = params.getParity();
	}
	
	/**
	 * @return the baudRate
	 */
	public EBaudRate getBaudRate()
	{
		return baudRate;
	}
	/**
	 * @param baudRate the baudRate to set
	 */
	public void setBaudRate(EBaudRate baudRate)
	{
		this.baudRate = baudRate;
	}
	/**
	 * @return the dataBits
	 */
	public EDataBits getDataBits()
	{
		return dataBits;
	}
	/**
	 * @param dataBits the dataBits to set
	 */
	public void setDataBits(EDataBits dataBits)
	{
		this.dataBits = dataBits;
	}
	/**
	 * @return the stopBits
	 */
	public EStopBits getStopBits()
	{
		return stopBits;
	}
	/**
	 * @param stopBits the stopBits to set
	 */
	public void setStopBits(EStopBits stopBits)
	{
		this.stopBits = stopBits;
	}
	/**
	 * @return the parity
	 */
	public EParity getParity()
	{
		return parity;
	}
	/**
	 * @param parity the parity to set
	 */
	public void setParity(EParity parity)
	{
		this.parity = parity;
	}
	/**
	 * @return the flowControl
	 */
	public EFlowControl getFlowControl()
	{
		return flowControl;
	}
	/**
	 * @param flowControl the flowControl to set
	 */
	public void setFlowControl(EFlowControl flowControl)
	{
		this.flowControl = flowControl;
	}
	
	public void SetParametersFrom(ConnectionParameters connParams)
	{
		this.baudRate = connParams.baudRate;
		this.dataBits = connParams.dataBits;
		this.flowControl = connParams.flowControl;
		this.parity = connParams.parity;
		this.stopBits = connParams.stopBits;	
	}
	
	// Implemented Parcelable Overridden methods
	public int describeContents()
	{
		return 0;
	}

	/** save object in parcel */
	public void writeToParcel(Parcel out, int flags)
	{
		out.writeParcelable(baudRate, PARCELABLE_WRITE_RETURN_VALUE);
		out.writeParcelable(dataBits, PARCELABLE_WRITE_RETURN_VALUE);
		out.writeParcelable(stopBits, PARCELABLE_WRITE_RETURN_VALUE);
		out.writeParcelable(flowControl, PARCELABLE_WRITE_RETURN_VALUE);
		out.writeParcelable(parity, PARCELABLE_WRITE_RETURN_VALUE);
	}

	public static final Parcelable.Creator<ConnectionParameters> CREATOR = new Parcelable.Creator<ConnectionParameters>()
	{
		public ConnectionParameters createFromParcel(Parcel in)
		{
			return new ConnectionParameters(in);
		}

		public ConnectionParameters[] newArray(int size)
		{
			return new ConnectionParameters[size];
		}
	};

	/** recreate object from parcel */
	private ConnectionParameters(Parcel in)
	{
		baudRate = in.readParcelable(EBaudRate.class.getClassLoader());
		dataBits = in.readParcelable(EDataBits.class.getClassLoader());
		stopBits = in.readParcelable(EStopBits.class.getClassLoader());
		flowControl = in.readParcelable(EFlowControl.class.getClassLoader());
		parity = in.readParcelable(EParity.class.getClassLoader());
	}

	@Override
	public String toString()
	{
		return "\t\tBaudrate: " + baudRate.toString() +
				" | Databits: " + dataBits.toString() +
				" | Stopbits: " + stopBits.toString() +
				" | Parity: " + parity.toString() +
				" | Flow control" + flowControl.toString();
	}
}
