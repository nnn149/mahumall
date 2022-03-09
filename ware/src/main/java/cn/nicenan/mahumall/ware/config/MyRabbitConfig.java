package cn.nicenan.mahumall.ware.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MyRabbitConfig {
    @Value("${myRabbitmq.MQConfig.queues}")
    private String queues;

    @Value("${myRabbitmq.MQConfig.eventExchange}")
    private String eventExchange;

    @Value("${myRabbitmq.MQConfig.routingKey}")
    private String routingKey;

    @Value("${myRabbitmq.MQConfig.delayQueue}")
    private String delayQueue;

    @Value("${myRabbitmq.MQConfig.letterRoutingKey}")
    private String letterRoutingKey;

    @Value("${myRabbitmq.MQConfig.ttl}")
    private Integer ttl;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Bean
    public Exchange stockEventExchange() {
        return new TopicExchange(eventExchange, true, false);
    }

    /**
     * String name, boolean durable, boolean exclusive, boolean autoDelete, @Nullable Map<String, Object> arguments
     */
    @Bean
    public Queue stockReleaseQueue() {
        return new Queue(queues, true, false, false);
    }

    @Bean
    public Queue stockDelayQueue() {

        Map<String, Object> args = new HashMap<>(3);
        // 信死了 交给 [stock-event-exchange] 交换机
        args.put("x-dead-letter-exchange", eventExchange);
        // 死信路由
        args.put("x-dead-letter-routing-key", letterRoutingKey);
        args.put("x-message-ttl", ttl);

        return new Queue(delayQueue, true, false, false, args);
    }

    /**
     * 普通队列的绑定关系
     * String destination, DestinationType destinationType, String exchange, String routingKey, @Nullable Map<String, Object> arguments
     */
    @Bean
    public Binding stockLockedReleaseBinding() {
        return new Binding(queues, Binding.DestinationType.QUEUE, eventExchange, letterRoutingKey + ".#", null);
    }

    /**
     * 延时队列的绑定关系
     * String destination, DestinationType destinationType, String exchange, String routingKey, @Nullable Map<String, Object> arguments
     */
    @Bean
    public Binding stockLockedBinding() {
        return new Binding(delayQueue, Binding.DestinationType.QUEUE, eventExchange, routingKey, null);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 定制RabbitTemplate
     * MyRabbitConfig对象创建完成以后，执行这个方法
     *
     * @return
     */
    @PostConstruct
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
                System.out.println("broker成功收到消息confirm...correlationData:" + correlationData + ";ack:" + ack + ";cause:" + cause);
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
