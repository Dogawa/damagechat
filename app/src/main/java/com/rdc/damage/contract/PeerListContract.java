package com.rdc.damage.contract;

import com.rdc.damage.bean.MessageBean;
import com.rdc.damage.bean.PeerBean;

import java.util.List;

/**
 * Created by Lin Yaotian on 2018/5/16.
 */
public interface PeerListContract {

    interface View{
        void updatePeerList(List<PeerBean> list);
        void messageReceived(MessageBean messageBean);
        void fileReceiving(MessageBean messageBean);
        void addPeer(PeerBean peerBean);
        void removePeer(String ip);
        void serverSocketError(String msg);
        void linkPeerSuccess(String ip);
        void linkPeerError(String message,String targetIp);
        void initServerSocketSuccess();
        void onStartShareScreen(MessageBean messageBean);
    }

    interface Model{
        void initServerSocket();
        void linkPeers(List<String> list);
        void linkPeer(String targetIp);
        void disconnect();
        boolean isInitServerSocket();
    }

    interface Presenter{
        void disconnect();
        void initServerSocketSuccess();
        void initSocket();
        void linkPeers(List<String> list);
        void linkPeer(String targetIp);
        void linkPeerSuccess(String ip);
        void linkPeerError(String message,String targetIp);
        void updatePeerList(List<PeerBean> list);
        void addPeer(PeerBean peerBean);
        void messageReceived(MessageBean messageBean);
        void removePeer(String ip);
        void serverSocketError(String msg);
        boolean isServerSocketConnected();
        void fileReceiving(MessageBean messageBean);
        void onStartShareScreen(MessageBean messageBean);
    }

}