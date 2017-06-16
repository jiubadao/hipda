package net.jejer.hipda.ui.setting;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.preference.Preference;
import android.text.TextUtils;

import net.jejer.hipda.R;
import net.jejer.hipda.async.UpdateHelper;
import net.jejer.hipda.bean.HiSettingsHelper;
import net.jejer.hipda.glide.GlideHelper;
import net.jejer.hipda.job.SettingChangedEvent;
import net.jejer.hipda.ui.AboutFragment;
import net.jejer.hipda.ui.FragmentUtils;
import net.jejer.hipda.ui.HiApplication;
import net.jejer.hipda.ui.MainFrameActivity;
import net.jejer.hipda.ui.SettingActivity;
import net.jejer.hipda.utils.Constants;
import net.jejer.hipda.utils.HiUtils;
import net.jejer.hipda.utils.NotificationMgr;
import net.jejer.hipda.utils.Utils;

import org.greenrobot.eventbus.EventBus;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * main setting fragment
 * Created by GreenSkinMonster on 2015-09-11.
 */
public class SettingMainFragment extends BaseSettingFragment {

    private int mScreenOrietation;
    private String mTheme;
    private int mPrimaryColor;
    private List<Integer> mForums;
    private Set<String> mFreqMenus;
    private boolean mNavBarColored;
    private String mFont;
    static boolean mCacheCleared;
    private boolean mNightSwitchEnabled;
    private String mIcon;
    private String mForumServer;
    private boolean mTrustAllCerts;
    private boolean mCircleAvatar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_main);

        bindPreferenceSummaryToValue();

        // "nested_#" is the <Preference android:key="nested" android:persistent="false"/>
        for (int i = 1; i <= 5; i++) {
            final int screenKey = i;
            findPreference("nested_" + i).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                private long mLastClickTime;

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //avoid double click
                    long currentClickTime = System.currentTimeMillis();
                    long elapsedTime = currentClickTime - mLastClickTime;
                    mLastClickTime = currentClickTime;
                    if (elapsedTime <= Constants.MIN_CLICK_INTERVAL)
                        return true;

                    Intent intent = new Intent(getActivity(), SettingActivity.class);
                    intent.putExtra(SettingNestedFragment.TAG_KEY, screenKey);
                    ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(getActivity(), R.anim.slide_in_left, R.anim.no_anim);
                    ActivityCompat.startActivity(getActivity(), intent, options.toBundle());
                    return true;
                }
            });
        }

        mScreenOrietation = HiSettingsHelper.getInstance().getScreenOrietation();
        mTheme = HiSettingsHelper.getInstance().getActiveTheme();
        mPrimaryColor = HiSettingsHelper.getInstance().getPrimaryColor();
        mForums = HiSettingsHelper.getInstance().getForums();
        mFreqMenus = HiSettingsHelper.getInstance().getFreqMenus();
        mNavBarColored = HiSettingsHelper.getInstance().isNavBarColored();
        mNightSwitchEnabled = !TextUtils.isEmpty(HiSettingsHelper.getInstance().getNightTheme());
        mFont = HiSettingsHelper.getInstance().getFont();
        mIcon = HiSettingsHelper.getInstance().getStringValue(HiSettingsHelper.PERF_ICON, "0");
        mForumServer = HiSettingsHelper.getInstance().getForumServer();
        mTrustAllCerts = HiSettingsHelper.getInstance().isTrustAllCerts();
        mCircleAvatar = HiSettingsHelper.getInstance().isCircleAvatar();

        setActionBarTitle(R.string.title_fragment_settings);
    }

    @Override
    public void onStop() {
        super.onStop();

        HiSettingsHelper.getInstance().reload();

        SettingChangedEvent event = new SettingChangedEvent();

        if (HiSettingsHelper.getInstance().isNotiTaskEnabled()) {
            if (NotificationMgr.isAlarmRuning(getActivity()))
                NotificationMgr.cancelAlarm(getActivity());
            NotificationMgr.startAlarm(getActivity());
        } else {
            NotificationMgr.cancelAlarm(getActivity());
        }

        if (!HiSettingsHelper.getInstance().isGestureBack() && getActivity() != null)
            ((MainFrameActivity) getActivity()).drawer.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

        String newIcon = HiSettingsHelper.getInstance().getStringValue(HiSettingsHelper.PERF_ICON, "0");
        if (TextUtils.isDigitsOnly(newIcon) && !mIcon.equals(newIcon))
            setIcon(Integer.parseInt(newIcon));

        HiSettingsHelper.getInstance().resetImageAutoLoadSize();

        if (HiSettingsHelper.getInstance().isCircleAvatar() != mCircleAvatar) {
            GlideHelper.initDefaultFiles();
        }

        if (mCacheCleared
                || HiSettingsHelper.getInstance().getScreenOrietation() != mScreenOrietation
                || !HiSettingsHelper.getInstance().getActiveTheme().equals(mTheme)
                || HiSettingsHelper.getInstance().getPrimaryColor() != mPrimaryColor
                || !HiSettingsHelper.getInstance().getForums().equals(mForums)
                || !HiSettingsHelper.getInstance().getFreqMenus().equals(mFreqMenus)
                || HiSettingsHelper.getInstance().isNavBarColored() != mNavBarColored
                || TextUtils.isEmpty(HiSettingsHelper.getInstance().getNightTheme()) == mNightSwitchEnabled
                || !HiSettingsHelper.getInstance().getFont().equals(mFont)
                || !HiSettingsHelper.getInstance().getForumServer().equals(mForumServer)
                || HiSettingsHelper.getInstance().isTrustAllCerts() != mTrustAllCerts
                || !mIcon.equals(newIcon)) {
            mCacheCleared = false;
            event.mRestart = true;
        }
        EventBus.getDefault().postSticky(event);
    }

    private void bindPreferenceSummaryToValue() {
        Preference dialogPref = findPreference(HiSettingsHelper.PERF_ABOUT);

        dialogPref.setSummary(HiApplication.getAppVersion()
                + (Utils.isFromGooglePlay(getActivity()) ? " (Google Play)" : ""));
        dialogPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                Intent intent = new Intent(getActivity(), SettingActivity.class);
                intent.putExtra(AboutFragment.TAG_KEY, AboutFragment.TAG_KEY);
                ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(getActivity(), R.anim.slide_in_left, R.anim.no_anim);
                ActivityCompat.startActivity(getActivity(), intent, options.toBundle());
                return true;
            }
        });

        final Preference checkPreference = findPreference(HiSettingsHelper.PERF_LAST_UPDATE_CHECK);
        checkPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                checkPreference.setSummary("上次检查 ：" + Utils.formatDate(new Date()));
                new UpdateHelper(getActivity(), false).check();
                return true;
            }
        });
        Date lastCheckTime = HiSettingsHelper.getInstance().getLastUpdateCheckTime();
        if (lastCheckTime != null) {
            checkPreference.setSummary("上次检查 ：" + Utils.formatDate(lastCheckTime));
        } else {
            checkPreference.setSummary("上次检查 ：- ");
        }

        Preference supportPreference = findPreference(HiSettingsHelper.PERF_SUPPORT);
        supportPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                setHasOptionsMenu(false);
                FragmentUtils.show(getActivity(),
                        FragmentUtils.parseUrl(HiUtils.BaseUrl + "viewthread.php?tid=" + HiUtils.CLIENT_TID));
                return true;
            }
        });

    }

    private void setIcon(int icon) {
        Context ctx = getActivity();
        PackageManager pm = getActivity().getPackageManager();

        pm.setComponentEnabledSetting(
                new ComponentName(ctx, "net.jejer.hipda.ng.MainActivity-Original"),
                icon == Constants.ICON_ORIGINAL ?
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );

        pm.setComponentEnabledSetting(
                new ComponentName(ctx, "net.jejer.hipda.ng.MainActivity-Circle"),
                icon == Constants.ICON_ROUND ?
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );
    }

}
