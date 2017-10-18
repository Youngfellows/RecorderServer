package com.byron.recorderserver;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.byron.recorderserver.interfaces.ReceiveRecorderCallback;
import com.byron.recorderserver.service.RecorderRecevieService;
import com.byron.recorderserver.utils.Constants;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    public String TAG = this.getClass().getSimpleName();
    private TextView mTvIp;
    private TextView mTvReceiveInfo;
    private SimpleDateFormat mSimpleDateFormat;
    private RecorderRecevieService.RecorderRecevierBinder mRecorderRecevierBinder;
    private ServiceConnection conn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i(TAG, "--------服务绑定成功了-----");
            mRecorderRecevierBinder = (RecorderRecevieService.RecorderRecevierBinder) iBinder;
            showIpAddress();
            startReceive();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "--------服务解绑了-----");
            mRecorderRecevierBinder = null;
        }
    };

    /**
     * 开启线程接收手机传递过来的消息
     */
    private void startReceive() {
        if (mRecorderRecevierBinder != null) {
            mRecorderRecevierBinder.startServer(Constants.PORT);
            mRecorderRecevierBinder.receiveAudio(new ReceiveRecorderCallback() {
                @Override
                public void receiveInfo(final String info) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTvReceiveInfo.append("\n[" + mSimpleDateFormat.format(new Date()) + "]" + info);
                            mTvReceiveInfo.append("\n\n");
                        }
                    });
                }
            });
        }
    }

    /**
     * 显示本机IP
     */
    private void showIpAddress() {
        if (mRecorderRecevierBinder != null) {
            String ipAddress = mRecorderRecevierBinder.getLocalIPAddress();
            mTvIp.setText("本机IP: " + ipAddress + " 端口: " + Constants.PORT);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
        initListener();
    }

    private void initView() {
        mTvIp = findView(R.id.tv_ip_info);//显示IP地址
        mTvReceiveInfo = findView(R.id.tv_receive_info);//显示接收到的信息
    }

    private void initData() {
        mSimpleDateFormat = new SimpleDateFormat("hh:mm:ss");
        //mTvinfo.append("\n[" + mSimpleDateFormat.format(new Date()) + "]" + info);

        binderService();//开启服务
    }

    private void binderService() {
        Intent intent = new Intent(this, RecorderRecevieService.class);
        startService(intent);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    private void initListener() {

    }

    private <T extends View> T findView(int viewId) {
        return (T) findViewById(viewId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conn);
    }
}
