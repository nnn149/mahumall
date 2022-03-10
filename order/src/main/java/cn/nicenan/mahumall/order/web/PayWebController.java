package cn.nicenan.mahumall.order.web;

import cn.nicenan.mahumall.order.service.OrderService;
import cn.nicenan.mahumall.order.vo.PayAsyncVo;
import cn.nicenan.mahumall.order.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PayWebController {


    @Autowired
    private OrderService orderService;

    @ResponseBody
    @GetMapping(value = "/aliPayOrder", produces = "text/html")
    public String aliPayOrder(@RequestParam("orderSn") String orderSn) {
        System.out.println("接收到订单信息orderSn：" + orderSn);
        //PayVo payVo = orderService.getOrderPay(orderSn);
        PayAsyncVo payAsyncVo = new PayAsyncVo();
        payAsyncVo.setOut_trade_no(orderSn);
        payAsyncVo.setTrade_status("TRADE_SUCCESS");
        orderService.handlerPayResult(payAsyncVo);
//        String pay = alipayTemplate.pay(payVo);
        String pay = "<script> window.location.href='http://member.mahumall.com/memberOrder.html'</script>";
        return pay;
    }


}
