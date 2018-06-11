package com.quirodev.sac.getUsageTime;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Calendar;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;
import static android.os.Process.myUid;

public class UsagePresenter_D  implements UsageContract.Presenter {
    private static final int flags = PackageManager.GET_META_DATA;

    private UsageStatsManager usageStatsManager;
    private PackageManager packageManager;
    private UsageContract.View view;
    private final Context context;
    private long totalTime;



    public UsagePresenter_D(Context context, UsageContract.View view) {
        usageStatsManager = (UsageStatsManager) context.getSystemService("usagestats");
        packageManager = context.getPackageManager();
        this.view = view;
        this.context = context;
    }


    private long getStartTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        return calendar.getTimeInMillis();
    }
    @Override
    public void retrieveUsageStats() {
        if (!checkForPermission(context)) {
            view.onUserHasNoPermission();
            return;
        }

        totalTime = 0;

        List<String> installedApps = getInstalledAppList();

        List<UsageStats> mystats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
                getStartTime(),System.currentTimeMillis());
        HashMap<String,UsageStats> pleasework = new HashMap<>();

        for(int i = 0 ; i < mystats.size();i++) {
            pleasework.put(mystats.get(i).getPackageName(), mystats.get(i));
        }

        List<UsageStatsWrapper> finalList = buildUsageStatsWrapper(installedApps, pleasework);
        for(int i=0;i<finalList.size();i++){
            totalTime += finalList.get(i).getUsageStats().getTotalTimeInForeground();
        }

        view.onUsageStatsRetrieved(finalList);



    }
    public long getTotal() {
        return totalTime;
    }

    private boolean checkForPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(OPSTR_GET_USAGE_STATS, myUid(), context.getPackageName());
        return mode == MODE_ALLOWED;
    }

    private List<String> getInstalledAppList(){
        List<ApplicationInfo> infos = packageManager.getInstalledApplications(flags);
        List<String> installedApps = new ArrayList<>();
        for (ApplicationInfo info : infos){
            //if(info.packageName.startsWith("com.android"))
            installedApps.add(info.packageName);
        }
        return installedApps;
    }



private List<UsageStatsWrapper> buildUsageStatsWrapper(List<String> packageNames, HashMap<String, UsageStats> usageStatses) {

    List<UsageStatsWrapper> list = new ArrayList<>();
    for (String name : packageNames) {
        for (String key : usageStatses.keySet()) {
            if (name.equals(key)){
                list.add(fromUsageStat(usageStatses.get(key)));
            }
        }
    }
    Collections.sort(list);
    return list;
}

    private UsageStatsWrapper fromUsageStat(String packageName) throws IllegalArgumentException {
        try {
            ApplicationInfo ai = packageManager.getApplicationInfo(packageName, 0);
            return new UsageStatsWrapper(null, packageManager.getApplicationIcon(ai), packageManager.getApplicationLabel(ai).toString());

        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }
    private UsageStatsWrapper fromUsageStat(UsageStats usageStats) throws IllegalArgumentException {
        try {
            ApplicationInfo ai = packageManager.getApplicationInfo(usageStats.getPackageName(), 0);
            return new UsageStatsWrapper(usageStats, packageManager.getApplicationIcon(ai), packageManager.getApplicationLabel(ai).toString());

        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }


}
