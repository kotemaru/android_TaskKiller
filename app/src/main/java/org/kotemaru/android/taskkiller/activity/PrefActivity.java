package org.kotemaru.android.taskkiller.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;

import org.kotemaru.android.taskkiller.persistent.Config;
import org.kotemaru.android.taskkiller.MyApplication;
import org.kotemaru.android.taskkiller.R;
import org.kotemaru.android.taskkiller.receiver.AlarmReceiver;
import org.kotemaru.android.taskkiller.receiver.MonitorReceiver;

public class PrefActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefFragment fragment = new PrefFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
    }

    public void setChanged(boolean b) {
        Intent intent = new Intent();
        // 設定変更があったことを呼び出し元に返す。
        setResult(b ? RESULT_OK : RESULT_CANCELED, intent);
    }

    public static class PrefFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        private MyApplication mApplication;

        private boolean mBackupProcessMonitoring;
        private int mBackupMonitorLoggingCount;
        private int mBackupMonitorInterval;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref); // res/xml/pref.xml
            mApplication = (MyApplication) getActivity().getApplication();
        }

        @Override
        public void onResume() {
            super.onResume();
            resetSummary();
            mBackupProcessMonitoring = Config.isProcessMonitoring();
            mBackupMonitorLoggingCount = Config.getMonitorLoggingCount();
            mBackupMonitorInterval = Config.getMonitorInterval();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            AlarmReceiver.applyKillRepeat();
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences paramSharedPreferences, String paramString) {
            resetSummary();
            ((PrefActivity) getActivity()).setChanged(true);

            boolean isProcessMonitoring = Config.isProcessMonitoring();
            if (mBackupProcessMonitoring != isProcessMonitoring) {
                mBackupProcessMonitoring = isProcessMonitoring;
                MonitorReceiver.ctrlMonitor(getActivity(), isProcessMonitoring);
            }

            int interval = Config.getMonitorInterval();
            Log.d("DEBUG", "interval=" + interval + ":" + mBackupMonitorInterval + ":" + isProcessMonitoring);
            if (isProcessMonitoring && mBackupMonitorInterval != interval) {
                Log.d("DEBUG", "restart monitor");
                mBackupMonitorInterval = interval;
                MonitorReceiver.stopMonitor(getActivity());
                mApplication.getProcessMonitor().resetCpuLog();
                MonitorReceiver.startMonitor(getActivity());
            }

            int monitorLoggingCount = Config.getMonitorLoggingCount();
            if (mBackupMonitorLoggingCount != monitorLoggingCount) {
                mBackupMonitorLoggingCount = monitorLoggingCount;
                mApplication.getProcessMonitor().resetCpuLog();
            }

        }

        // CheckBoxを除く項目のSummaryに現在値を設定する。
        public void resetSummary() {
            SharedPreferences sharedPrefs = getPreferenceManager().getSharedPreferences();
            PreferenceScreen screen = this.getPreferenceScreen();
            for (int i = 0; i < screen.getPreferenceCount(); i++) {
                Preference pref = screen.getPreference(i);
                if (pref instanceof CheckBoxPreference || pref instanceof SwitchPreference)
                    continue;
                String key = pref.getKey();
                CharSequence val = sharedPrefs.getString(key, "");
                if (pref instanceof ListPreference) {
                    ListPreference listPref = (ListPreference) pref;
                    val = listPref.getEntry();
                }
                pref.setSummary(val);
            }
        }
    }
}