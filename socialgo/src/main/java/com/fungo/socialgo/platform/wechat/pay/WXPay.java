package com.fungo.socialgo.platform.wechat.pay;

import android.text.TextUtils;

import com.fungo.socialgo.exception.SocialError;
import com.fungo.socialgo.listener.OnPayListener;
import com.tencent.mm.opensdk.constants.Build;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 微信支付
 * Created by tsy on 16/6/1.
 */
public class WXPay {

    private static WXPay mWXPay;
    private IWXAPI mWXApi;
    private String mPayParam;
    private OnPayListener mListener;

    public WXPay(IWXAPI wxApi) {
        mWXApi = wxApi;
    }

    public static void initWxApi(IWXAPI wxApi) {
        if (mWXPay == null) {
            mWXPay = new WXPay(wxApi);
        }
    }

    public static WXPay getInstance() {
        return mWXPay;
    }

    public IWXAPI getWXApi() {
        return mWXApi;
    }

    /**
     * 发起微信支付
     */
    public void doPay(String pay_param, OnPayListener listener) {
        mPayParam = pay_param;
        mListener = listener;

        if (!check()) {
            if (mListener != null) {
                mListener.onError(new SocialError(SocialError.CODE_NOT_INSTALL));
            }
            return;
        }

        JSONObject param;
        try {
            param = new JSONObject(mPayParam);
        } catch (JSONException e) {
            e.printStackTrace();
            if (mListener != null) {
                mListener.onError(new SocialError(SocialError.CODE_PAY_PARAM_ERROR));
            }
            return;
        }
        if (TextUtils.isEmpty(param.optString("appid")) || TextUtils.isEmpty(param.optString("partnerid"))
                || TextUtils.isEmpty(param.optString("prepayid")) || TextUtils.isEmpty(param.optString("package")) ||
                TextUtils.isEmpty(param.optString("noncestr")) || TextUtils.isEmpty(param.optString("timestamp")) ||
                TextUtils.isEmpty(param.optString("sign"))) {
            if (mListener != null) {
                mListener.onError(new SocialError(SocialError.CODE_PAY_PARAM_ERROR));
            }
            return;
        }

        PayReq req = new PayReq();
        req.appId = param.optString("appid");
        req.partnerId = param.optString("partnerid");
        req.prepayId = param.optString("prepayid");
        req.packageValue = param.optString("package");
        req.nonceStr = param.optString("noncestr");
        req.timeStamp = param.optString("timestamp");
        req.sign = param.optString("sign");

        mWXApi.sendReq(req);
    }

    //支付回调响应
    public void onResp(int error_code) {
        if (mListener == null) {
            return;
        }

        if (error_code == 0) {   //成功
            mListener.onSuccess();
        } else if (error_code == -1) {   //错误
            mListener.onError(new SocialError(SocialError.CODE_PAY_ERROR));
        } else if (error_code == -2) {   //取消
            mListener.onCancel();
        }

        mListener = null;
    }

    //检测是否支持微信支付
    private boolean check() {
        return mWXApi.isWXAppInstalled() && mWXApi.getWXAppSupportAPI() >= Build.PAY_SUPPORTED_SDK_INT;
    }
}