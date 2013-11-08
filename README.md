# Rabbit MQ Failover

Demo classes that show how to enable automatic failover when connected to a RabbitMQ cluster

### Dependencies

In order for the tests to work you will need to set up a RabbitMQ cluster with at least two nodes. The nodes for the test can be specified in the file
`src/main/resources/rabbit-demo.properties`

There is a property inside this file:

    rabbit-hosts=rabbit1,rabbit2

Change it to either hostnames or IP addresses. Alternatively you can provide this property on the command line e.g.:


    java -cp {classpath} -Drabbit-hosts={comma-delimited-list} com.cisco.clif.rabbit.RabbitClusterPublishConsumeWithoutRetry

These can, for demo purposes be set up on the same host although it's more difficult to simulate network partitions if you do it that way.

The two main classes:

    RabbitClusterPublishConsumeWithoutRetry
    RabbitClusterPublishConsumeWithRetry
  
Are fairly self explanatory but...

The first just uses the spring rabbit client, publishing and consuming in a loop without any special error handling. The latter uses a `Spring-Retry` template to help with retrying upon failure. These templates are quite flexible allowing you to code for your specific use case if required. For more info please see here <http://docs.spring.io/spring-batch/reference/html/retry.html>
