package com.cisco.clif.rabbit;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created with IntelliJ IDEA.
 * User: jamekava
 * Date: 01/11/2013
 * Time: 23:52
 * To change this template use File | Settings | File Templates.
 */
public class RabbitClusterPublishConsumeWithoutRetry {

    private static final Log logger = LogFactory.getLog(RabbitClusterPublishConsumeWithoutRetry.class);

    public static void main(String...args) {

        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:rabbitContext.xml");
        ctx.registerShutdownHook();

        final AmqpTemplate dispatchTemplate = ctx.getBean("dispatcher", AmqpTemplate.class);
        final AmqpTemplate receiverTemplate = ctx.getBean("receiver", AmqpTemplate.class);
        final Queue testQueue = ctx.getBean("testQueue", Queue.class);

        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                for (; ; ) {
                    String msg = (String) receiverTemplate.receiveAndConvert(testQueue.getName());
                    if (msg != null) {
                        logger.info("Dequeued - " + msg);
                    } else {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            break;
                        }
                    }
                }
            }
        });

        for(int i =0;i <= 1000;i++) {
            dispatchTemplate.convertAndSend(testQueue.getName(), ""+i);
            logger.info("Dispatched message " + i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                break;
            }
        }

    }

}
