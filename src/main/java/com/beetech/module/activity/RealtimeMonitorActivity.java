package com.beetech.module.activity;

import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.beetech.module.R;
import com.beetech.module.adapter.ReadDataRealtimeRvAdapter;
import com.beetech.module.bean.ReadDataRealtime;
import com.beetech.module.constant.Constant;
import com.beetech.module.dao.AppLogSDDao;
import com.beetech.module.dao.ReadDataRealtimeSDDao;
import com.beetech.module.fragment.GridSpacingItemDecoration;
import com.beetech.module.listener.BatteryListener;
import com.beetech.module.listener.PhoneStatListener;
import com.beetech.module.service.JobProtectService;
import com.beetech.module.service.ModuleService;
import com.beetech.module.service.PlayerMusicService;
import com.beetech.module.service.RemoteService;
import com.beetech.module.utils.ServiceAliveUtils;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.view.annotation.ViewInject;
import java.util.List;

public class RealtimeMonitorActivity extends AppCompatActivity {
    private final static String TAG = RealtimeMonitorActivity.class.getSimpleName();
    private int refreshInterval = 1000*30; //刷新数据间隔

    private AppLogSDDao appLogSDDao;
    private BatteryListener listener;

    public TelephonyManager mTelephonyManager;
    public PhoneStatListener mListener;

    @ViewInject(R.id.rvReadDataRealtimeData)
    private RecyclerView rvReadDataRealtime;

    private ReadDataRealtimeSDDao readDataRealtimeSDDao;
    private List<ReadDataRealtime> readDataRealtimeList;
    private Integer sensorCount;

    private ReadDataRealtimeRvAdapter readDataRealtimeRvAdapter;
    int spanCount = 4;
    int spacing = 20;
    public ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_realtime_monitor);
        ViewUtils.inject(this);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD, WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);//开机不锁屏 设置
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //屏幕唤醒
        PowerManager pm = (PowerManager) getBaseContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "StartupReceiver:");//最后的参数是LogCat里用的Tag
        wl.acquire();

        //屏幕解锁
        KeyguardManager km= (KeyguardManager) getBaseContext().getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock kl = km.newKeyguardLock("StartupReceiver");//参数是LogCat里用的Tag
        kl.disableKeyguard();

        if(appLogSDDao == null){
            appLogSDDao = new AppLogSDDao(this);
        }

        appLogSDDao.save(TAG + " onCreate");

        startModuleService();

        //电量和插拔电源状态广播监听
        if(listener == null){
            listener = new BatteryListener(this);
            listener.register();
        }

        //获取telephonyManager, 监听信号强度
        if(mListener == null){
            mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            mListener = new PhoneStatListener(this);
            mTelephonyManager.listen(mListener, PhoneStatListener.LISTEN_SIGNAL_STRENGTHS);
        }

        rvReadDataRealtime = (RecyclerView)findViewById(R.id.rvReadDataRealtimeData);
        if(readDataRealtimeSDDao == null){
            readDataRealtimeSDDao = new ReadDataRealtimeSDDao(this);
        }

        rvReadDataRealtime.addItemDecoration(new GridSpacingItemDecoration(spanCount, spacing, true));

        //定时刷新
        handlerRefresh.removeCallbacks(runnableRefresh);
        handlerRefresh.postDelayed(runnableRefresh, 0);
    }

    public void startModuleService(){
        /*如果服务正在运行，直接return*/
        if (!ServiceAliveUtils.isServiceRunning(this, Constant.className_moduleService)){
            /* 启动串口通信服务 */
            startService(new Intent(this, ModuleService.class));

            //开启守护线程 aidl
            startService(new Intent(this, RemoteService.class));

            //循环播放一段无声音频
            Intent intent = new Intent(this,PlayerMusicService.class);
            startService(intent);

            //创建唤醒定时任务
            try {
                //获取JobScheduler 他是一种系统服务
                JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
                jobScheduler.cancelAll();
                JobInfo.Builder builder = new JobInfo.Builder(1024, new ComponentName(getPackageName(), JobProtectService.class.getName()));

                if(Build.VERSION.SDK_INT >= 24) {
                    //android N之后时间必须在15分钟以上
                    //            builder.setMinimumLatency(10 * 1000);
                    builder.setPeriodic(1000 * 60 * 15);
                }else{
                    builder.setPeriodic(1000 * 60 * 15);
                }
                builder.setPersisted(true);
                builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE);
                int schedule = jobScheduler.schedule(builder.build());
                if (schedule <= 0) {
                    Log.w(TAG, "schedule error！");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void queryData(){
        readDataRealtimeList = readDataRealtimeSDDao.queryAll();
        sensorCount = readDataRealtimeList == null ? 0 : readDataRealtimeList.size();
    }

    public void refresh(){
        readDataRealtimeRvAdapter = new ReadDataRealtimeRvAdapter(readDataRealtimeList);
        readDataRealtimeRvAdapter.setOnItemLongClickListener(new ReadDataRealtimeRvAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(View view, int position) {
                ReadDataRealtime readDataRealtime = readDataRealtimeList.get(position);
                Intent intent=new Intent(getBaseContext(), QueryDataActivity.class);
                intent.putExtra("sensorId", readDataRealtime.getSensorId());
                startActivity(intent);
            }
        });
        readDataRealtimeRvAdapter.setOnItemClickListener(new ReadDataRealtimeRvAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                ReadDataRealtime readDataRealtime = readDataRealtimeList.get(position);
                Intent intent=new  Intent(RealtimeMonitorActivity.this, TempLineActivity.class);
                intent.putExtra("sensorId",readDataRealtime.getSensorId());
                startActivity(intent);
            }
        });

        GridLayoutManager mGridLayoutManager = new GridLayoutManager(this, spanCount);
        mGridLayoutManager.setOrientation(LinearLayout.VERTICAL);
        rvReadDataRealtime.setLayoutManager(mGridLayoutManager);

        rvReadDataRealtime.setAdapter(readDataRealtimeRvAdapter);
        readDataRealtimeRvAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handlerRefresh.removeCallbacks(runnableRefresh);
        handlerRefresh.postDelayed(runnableRefresh, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) {
            listener.unregister();
        }

//        new ModuleUtils(this).free(); //我们的应用要一直运行
        appLogSDDao.save(TAG+" onDestroy");
    }

    //定时刷新
    private Handler handlerRefresh = new Handler(){};
    Runnable runnableRefresh = new Runnable() {
        @Override
        public void run() {

            RefreshAsyncTask refreshAsyncTask = new RefreshAsyncTask();
            refreshAsyncTask.execute();

            handlerRefresh.postDelayed(this, refreshInterval);
        }
    };

    class RefreshAsyncTask extends AsyncTask<String, Integer, Integer> {

        @Override
        protected void onPreExecute() {
            if(progressDialog == null){

            }
            if(progressDialog != null && !progressDialog.isShowing()) {
                progressDialog.show();
            }
        }

        @Override
        protected Integer doInBackground(String... params) {
            queryData();
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            refresh();
            if(progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_BACK){
            handlerRefresh.removeCallbacks(runnableRefresh);//切换到背景不更新

//            moveTaskToBack(true);
            Intent intent = new Intent();
            intent.setClass(RealtimeMonitorActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            startActivity(intent);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}