package com.cisco.clif.rabbit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.backoff.Sleeper;
import org.springframework.retry.policy.AlwaysRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User: James Kavanagh
 * Date: 08/11/2013
 * Time: 14:15
 *
 * Class that publishes messages to a rabbitmq broker and then subsequently
 * consumes them. This implementation utilises Spring Retry templates that
 * provide a flexible way to deal with error conditions and continue to
 * make progress if possible without having to write complex custom logic
 *
 */
public class RabbitClusterPublishConsumeWithRetry {

    private static final Log logger = LogFactory.getLog(RabbitClusterPublishConsumeWithRetry.class);

    public static void main(String...args) {

        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:rabbitContext.xml");
        ctx.registerShutdownHook();

        final AmqpTemplate dispatchTemplate = ctx.getBean("dispatcher", AmqpTemplate.class);
        final AmqpTemplate receiverTemplate = ctx.getBean("receiver", AmqpTemplate.class);
        final Queue testQueue = ctx.getBean("testQueue", Queue.class);

        final RetryTemplate retry = new RetryTemplate();
        retry.setRetryPolicy(new AlwaysRetryPolicy());
        final FixedBackOffPolicy fbop = new FixedBackOffPolicy();
        fbop.setBackOffPeriod(5000);
        retry.setBackOffPolicy(fbop);

        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                for(;;) {
                    try {

                        retry.execute(new RetryCallback<Object>() {
                            @Override
                            public Object doWithRetry(RetryContext retryContext) throws Exception {
                                String msg = (String) receiverTemplate.receiveAndConvert(testQueue.getName());
                                if(msg != null) {
                                    System.out.println("Dequeued -> " + msg);
                                } else {
                                    try {
                                        Thread.sleep(2000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                                    }
                                }
                                return null;
                            };
                        });
                    } catch (Exception e) {
                        break;
                    }
                }
            }
        });

        for(int i =0;i <= 1000;i++) {
            final int x = i;
            try {
                retry.execute(new RetryCallback<Object>() {
                    @Override
                    public Object doWithRetry(RetryContext retryContext) throws Exception {
                        dispatchTemplate.convertAndSend(testQueue.getName(), ""+x);
                        Thread.sleep(2000);
                        return "123";
                    }
                });
            } catch (InterruptedException ie) {
                break;
            } catch (Exception e) {
                logger.error("Oops!", e);
            }
        }
    }

}
