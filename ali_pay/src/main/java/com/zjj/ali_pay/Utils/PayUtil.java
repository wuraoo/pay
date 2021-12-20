package com.zjj.ali_pay.Utils;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 支付相关的工具类
 */
@Component
public class PayUtil {

    @Autowired
    private PayProperties payProperties;

    /**
     * 查询手机支付结果
     * @param outTradeNo
     * @return
     * @throws AlipayApiException
     */
    public boolean confirmTrade( String outTradeNo) throws AlipayApiException {
        AlipayClient alipayClient = new DefaultAlipayClient(
                payProperties.getGatewayUrl(),
                payProperties.getAppId(),
                payProperties.getMerchantPrivateKey(),
                "json",
                payProperties.getCharset(),
                payProperties.getAlipayPublicKey(),
                payProperties.getSignType()
        );
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        model.setOutTradeNo(outTradeNo);
        request.setBizModel(model);
        AlipayTradeQueryResponse response = alipayClient.execute(request);
        if (response.isSuccess()) {
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }


    /**
     * 获取支付宝服务端的返回信息用于验签
     * @param request
     * @return
     */
    public Map<String,String> createParamMap(HttpServletRequest request){
        //获取支付宝POST过来反馈信息
        Map<String,String> params = new HashMap<String,String>();
        Map<String,String[]> requestParams = request.getParameterMap();
        for (Iterator<String> iter = requestParams.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            //乱码解决，这段代码在出现乱码时使用
            try {
                valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            params.put(name, valueStr);
        }
        return params;
    }


    /**
     * 手机扫码支付
     * @param outTradeNo
     * @param subject
     * @param totalAmount
     * @return
     * @throws AlipayApiException
     */
    public String sendRequestWapToAliapy(
            String outTradeNo,
            String subject,
            String totalAmount
    ) throws AlipayApiException {
        //获得初始化的AlipayClient
        AlipayClient alipayClient = new DefaultAlipayClient(
                payProperties.getGatewayUrl(),
                payProperties.getAppId(),
                payProperties.getMerchantPrivateKey(),
                "json",
                payProperties.getCharset(),
                payProperties.getAlipayPublicKey(),
                payProperties.getSignType());
        //设置请求参数
        AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();
        // 成功跳转url
        alipayRequest.setReturnUrl("http:192.168.1.123:8001/wap/success");
        // 异步确认返回url
        alipayRequest.setNotifyUrl(payProperties.getNotifyUrl());

        // 设置请求的参数,使用对象的方式
        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        model.setOutTradeNo(outTradeNo);
        model.setTotalAmount(totalAmount);
        model.setSubject(subject);
        model.setProductCode("QUICK_WAP_PAY");
        alipayRequest.setBizModel(model);

        // 发送请求
        String result = alipayClient.pageExecute(alipayRequest).getBody();
        return result;
    }



    /**
     * 电脑网站支付请求方法
     *
     * @param outTradeNo  商户订单号，商户网站订单系统中唯一订单号，必填
     * @param subject     订单名称，必填
     * @param totalAmount 付款金额，必填
     * @param body        商品描述，可空
     * @return
     */
    public String sendRequestPcToAliapy(
            String outTradeNo,
            String subject,
            String totalAmount,
            String body
    ) throws AlipayApiException {
        //获得初始化的AlipayClient
        AlipayClient alipayClient = new DefaultAlipayClient(
                payProperties.getGatewayUrl(),
                payProperties.getAppId(),
                payProperties.getMerchantPrivateKey(),
                "json",
                payProperties.getCharset(),
                payProperties.getAlipayPublicKey(),
                payProperties.getSignType());
        //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        // 成功跳转url
        alipayRequest.setReturnUrl(payProperties.getReturnUrl());
        // 异步确认url
        alipayRequest.setNotifyUrl(payProperties.getNotifyUrl());

        // 使用对象的方式设置请求参数
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(outTradeNo);
        model.setTotalAmount(totalAmount);
        model.setSubject(subject);
        model.setBody(body);
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        alipayRequest.setBizModel(model);

        // 发送请求
        String result = alipayClient.pageExecute(alipayRequest).getBody();
        return result;
    }

}
