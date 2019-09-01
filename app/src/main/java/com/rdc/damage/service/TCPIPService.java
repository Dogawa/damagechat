package com.rdc.damage.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;


import com.rdc.damage.util.MacroDefine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by HeartDawn on 2017/5/4.
 */
public class TCPIPService extends Service {
    // 消息队列
    protected Queue<String> lstQueue = null;
    private String IpAddress;
    private final int mListenPort = 9090;
    private Socket socket = null;
    private ServerSocket mServerSocket = null;
    private boolean isServer = false;
    private boolean IsRun = false;

    private BufferedReader mBufferedReader = null;
    private PrintWriter mPrintWriter = null;

    private TcpIpBinder binder = null;

    @Override
    public void onCreate() {
        super.onCreate();
        lstQueue = new LinkedList<String>();

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isOk = false;
                do {
                    isOk = con();
                } while (!isOk);
                try {
                    if (isServer) {
                        socket = mServerSocket.accept();
                    }
                    mBufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                    mPrintWriter = new PrintWriter(socket.getOutputStream(), true);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (IsRun){
                                try {
                                    if (!lstQueue.isEmpty()) {
                                        String msg = lstQueue.poll();
                                        System.out.println(msg);
                                        Log.d("MSG=V",msg);
                                        mPrintWriter.println(msg);
                                        mPrintWriter.flush();
                                    }else {Thread.sleep(100);}
                                }catch (Exception e){
                                    e.printStackTrace();;
                                }
                            }
                        }
                    }).start();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (IsRun){
                                try {
                                    /*
                                    StringBuffer stringBuffer = new StringBuffer();
                                    while ((msg = mBufferedReader.readLine()) != null){
                                        stringBuffer.append(msg);
                                        Log.d("MSG=V",msg);
                                    }*/
                                    String msg = mBufferedReader.readLine();
                                    Log.d("MSG=V","FROM=="+ msg);
                                    if(msg!=null && msg.length() != 0){
                                        Intent broadCastIntent = new Intent();
                                        broadCastIntent.setAction(MacroDefine.BroadcastFilter.TCP_IP_BROADCASTSERVICEFILTER);
                                        broadCastIntent.putExtra("MSG", msg);
                                        sendBroadcast(broadCastIntent);
                                    }
                                }catch (Exception ex){
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }).start();

                } catch (Exception ex) {
                    IsRun = false;
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    private boolean con() {
        try {
            if (isServer) {
                mServerSocket = new ServerSocket(mListenPort);
            } else {
                socket = new Socket(InetAddress.getByName(IpAddress), mListenPort);
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    private void initServer(Intent intent) {
        IpAddress = intent.getStringExtra("IpAddress");
        isServer = intent.getBooleanExtra("IsServer", false);
        IsRun = true;
        binder = new TcpIpBinder();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        initServer(intent);
        return  binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public class  TcpIpBinder extends Binder {

        public void addHandler(String msg) {
            lstQueue.add(msg);
        }

        TCPIPService getService(){ return  TCPIPService.this;}
    }
}
