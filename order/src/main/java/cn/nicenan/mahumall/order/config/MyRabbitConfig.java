package cn.nicenan.mahumall.order.config;

import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class MyRabbitConfig {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 定制RabbitTemplate
     *
     * @return
     */
    @PostConstruct //MyRabbitConfig对象创建完成以后，执行这个方法
    public void initRabbitTemplate() {
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            /**
             * Broker收到消息后的回调
             * @param correlationData 当前消息的唯一关联数据
             * @param ack 消息是否成功收到
             * @param cause  失败的原因
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                System.out.println("confirm...correlationData:" + correlationData + ";ack:" + ack + ";cause:" + cause);
            }
        });

        //只要消息没有投递给指定的队列，就触发
        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {

            @Override
            public void returnedMessage(ReturnedMessage returnedMessage) {
                System.out.println("投递失败消息的详细信息:" + returnedMessage.getMessage() + "回复的状态码" +
                        returnedMessage.getReplyCode() + "回复的消息" + returnedMessage.getReplyText() +
                        "发给哪个交换机:" + returnedMessage.getExchange() + "发给哪个队列(路由键):" + returnedMessage.getRoutingKey());
            }
        });
    }
}
