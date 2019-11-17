package com.rdc.damage.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
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

    private static final String TAG = "NetchartActivity";
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

    private static final int REQUEST_CODE = 1000;
    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;
    private MediaRecorder mMediaRecorder;
    private static final int DISPLAY_WIDTH = 720;
    private static final int DISPLAY_HEIGHT = 1280;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionCallback mMediaProjectionCallback;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_PERMISSIONS = 10;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }



    private Button mBtnBg;
    private Button mBtnStopSharing;

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


        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;

        mMediaRecorder = new MediaRecorder();

        mProjectionManager = (MediaProjectionManager) getSystemService
                (Context.MEDIA_PROJECTION_SERVICE);


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
                HandlerThread handlerThread = new HandlerThread("start");
                handlerThread.start();
                final SocketThread socketThread = SocketManager.getInstance().getSocketThreadByIp(IpAddress);


                if (socketThread != null){
                    new Handler(handlerThread.getLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            MessageBean shareMessage = new MessageBean(IpAddress);
                            shareMessage.setMine(true);
                            shareMessage.setMsgType(Protocol.SHARE_SCREEN);
                            socketThread.sendMsg(shareMessage, -1);
                        }
                    });

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
                doRecordScreen();

            }
        });

        mBtnStopSharing = findViewById(R.id.btnStopShareScreen);
        mBtnStopSharing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendStopMessage();
                onStopScreenRecord();
                finish();
            }
        });

    }

    private void sendStopMessage() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("stop");
        tcpIpBinder.addHandler(stringBuffer.toString());
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
            if (strArr[0].equals("canvas")) {
                float x = Float.parseFloat(strArr[1]);
                float y = Float.parseFloat(strArr[2]);
                int action = Integer.parseInt(strArr[3]);
                mPaletteView.dispatchTouchEvent(x, y, action);
                Log.e("NetchartActivity", "TcpIpBroadReceiver onReceive();" + strMsg);
            }else if(strArr[0].equals("stop")) {
                finish();
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

    private void shareScreen() {
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
            return;
        }
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("MainActivity",
                DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null
                /*Handler*/);
    }

    private void initRecorder() {
        try {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mMediaRecorder.setOutputFile(Environment
                    .getExternalStoragePublicDirectory(Environment
                            .DIRECTORY_DOWNLOADS) + "/video.mp4");
            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
            mMediaRecorder.setVideoFrameRate(30);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation + 90);
            mMediaRecorder.setOrientationHint(orientation);
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doRecordScreen() {
        if (ContextCompat.checkSelfPermission(NetchartActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) + ContextCompat
                .checkSelfPermission(NetchartActivity.this,
                        Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale
                    (NetchartActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale
                            (NetchartActivity.this, Manifest.permission.RECORD_AUDIO)) {
                Snackbar.make(findViewById(android.R.id.content), "Please enable Microphone and Storage permissions.",
                        Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ActivityCompat.requestPermissions(NetchartActivity.this,
                                        new String[]{Manifest.permission
                                                .WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                                        REQUEST_PERMISSIONS);
                            }
                        }).show();
            } else {
                ActivityCompat.requestPermissions(NetchartActivity.this,
                        new String[]{Manifest.permission
                                .WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                        REQUEST_PERMISSIONS);
            }
        } else {
            onStartScreenRecord();
        }
    }

    private void onStartScreenRecord() {
        initRecorder();
        shareScreen();
    }

    private void onStopScreenRecord() {
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        Log.v(TAG, "Stopping Recording");
        stopScreenSharing();
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            Log.v(TAG, "Recording Stopped");
            mMediaProjection = null;
            stopScreenSharing();
        }
    }

    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        //mMediaRecorder.release(); //If used: mMediaRecorder object cannot
        // be reused again
        destroyMediaProjection();
    }
    private void destroyMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        Log.i(TAG, "MediaProjection Stopped");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                if ((grantResults.length > 0) && (grantResults[0] +
                        grantResults[1]) == PackageManager.PERMISSION_GRANTED) {
                    onStartScreenRecord();
                } else {
                    Snackbar.make(findViewById(android.R.id.content), "Please enable Microphone and Storage permissions.",
                            Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                                    intent.setData(Uri.parse("package:" + getPackageName()));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                    startActivity(intent);
                                }
                            }).show();
                }
                return;
            }
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this,
                    "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            return;
        }
        mMediaProjectionCallback = new MediaProjectionCallback();
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }
}
