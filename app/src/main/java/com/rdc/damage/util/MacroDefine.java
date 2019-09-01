package com.rdc.damage.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

/**
 * Created by HeartDawn on 2017/5/2.
 */
public class MacroDefine {
    /***
     * 广播Action的设定
     */
    public interface BroadcastFilter {
        public final String BROADCASTSERVICEFILTER = "netchat.heardawn.netchat.com.netchat.service.BroadcastService";
        public final String TCP_IP_BROADCASTSERVICEFILTER = "netchat.heardawn.netchat.com.netchat.service.TCPIPServeice";
    }

    public static String convertIconToString(Bitmap bitmap){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);
        byte[] appicon = stream.toByteArray();
        return Base64.encodeToString(appicon,Base64.DEFAULT);
    }

    public static Bitmap convertStringToBitmap(String tt){
        try{
            byte[] bitmapArray;
            bitmapArray = Base64.decode(tt,Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bitmapArray,0,bitmapArray.length);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return  null;
    }
}
