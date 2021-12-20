package com.zjj.ali_pay.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.zjj.ali_pay.Utils.PayProperties;
import com.zjj.ali_pay.Utils.PayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Controller
public class PayController {

    @Autowired
    private PayProperties payProperties;
    @Autowired
    private PayUtil payUtil;


    /**
     * 手机网页支付入口
     *
     * @return
     */
    @GetMapping("wapPay")
    @ResponseBody
    public String wapPay() throws AlipayApiException {
        String trade_no = UUID.randomUUID().toString().replace("-", "");
        // 向支付宝接口发起请求
        String result = payUtil.sendRequestWapToAliapy(trade_no, "vip * 1月", "15");
        // 将支付页面返回给浏览器渲染
        return result;
    }

    /**
     * 电脑网页支付入口
     *
     * @return
     */
    @PostMapping("pagePay")
    @ResponseBody
    public String pagePay() throws AlipayApiException {
        String trade_no = UUID.randomUUID().toString().replace("-", "");
        // 向支付宝接口发起请求
        String result = payUtil.sendRequestPcToAliapy(trade_no, "vip * 1月", "15", "xxx会员");
        // 将支付页面返回给浏览器渲染
        return result;
    }

    /**
     * 订单确认
     */
    @GetMapping("confirm")
    @ResponseBody
    public boolean confirmTrade(String tradeNo) throws AlipayApiException {
        boolean res = payUtil.confirmTrade(tradeNo);
        return res;
    }

    /**
     * 异步返回，这里是可靠的，由支付确认之后的异步返回结果
     * @param request
     * @throws UnsupportedEncodingException
     * @throws AlipayApiException
     */
    @PostMapping("/notify")
    @ResponseBody
    public String notifyUrlMethod(HttpServletRequest request) throws UnsupportedEncodingException, AlipayApiException {
        Map<String, String> params = payUtil.createParamMap(request);
        //调用SDK验证签名
        boolean signVerified = AlipaySignature.rsaCheckV1(
                params,
                payProperties.getAlipayPublicKey(),
                payProperties.getCharset(),
                payProperties.getSignType());

        /* 实际验证过程建议商户务必添加以下校验：
        1、需要验证该通知数据中的out_trade_no是否为商户系统中创建的订单号，
        2、判断total_amount是否确实为该订单的实际金额（即商户订单创建时的金额），
        3、校验通知中的seller_id（或者seller_email) 是否为out_trade_no这笔单据的对应的操作方（有的时候，一个商户可能有多个seller_id/seller_email）
        4、验证app_id是否为该商户本身。
        */
        if(signVerified) {
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            //商户订单号
            String out_trade_no = new String(request.getParameter("out_trade_no").getBytes("ISO-8859-1"),"UTF-8");
            //支付宝交易号
            String trade_no = new String(request.getParameter("trade_no").getBytes("ISO-8859-1"),"UTF-8");
            //交易状态
            String trade_status = new String(request.getParameter("trade_status").getBytes("ISO-8859-1"),"UTF-8");

            //判断该笔订单是否在商户网站中已经做过处理
            //如果没有做过处理，根据订单号（out_trade_no）在商户网站的订单系统中查到该笔订单的详细，并执行商户的业务程序
            //如果有做过处理，不执行商户的业务程序
            //注意：
            //退款日期超过可退款期限后（如三个月可退款），支付宝系统发送该交易状态通知

            // 交易失败
            if(trade_status.equals("TRADE_FINISHED")){
                return "failure";
            }
            // 交易成功
            else if (trade_status.equals("TRADE_SUCCESS")){
                // TODO 做具体业务，如数据库修改，缓存修改等
                boolean trade = payUtil.confirmTrade(out_trade_no);
                System.out.println(trade);
                return "success";
            }
        }else {//验证失败
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return null;
    }


    /**
     *  由支付宝服务返回的同步参数,注意：同步返回不可靠
     * @param request
     * @return
     * @throws UnsupportedEncodingException
     * @throws AlipayApiException
     */
    @GetMapping("/return")
    public String returnUrlMethod(HttpServletRequest request, Model model) throws UnsupportedEncodingException, AlipayApiException {
        Map<String, String> params = payUtil.createParamMap(request);
        //调用SDK验证签名
        boolean signVerified = AlipaySignature.rsaCheckV1(
                params,
                payProperties.getAlipayPublicKey(),
                payProperties.getCharset(),
                payProperties.getSignType());

        if(signVerified) {
            // TODO 在这里做初步验签
            if(signVerified) {
                String out_trade_no = new String(request.getParameter("out_trade_no").getBytes("ISO-8859-1"),"UTF-8");
                model.addAttribute("out_trade_no", out_trade_no);
                return "wait";
            }else{
                return "trade-failure";
            }
        }else {
            // TODO 验签失败
            return "trade-failure";
        }
    }
    //qihwbp9308@sandbox.com

}
