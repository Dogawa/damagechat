package com.rdc.damage.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.rdc.damage.R;
import com.rdc.damage.bean.MessageBean;
import com.rdc.damage.config.Protocol;
import com.rdc.damage.manager.SocketManager;
import com.rdc.damage.service.TCPIPService;
import com.rdc.damage.thread.SocketThread;
import com.rdc.damage.util.MacroDefine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * Created by HeartDawn on 2017/5/4.
 */
public class NetchartActivity extends AppCompatActivity implements View.OnClickListener, PaletteView.Callback,Handler.Callback{


    private View mUndoView;
    private View mRedoView;
    private View mPenView;
    private View mEraserView;
    private View mClearView;
    private PaletteView mPaletteView;
    private ProgressDialog mSaveProgressDlg;
    private static final int MSG_SAVE_SUCCESS = 1;
    private static final int MSG_SAVE_FAILED = 2;
    private Handler mHandler;


    private Button mBtnBg;

    private TCPIPService.TcpIpBinder tcpIpBinder = null;

    private boolean IsServer = false;
    private String IpAddress = null;

    private TcpIpBroadReceiver broadcastInformationReceiver = null;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            tcpIpBinder = (TCPIPService.TcpIpBinder)service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            tcpIpBinder = null;
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.netchart_activity);

        Bundle mBundle = this.getIntent().getExtras();

        IsServer = mBundle.getBoolean("IsServer");
        IpAddress = mBundle.getString("IpAddress");

        Intent serviceIntent = new Intent(NetchartActivity.this, TCPIPService.class);
        serviceIntent.putExtra("IpAddress",IpAddress);
        serviceIntent.putExtra("IsServer",IsServer);
        bindService(serviceIntent,serviceConnection,Context.BIND_AUTO_CREATE);

        broadcastInformationReceiver = new TcpIpBroadReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MacroDefine.BroadcastFilter.TCP_IP_BROADCASTSERVICEFILTER);
        registerReceiver(broadcastInformationReceiver, intentFilter);

        initPaletteView();
        findViewById(R.id.btnStore).setOnClickListener(this);


        if(IsServer) {
            mPaletteView.setBackground(getDrawable(R.drawable.demage_base));
        }

        mBtnBg = (Button)findViewById(R.id.btnShareScreen);
        mBtnBg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(IsServer){
                    Toast.makeText(NetchartActivity.this,"等待对方来发送",Toast.LENGTH_LONG).show();
                    return;
                }
                SocketThread socketThread = SocketManager.getInstance().getSocketThreadByIp(IpAddress);

                MessageBean shareMessage = new MessageBean(IpAddress);
                shareMessage.setMine(true);
                shareMessage.setMsgType(Protocol.SHARE_SCREEN);
                if (socketThread != null){
                    socketThread.sendMsg(shareMessage, -1);
                }

//                mPaletteView.setBackground(getDrawable(R.drawable.demage_base));

//                StringBuffer stringBuffer = new StringBuffer();
//                stringBuffer.append("Image;");
//                stringBuffer.append("text");
//                tcpIpBinder.addHandler(stringBuffer.toString());
                mPaletteView.setBackground(getDrawable(R.drawable.demage_base));

//                mPaletteView.dispatchTouchEvent((float)409.78656,(float)210.61053,0);
//                mPaletteView.dispatchTouchEvent((float)417.5566,(float)217.20905,2);
//                mPaletteView.dispatchTouchEvent((float)545.3176,(float)477.84088,2);
//                mPaletteView.dispatchTouchEvent((float)568.7038,(float)518.3256,1);

            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastInformationReceiver);
        unbindService(serviceConnection);
        mHandler.removeMessages(MSG_SAVE_FAILED);
        mHandler.removeMessages(MSG_SAVE_SUCCESS);
    }

    private void initSaveProgressDlg(){
        mSaveProgressDlg = new ProgressDialog(this);
        mSaveProgressDlg.setMessage("正在保存,请稍候...");
        mSaveProgressDlg.setCancelable(false);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what){
            case MSG_SAVE_FAILED:
                mSaveProgressDlg.dismiss();
                Toast.makeText(this,"保存失败",Toast.LENGTH_SHORT).show();
                break;
            case MSG_SAVE_SUCCESS:
                mSaveProgressDlg.dismiss();
                Toast.makeText(this,"图片已保存",Toast.LENGTH_SHORT).show();
                break;
        }
        return true;
    }

    private static void scanFile(Context context, String filePath) {
        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scanIntent.setData(Uri.fromFile(new File(filePath)));
        context.sendBroadcast(scanIntent);
    }

    private static String saveImage(Bitmap bmp, int quality) {
        if (bmp == null) {
            return null;
        }
        File appDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (appDir == null) {
            return null;
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, fos);
            fos.flush();
            return file.getAbsolutePath();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    public void onUndoRedoStatusChanged() {
        mUndoView.setEnabled(mPaletteView.canUndo());
        mRedoView.setEnabled(mPaletteView.canRedo());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.undo:
                mPaletteView.undo();
                break;
            case R.id.redo:
                mPaletteView.redo();
                break;
            case R.id.pen:
                v.setSelected(true);
                mEraserView.setSelected(false);
                mPaletteView.setMode(PaletteView.Mode.DRAW);
                break;
            case R.id.eraser:
                v.setSelected(true);
                mPenView.setSelected(false);
                mPaletteView.setMode(PaletteView.Mode.ERASER);
                break;
            case R.id.clear:
                mPaletteView.clear();
                break;
            case R.id.btnStore:
                saveImage();
                break;

        }
    }

    private void initPaletteView() {
        mPaletteView = (PaletteView) findViewById(R.id.palette);
        mPaletteView.setCallback(this);

        mUndoView = findViewById(R.id.undo);
        mRedoView = findViewById(R.id.redo);
        mPenView = findViewById(R.id.pen);
        mPenView.setSelected(true);
        mEraserView = findViewById(R.id.eraser);
        mClearView = findViewById(R.id.clear);

        mUndoView.setOnClickListener(this);
        mRedoView.setOnClickListener(this);
        mPenView.setOnClickListener(this);
        mEraserView.setOnClickListener(this);
        mClearView.setOnClickListener(this);

        mUndoView.setEnabled(false);
        mRedoView.setEnabled(false);

        mHandler = new Handler(this);
    }


    public class TcpIpBroadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String strMsg = intent.getStringExtra("MSG");
            String[] strArr = strMsg.split(";");
            if(strArr[0].equals("canvas")){
                float x = Float.parseFloat(strArr[1]);
                float y = Float.parseFloat(strArr[2]);
                int action = Integer.parseInt(strArr[3]);
                mPaletteView.dispatchTouchEvent(x, y, action);
                Log.e("NetchartActivity", "TcpIpBroadReceiver onReceive();" + strMsg);
            }else{
//                mPaletteView.setBackground(getDrawable(R.drawable.demage_base));
            }
        }
    }

    @Override
    public void onPassTouchEvent(float x, float y, int action) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("canvas;" + Float.toString(x) + ";" + Float.toString(y) + ";" + Integer.toString(action));
        Log.e("NetchartActivity", "onPassTouchEvent " + stringBuffer.toString());
        tcpIpBinder.addHandler(stringBuffer.toString());
    }

    private void saveImage() {
        if(mSaveProgressDlg==null){
            initSaveProgressDlg();
        }
        mSaveProgressDlg.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bm = mPaletteView.buildBitmap();
                String savedFile = saveImage(bm, 100);
                if (savedFile != null) {
                    scanFile(getApplicationContext(), savedFile);
                    mHandler.obtainMessage(MSG_SAVE_SUCCESS).sendToTarget();
                }else{
                    mHandler.obtainMessage(MSG_SAVE_FAILED).sendToTarget();
                }
            }
        }).start();
    }
}
