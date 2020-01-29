package uk.gov.justice.digital.hmpps.keyworker.config;

import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.destination.DynamicDestinationResolver;

import javax.jms.Session;

@Configuration
@EnableJms
@ConditionalOnProperty(name = "sqs.provider")
@Slf4j
public class JmsConfig {

    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(final AmazonSQS awsSqsClient) {
        final var factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(new SQSConnectionFactory(new ProviderConfiguration(), awsSqsClient));
        factory.setDestinationResolver(new DynamicDestinationResolver());
        factory.setConcurrency("3-10");
        factory.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
        factory.setErrorHandler(t -> log.error("JMS error occurred", t));
        return factory;
    }

    @Bean
    @ConditionalOnProperty(name = "sqs.provider", havingValue = "aws")
    public AmazonSQS awsSqsClient(@Value("${sqs.aws.access.key.id}") final String accessKey,
                                  @Value("${sqs.aws.secret.access.key}") final String secretKey,
                                  @Value("${sqs.region}") final String region) {
        return AmazonSQSAsyncClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                .withRegion(region)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "sqs.provider", havingValue = "aws")
    public AmazonSQS awsSqsDlqClient(@Value("${sqs.aws.dlq.access.key.id}") final String accessKey,
                                     @Value("${sqs.aws.dlq.secret.access.key}") final String secretKey,
                                     @Value("${sqs.endpoint.region}") String region) {
        return AmazonSQSAsyncClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                .withRegion(region)
                .build();
    }

    @Bean("awsSqsClient")
    @ConditionalOnExpression("'${sqs.provider}'.equals('localstack') and '${sqs.embedded}'.equals('false')")
    public AmazonSQS awsSqsClientLocalStack(@Value("${sqs.endpoint.url}") final String serviceEndpoint,
                                            @Value("${sqs.endpoint.region}") final String region) {
        return AmazonSQSAsyncClientBuilder.standard()
                .withEndpointConfiguration(new EndpointConfiguration(serviceEndpoint, region))
                .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
                .build();
    }

    @Bean("awsSqsDlqClient")
    @ConditionalOnExpression("'${sqs.provider}'.equals('localstack') and '${sqs.embedded}'.equals('false')")
    public AmazonSQS awsSqsDlqClientLocalstack(@Value("${sqs.endpoint.url}") final String serviceEndpoint,
                                               @Value("${sqs.endpoint.region}") final String region) {
        return AmazonSQSClientBuilder.standard()
                .withEndpointConfiguration(new EndpointConfiguration(serviceEndpoint, region))
                .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
                .build();
    }
}
