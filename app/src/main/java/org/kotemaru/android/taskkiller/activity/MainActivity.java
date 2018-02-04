package org.kotemaru.android.taskkiller.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.kotemaru.android.taskkiller.MyApplication;
import org.kotemaru.android.taskkiller.R;
import org.kotemaru.android.taskkiller.monitor.ProcessMonitor;
import org.kotemaru.android.taskkiller.monitor.ItemInfo;
import org.kotemaru.android.taskkiller.persistent.Config;
import org.kotemaru.android.taskkiller.receiver.ScreenOffReceiver;
import org.kotemaru.android.taskkiller.service.InstanceKeepService;
import org.kotemaru.android.taskkiller.widget.GraphView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private ListView mListView;
    private ProcessAdapter mProcessAdapter;
    private Handler mUiHandler = new Handler(Looper.getMainLooper());
    private ProcessMonitor mProcessMonitor;
    private List<ItemInfo> mItemInfoList = new ArrayList<ItemInfo>();
    private PackageManager mPackageManager;
    private HashMap<String, Drawable> mIconMap = new HashMap<String, Drawable>();
    private StringBuilder mSbuf = new StringBuilder(10);
    private Drawable mDefaultIcon;
    private MyApplication mApplication;

    private ListMode mListMode = ListMode.PACKAGE;

    private enum ListMode {
        PACKAGE, PROCESS
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(this, InstanceKeepService.class);
        startService(intent);
        mPackageManager = getPackageManager();
        mApplication = (MyApplication) getApplication();

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_manage);
        mDefaultIcon = new BitmapDrawable(getResources(), bitmap);

        //mProcessMonitor = new ProcessMonitor(this);

        //ScreenOffReceiver.create(this);

        mProcessAdapter = new ProcessAdapter();
        mListView = (ListView) findViewById(R.id.listView);
        mListView.setAdapter(mProcessAdapter);

        ActionBar bar = getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        ActionBar.Tab tab1 = bar.newTab().setText("Package").setTabListener(new TabListener(ListMode.PACKAGE));
        ActionBar.Tab tab2 = bar.newTab().setText("Process").setTabListener(new TabListener(ListMode.PROCESS));
        bar.addTab(tab1);
        bar.addTab(tab2);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ItemInfo info = mProcessAdapter.getItemInfo(position);
                Log.d(TAG, "onItemClick:" + info.getPackageName());
                if (info.getPackageName() == null) return;

                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + info.getPackageName()));
                startActivity(intent);
            }
        });

    }

    private class TabListener implements ActionBar.TabListener {
        private final ListMode mUseListMode;

        public TabListener(ListMode listMode) {
            mUseListMode = listMode;
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            mListMode = mUseListMode;
            refresh();
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mApplication.setMainActivity(this);
        refresh();
    }

    public void postRefresh() {
        mUiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        }, 1L);
    }

    private void refresh() {
        if (mListMode == ListMode.PACKAGE) {
            mApplication.getProcessMonitor().getPackageInfoList(mItemInfoList);
        } else {
            mApplication.getProcessMonitor().refresh(this, false);
            mApplication.getProcessMonitor().getProcessInfoList(mItemInfoList, Config.isShowSystemProcess());
        }

        if (!Config.isProcessMonitoring() || Config.getSortCondition() == Config.SortCondition.NAME) {
            Collections.sort(mItemInfoList, new Comparator<ItemInfo>() {
                @Override
                public int compare(ItemInfo lhs, ItemInfo rhs) {
                    int res = lhs.getTitle().compareTo(rhs.getTitle());
                    if (res != 0) return res;
                    return lhs.getSubTitle().compareTo(rhs.getSubTitle());
                }
            });
        } else if (Config.getSortCondition() == Config.SortCondition.CPU_LATEST) {
            Collections.sort(mItemInfoList, new Comparator<ItemInfo>() {
                @Override
                public int compare(ItemInfo lhs, ItemInfo rhs) {
                    return (int) ((rhs.getCpuRateLog().lastCpuRate - lhs.getCpuRateLog().lastCpuRate) * 1000000);
                }
            });
        } else {
            Collections.sort(mItemInfoList, new Comparator<ItemInfo>() {
                @Override
                public int compare(ItemInfo lhs, ItemInfo rhs) {
                    return (int) ((rhs.getCpuRateLog().avgCpuRate - lhs.getCpuRateLog().avgCpuRate) * 1000000);
                }
            });
        }
        mProcessAdapter.setItemInfos(mItemInfoList);
    }

    @Override
    protected void onPause() {
        mApplication.setMainActivity(null);
        super.onPause();
    }

    class ProcessAdapter extends BaseAdapter {
        private List<ItemInfo> mItemInfos;
        private LayoutInflater mInflater = getLayoutInflater();

        ProcessAdapter() {
        }

        public void setItemInfos(List<ItemInfo> ItemInfos) {
            this.mItemInfos = ItemInfos;
            this.notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (mItemInfos == null) return 0;
            return mItemInfos.size();
        }

        @Override
        public Object getItem(int position) {
            return getItemInfo(position);
        }

        public ItemInfo getItemInfo(int position) {
            if (mItemInfos == null) return null;
            return mItemInfos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View view, ViewGroup parent) {
            if (view == null) {
                view = mInflater.inflate(R.layout.list_item, null, false);
            }
            ImageView icon = (ImageView) view.findViewById(R.id.icon);
            TextView subtitle = (TextView) view.findViewById(R.id.subtitle);
            TextView title = (TextView) view.findViewById(R.id.title);
            TextView rate = (TextView) view.findViewById(R.id.rate);
            GraphView graph = (GraphView) view.findViewById(R.id.graph);
            CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkBox);

            ItemInfo info = mItemInfos.get(position);
            final String packageName = info.getPackageName();
            icon.setImageDrawable(getIcon(packageName));
            title.setText(info.getTitle());
            subtitle.setText(info.getSubTitle());

            rate.setText(toPerString(Config.getSortCondition() == Config.SortCondition.CPU_AVERAGE
                    ? info.getCpuRateLog().avgCpuRate : info.getCpuRateLog().lastCpuRate));
            graph.setData(info.getCpuRateLog().log);

            rate.setVisibility(Config.isProcessMonitoring() ? View.VISIBLE : View.INVISIBLE);
            graph.setVisibility(Config.isProcessMonitoring() ? View.VISIBLE : View.INVISIBLE);
            checkBox.setVisibility(packageName != null ? View.VISIBLE : View.INVISIBLE);
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(mApplication.isKillOnSleep(packageName));
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Log.d(TAG, "onCheckedChanged:" + packageName + ":" + isChecked);
                    mApplication.setKillOnSleep(packageName, isChecked);
                }
            });

            return view;
        }
    }


    private Drawable getIcon(String packageName) {
        Drawable icon = mIconMap.get(packageName);
        if (icon == null) {
            try {
                ApplicationInfo info = mPackageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                icon = info.loadIcon(mPackageManager);
            } catch (PackageManager.NameNotFoundException e) {
                icon = mDefaultIcon;
            }
            mIconMap.put(packageName, icon);
        }
        return icon;
    }


    private String toPerString(float f) {
        int v = (int) (f * 10000F);
        int intPart = v / 100;
        int decPart = v % 100;

        mSbuf.setLength(0);
        mSbuf.append(intPart).append('.');
        if (decPart < 10) mSbuf.append('0');
        mSbuf.append(decPart).append('%');
        return mSbuf.toString();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        /*
        MenuItem item;
        item = menu.add(0, R.string.action_refresh, 0, R.string.action_refresh);
        item.setIcon(R.drawable.refresh);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        item = menu.add(0, R.string.action_settings, 0, R.string.action_settings);
        item.setIcon(R.drawable.settings);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
*/
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, PrefActivity.class));
                return true;
            case R.id.action_refresh:
                mApplication.getProcessMonitor().reload(this);
                refresh();
                return true;
            case R.id.action_kill:
                ScreenOffReceiver.killProcesses();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mApplication.getProcessMonitor().reload(mApplication);
                        refresh();
                    }
                });
                return true;
            case R.id.action_help:
                intent = new Intent(this, WebViewActivity.class);
                intent.setData(Uri.parse("file:///android_asset/help.html"));
                startActivity(intent);
                return true;
            case R.id.action_about:
                intent = new Intent(this, WebViewActivity.class);
                intent.setData(Uri.parse("file:///android_asset/about.html"));
                startActivity(intent);
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

}
