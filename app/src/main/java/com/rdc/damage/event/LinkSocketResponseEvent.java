package com.rdc.damage.event;

import com.rdc.damage.bean.PeerBean;

/**
 * Created by Lin Yaotian on 2018/5/30.
 */
public class LinkSocketResponseEvent {
    private boolean state;
    private PeerBean peerBean;

    public PeerBean getPeerBean() {
        return peerBean;
    }

    public void setPeerBean(PeerBean peerBean) {
        this.peerBean = peerBean;
    }

    public LinkSocketResponseEvent(boolean state, PeerBean peerBean){
        this.state = state;
        this.peerBean = peerBean;
    }

    public boolean isState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }
}
