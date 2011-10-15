package eu.sioux.phenomgame;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
	static String getIpAddress(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("ip", "192.168.24.22");
	}
	static String getPortNumber(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("port", "8888");
	}
}
