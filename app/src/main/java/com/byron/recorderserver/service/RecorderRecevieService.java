package com.byron.recorderserver.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.byron.recorderserver.interfaces.ReceiveRecorderCallback;
import com.byron.recorderserver.utils.IPUtil;
import com.byron.recorderserver.utils.ThreadPoolUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Jie.Chen on 2017/10/17.
 */

public class RecorderRecevieService extends Service {
    private String TAG = this.getClass().getSimpleName();
    private RecorderRecevierBinder mRecevierBinder;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (mRecevierBinder == null) {
            mRecevierBinder = new RecorderRecevierBinder();
        }
        return mRecevierBinder;
    }

    public class RecorderRecevierBinder extends Binder {
        private ServerSocket mServerSocket = null;

        /**
         * 打开服务
         *
         * @param port
         */
        public void startServer(int port) {
            //ServerSocket的默认参数空的构造方法用途是，允许服务器在绑定到特定端口之前，先设置一些ServerSocket选项
            //否则一旦服务器与特定端口绑定，有些选项就不能在改变了，比如SO_REUSEADDR选项
            try {
                mServerSocket = new ServerSocket();
                //设定允许端口重用，无论Socket还是ServetSocket都要在绑定端口之前设定此属性，否则端口绑定后再设置此属性是徒劳的
                mServerSocket.setReuseAddress(true);
                //服务器绑定端口
                mServerSocket.bind(new InetSocketAddress(port));
                //从连接请求队列中(backlog)取出一个客户的连接请求，然后创建与客户连接的Socket对象，并将它返回
                //如果队列中没有连接请求，accept()就会一直等待，直到接收到了连接请求才返回
                //Socket socket = serverSocket.accept();
            } catch (Exception e) {
                Log.e(TAG, "服务器启动失败，堆栈轨迹如下：");
                e.printStackTrace();
                //isBound()---判断是否已与一个端口绑定，只要ServerSocket已经与一个端口绑定，即使它已被关闭，isBound()也会返回true
                //isClosed()--判断ServerSocket是否关闭，只有执行了close()方法，isClosed()才返回true
                //isClosed()--否则即使ServerSocket还没有和特定端口绑定，isClosed()也会返回false
                //下面的判断就是要确定一个有引用的，已经与特定端口绑定，并且还没有被关闭的ServerSocket
                if (null != mServerSocket && mServerSocket.isBound() && !mServerSocket.isClosed()) {
                    try {
                        //serverSocket.close()可以使服务器释放占用的端口，并断开与所有客户机的连接
                        //当服务器程序运行结束时，即使没有执行serverSocket.close()方法，操作系统也会释放此服务器占用的端口
                        //因此服务器程序并不一定要在结束前执行serverSocket.close()方法
                        //但某些情景下若希望及时释放服务器端口，以便其它程序能够占用该端口，则可显式调用serverSocket.close()
                        mServerSocket.close();
                        Log.i(TAG, "startServer : 服务器已关闭");
                    } catch (IOException ioe) {
                        //ignore the exception
                    }
                }
            }
            Log.i(TAG, "startServer: 服务器启动成功，开始监听" + mServerSocket.getLocalSocketAddress());
        }

        /**
         * 接收手机传送过来的音频数据
         */
        public void receiveAudio(ReceiveRecorderCallback callback) {
            if (mServerSocket != null && mServerSocket.isBound() && !mServerSocket.isClosed()) {
                ThreadPoolUtil.getInstance().receiveAudio(mServerSocket,callback);
            }
        }

        /**
         * 获取本机的IP地址
         *
         * @return
         */
        public String getLocalIPAddress() {
            return IPUtil.getLocalIPAddress(true);
        }
    }


}
