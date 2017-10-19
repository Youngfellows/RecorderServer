package com.byron.recorderserver.utils;

import android.util.Log;

import com.byron.recorderserver.interfaces.ReceiveRecorderCallback;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolUtil {
    private String TAG = this.getClass().getSimpleName();
    private ExecutorService mExecutor;

    private ThreadPoolUtil() {
        this.mExecutor = Executors.newCachedThreadPool();
    }

    private static ThreadPoolUtil instance;

    public static ThreadPoolUtil getInstance() {
        if (instance == null) {
            synchronized (ThreadPoolUtil.class) {
                if (instance == null) {
                    instance = new ThreadPoolUtil();
                }
            }
        }
        return instance;
    }

    /**
     * 接收数据
     *
     * @param serverSocket
     * @param callback
     */
    public void receiveAudioTest(final ServerSocket serverSocket, final ReceiveRecorderCallback callback) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    Socket socket = null;
                    try {
                        socket = serverSocket.accept();
                        Log.i(TAG, "New connection accepted " + socket.getRemoteSocketAddress());
                        InputStream in = socket.getInputStream();
                        int dataLen = 0;             //报文前六个字节所标识的完整报文长度
                        int readLen = 0;             //已成功读取的字节数
                        int needDataLen = 0;         //剩余需要读取的报文长度,即报文正文部分的长度
                        byte[] buffer = new byte[6]; //假设报文协议为：前6个字节表示报文长度(不足6位左补0)，第7个字节开始为报文正文
                        while (readLen < 6) {
                            readLen += in.read(buffer, readLen, 6 - readLen);
                        }
                        dataLen = Integer.parseInt(new String(buffer));
                        Log.i(TAG, "dataLen=" + dataLen);

                        byte[] datas = new byte[dataLen];
                        System.arraycopy(buffer, 0, datas, 0, 6);
                        needDataLen = dataLen - 6;
                        while (needDataLen > 0) {
                            readLen = in.read(datas, dataLen - needDataLen, needDataLen);
                            Log.i(TAG, "needDataLen=" + needDataLen + " readLen=" + readLen);
                            needDataLen = needDataLen - readLen;
                        }
                        Log.i(TAG, "Receive request " + new String(datas, "UTF-8"));

                        if (callback != null) {
                            callback.receiveInfo(new String(datas, "UTF-8"));
                        }

                        OutputStream out = socket.getOutputStream();
                        out.write("The server status is opening".getBytes("UTF-8"));
                        //The flush method of OutputStream does nothing
                        //out.flush();
                    } catch (IOException e) {
                        //当服务端正在进行发送数据的操作时，如果客户端断开了连接，那么服务器会抛出一个IOException的子类SocketException异常
                        //java.net.SocketException: Connection reset by peer
                        //这只是服务器与单个客户端通信时遇到了异常，这种异常应该被捕获，使得服务器能够继续与其它客户端通信
                        e.printStackTrace();
                    } finally {
                        if (null != socket) {
                            try {
                                //与一个客户通信结束后要关闭Socket，此时socket的输出流和输入流也都会被关闭
                                //若先后调用shutdownInput()和shutdownOutput()方法，也仅关闭了输入流和输出流，并不等价于调用close()
                                //通信结束后，仍然要调用Socket.close()方法，因为只有该方法才会释放Socket所占用的资源，如占用的本地端口等
                                socket.close();
                            } catch (IOException e) {
                                //ignore the exception
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * 接收服务端发送过来的音频数据
     *
     * @param serverSocket
     * @param callback
     */
    public void receiveAudio(final ServerSocket serverSocket, final ReceiveRecorderCallback callback) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    Socket socket = null;
                    try {
                        socket = serverSocket.accept();
                        Log.i(TAG, "New connection accepted " + socket.getRemoteSocketAddress());

                        InputStream in = socket.getInputStream();//接收TCP相应
                        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int len = -1;
                        while ((len = in.read(buffer)) != -1) {
                            bytesOut.write(buffer, 0, len);
                        }
                        Log.i(TAG, "收到服务器的应答=[" + bytesOut.toString("UTF-8") + "]");
                        if (callback != null) {
                            callback.receiveInfo(bytesOut.toString("UTF-8"));
                        }
                        in.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (socket != null) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        });
    }

}
