package cn.nicenan.mahumall.order.listener;

import cn.nicenan.mahumall.common.to.mq.SecKillOrderTo;
import cn.nicenan.mahumall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@RabbitListener(queues = "order.seckill.order.queue")
@Component
public class OrderSecKillListener {

    @Autowired
    private OrderService orderService;

    @RabbitHandler
    public void listener(SecKillOrderTo secKillOrderTo, Channel channel, Message message) throws IOException {
        try {
            // 创建秒杀单的信息
            orderService.createSecKillOrder(secKillOrderTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

//            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
