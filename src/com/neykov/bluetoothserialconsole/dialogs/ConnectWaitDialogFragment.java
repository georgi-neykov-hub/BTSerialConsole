package com.neykov.bluetoothserialconsole.dialogs;

import com.neykov.bluetoothserialconsole.R;
import android.app.DialogFragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A simple {@link android.app.Fragment} subclass. Use the {@link ConnectWaitDialogFragment#newInstance}
 * factory method to create an instance of this fragment.
 * 
 */
public class ConnectWaitDialogFragment extends DialogFragment
{
	public static final String TAG = "ConnectWaitDialogFragment";
	
	private static final String ARG_MESSAGE = "dialog message";

	private TextView messageTextView;

	/**
	 * Use this factory method to create a new instance of this fragment using the provided parameters.
	 * 
	 * @param initialMessage
	 *            Parameter 1.
	 * @param param2
	 *            Parameter 2.
	 * @return A new instance of fragment ConnectWaitDialogFragment.
	 */
	public static ConnectWaitDialogFragment newInstance(String initialMessage)
	{
		ConnectWaitDialogFragment fragment = new ConnectWaitDialogFragment();
		Bundle args = new Bundle();
		args.putString(ARG_MESSAGE, initialMessage);
		fragment.setArguments(args);
		return fragment;
	}

	public static ConnectWaitDialogFragment newInstance()
	{
		ConnectWaitDialogFragment fragment = new ConnectWaitDialogFragment();
		return fragment;
	}

	public ConnectWaitDialogFragment()
	{
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Set some style options.
		this.setCancelable(false);
		this.setStyle(STYLE_NO_INPUT, 0);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// Inflate layout from resource and get the message TextView.
		View layout = inflater.inflate(R.layout.fragment_connect_wait, container, false);
		layout.bringToFront();
		messageTextView = (TextView) layout.findViewById(R.id.connect_wait_message);

		if (savedInstanceState == null && getArguments() != null)
		{
			messageTextView.setText(getArguments().getString(ARG_MESSAGE));
		}

		return layout;
	}

	public void setMessage(int resId)
	{
		if (messageTextView != null)
		{
			messageTextView.setText(resId);
		}
	}

	public void setMessage(CharSequence text)
	{
		if (messageTextView != null)
		{
			messageTextView.setText(text);
		}
	}
}
