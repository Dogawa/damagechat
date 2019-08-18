package com.rdc.p2p.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.rdc.p2p.R;
import com.rdc.p2p.service.TCPIPService;
import com.rdc.p2p.util.MacroDefine;


/**
 * Created by HeartDawn on 2017/5/4.
 */
public class NetchartActivity extends AppCompatActivity {

    private ImageView iv;
    //原图
    private Bitmap bitsrc;
    //拷贝图
    private Bitmap bitcopy;
    private Canvas canvas;
    private Paint paint;
    private int startX;
    private int startY;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.netchart_activity);

        iv = (ImageView) findViewById(R.id.imageViewBg);

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

        // setBitmap();
        /*
        paint = new Paint();
        paint.setStrokeWidth(5);
        paint.setColor(Color.GREEN);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        baseBitmap = Bitmap.createBitmap(displayMetrics.widthPixels, displayMetrics.heightPixels,
                Bitmap.Config.ARGB_8888);
        System.out.println("图宽度："+iv.getWidth());
        System.out.println("图高度："+iv.getHeight());
        canvas = new Canvas(baseBitmap);
        canvas.drawColor(Color.WHITE);*/

        iv.setOnTouchListener(new View.OnTouchListener() {

            // 设置手指开始的坐标
            int startX;
            int startY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: // 手指第一次接触屏幕
                        startX = (int) event.getX();
                        startY = (int) event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:// 手指在屏幕上滑动
                        int newX = (int) event.getX();
                        int newY = (int) event.getY();

                        StringBuffer stringBuffer = new StringBuffer();
                        stringBuffer.append("canvas;").append(startX+";").append(startY + ";").append(newX + ";").append(newY);

                        // canvas.drawLine(startX, startY, newX, newY, paint);
                        canvas.drawLine(startX, startY, newX, newY, paint);

                        // 重新更新画笔的开始位置
                        startX = (int) event.getX();
                        startY = (int) event.getY();
                        iv.setImageBitmap(bitcopy);

                        tcpIpBinder.addHandler(stringBuffer.toString());
                        break;
                    case MotionEvent.ACTION_UP: // 手指离开屏幕
                        break;

                    default:
                        break;
                }
                return true;
            }
        });

        mBtnBg = (Button)findViewById(R.id.btnShareScreen);
        mBtnBg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // iv.setImageResource(R.drawable.bg);
                if(IsServer){
                    Toast.makeText(NetchartActivity.this,"等待对方来发送",Toast.LENGTH_LONG).show();
                    return;
                }
                bitsrc = BitmapFactory.decodeResource(getResources(), R.drawable.bg);
                StringBuffer stringBuffer = new StringBuffer();
                stringBuffer.append("Image;");
                // stringBuffer.append(MacroDefine.convertIconToString(bitsrc));
                stringBuffer.append("text");
                tcpIpBinder.addHandler(stringBuffer.toString());
                setBitmap();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastInformationReceiver);
        unbindService(serviceConnection);
    }

    public void setBitmap() {
        // 加载画画板的背景图
        // bitsrc = BitmapFactory.decodeResource(getResources(), R.drawable.bg);
        // 创建图片副本
        // 1.在内存中创建一个与原图一模一样大小的bitmap对象，创建与原图大小一致的白纸
        bitcopy = Bitmap.createBitmap(bitsrc.getWidth(), bitsrc.getHeight(),
                bitsrc.getConfig());
        // 2.创建画笔对象
        paint = new Paint();
        paint.setStrokeWidth(5);
        paint.setColor(Color.GREEN);
        // 3.创建画板对象，把白纸铺在画板上
        canvas = new Canvas(bitcopy);
        // 4.开始作画，把原图的内容绘制在白纸上
        canvas.drawBitmap(bitsrc, new Matrix(), paint);
        // 5.将绘制的图放入imageview中
        iv.setImageBitmap(bitcopy);
    }

    public class TcpIpBroadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String strMsg = intent.getStringExtra("MSG");
            String[] strArr = strMsg.split(";");
            if(strArr[0].equals("canvas")){
                int startx = Integer.parseInt(strArr[1]);
                int starty = Integer.parseInt(strArr[2]);
                int endx = Integer.parseInt(strArr[3]);
                int endy = Integer.parseInt(strArr[4]);
                canvas.drawLine(startx, starty, endx, endy, paint);
                iv.setImageBitmap(bitcopy);
            }else{
                String image = strArr[1];
                // bitsrc = MacroDefine.convertStringToBitmap(image);
                bitsrc = BitmapFactory.decodeResource(getResources(), R.drawable.bg);
                // iv.setImageBitmap(bitmap);
                setBitmap();
            }
        }
    }
}
