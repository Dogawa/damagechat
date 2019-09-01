package com.rdc.damage.listener;

import android.view.View;

import com.rdc.damage.widget.PlayerSoundView;

/**
 * Created by Lin Yaotian on 2018/5/30.
 */
public interface OnItemViewClickListener {
    void onImageClick(int position);

    void onTextLongClick(int position, View view);

    void onFileClick(int position);

    void onAlterClick(int position);

    void onAudioClick(PlayerSoundView mPsvPlaySound, String audioUrl);

    void onVideoClick(int position);
}
