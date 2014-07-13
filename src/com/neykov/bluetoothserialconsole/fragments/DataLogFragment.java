package com.neykov.bluetoothserialconsole.fragments;

import com.neykov.bluetoothserialconsole.R;
import com.neykov.bluetoothserialconsole.datalog.EEntryType;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.bluetooth.BluetoothDevice;
import android.text.Editable;
import android.text.Html;
import android.text.format.Time;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.TextView.OnEditorActionListener;

/**
 * A simple {@link android.app.Fragment} subclass. Activities that contain this fragment must implement the
 * {@link DataLogFragment.OnDataLogFragmentInteractionListener} interface to handle interaction events. Use the
 * {@link DataLogFragment#newInstance} factory method to create an instance of this fragment.
 * 
 */
public class DataLogFragment extends Fragment
{
	public static final String TAG = "DataLogFragment";
	private static final String FORMAT_DATA_LOG_SEND = "<font color=\"%1s\"><b>|%2s|Sent:> </b>%3s</font><br/>";
	private static final String FORMAT_DATA_LOG_RECEIVE = "<font color=\"%1s\"><b>|%2s|Received:> </b>%3s</font><br/>";
	private static final String FORMAT_DATA_LOG_ERROR = "<font color=\"%1s\"><b>|%2s|Error:> </b>%3s</font><br/>";
	private static final String FORMAT_DATA_LOG_CONNECTION = "<font color=\"%1s\"><b>|%s2|Connection:> </b>%3s</font><br/>";
	private static final String FORMAT_DATE = "%k:%M:%S";

	// Default colors for different types of log data.
	private static final String COLOR_SEND = "#0099CC";
	private static final String COLOR_RECEIVE = "#339933";
	private static final String COLOR_ERROR = "#FF5050";
	private static final String COLOR_CONNECTION = "#E6E6E6";

	// Carriage Return and Line feed symbols constants
	private static final String LF = "\n";
	private static final String CR = "\r";

	// The ID of the attached DeviceDriver
	private static final String ARGS_BLUETOOTH_DEVICE = "device";
	
	private String deviceName;
	private boolean isConnected;
	private Editable logTextEditable;
	
	
	// Private members for Buttons, Text Boxes, etc...
	private TextView logTextView;
	private EditText commandLine;
	private Button sendButton;
	private Button saveLogButton;
	private Button clearLogButton;
	private ScrollView scrollView;
	private CheckBox appendLFCheckBox;
	private CheckBox appendCRCheckBox;
	private BluetoothDevice mDevice;
	
	private final Runnable ScrollDownRunnable = new Runnable()
	{
		
		@Override
		public void run()
		{
			scrollView.fullScroll(View.FOCUS_DOWN);
		}
	};
	
	private final Time currentTime = new Time();
	
	
	// OnClickListeners for the buttons.
	private final OnClickListener onClearLogClickListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			// TODO Auto-generated method stub
		}

	};
	private final OnClickListener onSaveLogClickListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{

		}
	};
	private final OnClickListener onSendClickListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			String outCommand = commandLine.getText().toString();

			// If checked, add CR / LF.
			if (appendLFCheckBox.isChecked())
			{
				outCommand+=LF;
			}
			if (appendCRCheckBox.isChecked())
			{
				outCommand+=CR;
			}

			mListener.onFragmentDataSend(DataLogFragment.this, outCommand);
			commandLine.setText("");
			scrollView.post(ScrollDownRunnable);
		}
	};
	private final OnEditorActionListener onKeyboardSendListener = new OnEditorActionListener()
	{

		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
		{
			if (actionId == EditorInfo.IME_ACTION_SEND && sendButton.isEnabled())
			{
				sendButton.performClick();
				return true;
			}
			return false;
		}
	};

	private OnDataLogFragmentInteractionListener mListener;

	/**
	 * Use this factory method to create a new instance of this fragment
	 * 
	 * @return A new instance of fragment BlankFragment.
	 */

	public static DataLogFragment newInstance(BluetoothDevice device)
	{
		DataLogFragment fragment = new DataLogFragment();
		Bundle args = new Bundle();
		args.putParcelable(ARGS_BLUETOOTH_DEVICE, device);
		fragment.setArguments(args);
		return fragment;
	}

	public DataLogFragment()
	{
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.setHasOptionsMenu(true);
		this.mDevice = getArguments().getParcelable(ARGS_BLUETOOTH_DEVICE);
		this.deviceName = mDevice.getAddress();
		this.logTextEditable = Editable.Factory.getInstance().newEditable("");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// Inflate layout and get View for main container.
		View v = inflater.inflate(R.layout.fragment_data_log, container, false);

		// Get view instances.
		logTextView = (TextView) v.findViewById(R.id.fragment_datalog_logtext);
		scrollView = (ScrollView) v.findViewById(R.id.fragment_datalog_scrollview);
		commandLine = (EditText) v.findViewById(R.id.fragment_datalog_cmdline);
		sendButton = (Button) v.findViewById(R.id.fragment_datalog_send);
		clearLogButton = (Button) v.findViewById(R.id.fragment_datalog_clearlog);
		saveLogButton = (Button) v.findViewById(R.id.fragment_datalog_savelog);
		appendLFCheckBox = (CheckBox) v.findViewById(R.id.fragment_datalog_lf);
		appendCRCheckBox = (CheckBox) v.findViewById(R.id.fragment_datalog_cr);

		// Set the contents of logTextView.
		logTextView.setText(logTextEditable,BufferType.EDITABLE);
		
		//Scroll to bottom
		scrollView.post(ScrollDownRunnable);
		
		// Connect the views with OnClickListeners
		sendButton.setOnClickListener(onSendClickListener);
		clearLogButton.setOnClickListener(onClearLogClickListener);
		saveLogButton.setOnClickListener(onSaveLogClickListener);
		commandLine.setOnEditorActionListener(onKeyboardSendListener);

		// Disable controls.
		toggleControls(false);

		return v;
	}
	
	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		try
		{
			mListener = (OnDataLogFragmentInteractionListener) activity;
		} catch (ClassCastException e)
		{
			throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach()
	{
		super.onDetach();
		mListener = null;
	}

	@Override
	public void onResume()
	{
		super.onResume();
		toggleControls(isConnected);
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		
		//TODO: Save Log Text to a file.
	}
	
	public void onConnect()
	{
		isConnected = true;
		if(isResumed())	toggleControls(true);
	}

	public void onDisconnect()
	{
		isConnected = true;
		if(isResumed())	toggleControls(false);
	}

	public void onConnectStart()
	{
		if(isVisible())
		{
		}
	}

	public void onDisconnectStart()
	{
	
		
	}

	public void onDataReceive(String data, EEntryType type)
	{
		// Get current time.
		currentTime.setToNow();
		String currDate = currentTime.format(FORMAT_DATE);
		
		// Compose content based on data type.
		String html;
		switch (type)
		{
			case ERROR:
				html = String.format(FORMAT_DATA_LOG_ERROR, COLOR_ERROR, currDate, data);
				break;
			case RECEIVE:
				html = String.format(FORMAT_DATA_LOG_RECEIVE, COLOR_RECEIVE, currDate, data);
				break;
			case SEND:
				html = String.format(FORMAT_DATA_LOG_SEND, COLOR_SEND, currDate, data);
				break;
			default:
				html = String.format(FORMAT_DATA_LOG_CONNECTION, COLOR_CONNECTION, currDate, data);
				break;
		}
		// Append composed data and scroll TextView to bottom.
		if(!isHidden())
		{
			logTextView.append(Html.fromHtml(html));
			scrollView.post(ScrollDownRunnable);
		}
		else
			logTextEditable.append(Html.fromHtml(html));
	}

	private void toggleControls(boolean enable)
	{
		if(this.isAdded())
		{
			sendButton.setEnabled(enable);
			commandLine.setEnabled(enable);
		}
	}

	public String getDeviceName()
	{
		return deviceName;
	}
	
	public BluetoothDevice getDevice()
	{
		return mDevice;
	}
	
	/**
	 * This interface must be implemented by activities that contain this fragment to allow an interaction in this
	 * fragment to be communicated to the activity and potentially other fragments contained in that activity.
	 * <p>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html" >Communicating with Other
	 * Fragments</a> for more information.
	 */
	public interface OnDataLogFragmentInteractionListener
	{
		public void onFragmentDataSend(DataLogFragment sender, String data);
	}

}
