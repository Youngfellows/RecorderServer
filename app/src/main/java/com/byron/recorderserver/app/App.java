package com.byron.recorderserver.app;

import android.app.Application;
import android.content.Context;

/**
 * Created by Jie.Chen on 2017/10/17.
 */

public class App extends Application {
    private Context mContext;

    public Context getContext() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mContext = this;

        // TODO: 2017/10/17 开发接收音频数据的服务
    }
}
