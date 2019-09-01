package com.rdc.damage.presenter;

import com.rdc.damage.base.BasePresenter;
import com.rdc.damage.contract.ScanDeviceContract;
import com.rdc.damage.model.ScanDeviceModel;

import java.util.List;

/**
 * Created by Lin Yaotian on 2018/5/14.
 */
public class ScanDevicePresenter extends BasePresenter<ScanDeviceContract.View> implements ScanDeviceContract.Presenter {

    private ScanDeviceContract.Model model;

    public ScanDevicePresenter(){
        model = new ScanDeviceModel(this);
    }

    @Override
    public void scanDevice() {
        model.scanDevice();
    }

    @Override
    public void scanDeviceSuccess(List<String> ipList) {
        if (isAttachView()){
            getMvpView().scanDeviceSuccess(ipList);
        }
    }

    @Override
    public void scanDeviceError(String message) {
        if (isAttachView()){
            getMvpView().scanDeviceError(message);
        }
    }

}
