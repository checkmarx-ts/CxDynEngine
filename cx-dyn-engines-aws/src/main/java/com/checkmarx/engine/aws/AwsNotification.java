package com.checkmarx.engine.aws;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.AmazonSNSException;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.checkmarx.engine.CxConfig;
import com.checkmarx.engine.rest.Notification;
import com.google.common.base.Strings;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@Profile("aws")
public class AwsNotification implements Notification {

    private final AmazonSNS amazonSNS;
    //Active map containing a hash of a particular message event that was sent and the time associated with it
    private Map<String, LocalDateTime> activeNotification = new HashMap<>();
    private final CxConfig cxConfig;
    private static final Logger log = LoggerFactory.getLogger(AwsNotification.class);

    public AwsNotification(CxConfig cxConfig) {
        this.cxConfig = cxConfig;
        this.amazonSNS = AmazonSNSClientBuilder.defaultClient();
    }

    @Override
    public void sendNotification(String subject, String message, Throwable throwable) {
        String hash = DigestUtils.sha256Hex(message);
        LocalDateTime now = LocalDateTime.now();
        if(activeNotification.containsKey(hash)){
            LocalDateTime sent = activeNotification.get(hash);
            if(now.isAfter(sent.plusMinutes(cxConfig.getNotificationTimer()))){
                publish(subject, message);
                activeNotification.put(hash, now);
            }
            else{
                log.debug("Message not sent: {}", message);
                log.debug("Last message sent {}", sent);
            }
        }
        else{
            publish(subject, message);
            activeNotification.put(hash, now);
        }

    }

    private void publish(String subject, String message) {
        String snsTopic = cxConfig.getNotificationId();
        if (Strings.isNullOrEmpty(snsTopic)) {
            log.info("No SNS topic has been specified.  No notification sent");
        } else {
            try {
                PublishRequest publishRequest = new PublishRequest(snsTopic, message, subject);
                log.debug("Publishing event to topic {} - {}", snsTopic, message);
                PublishResult publishResult = this.amazonSNS.publish(publishRequest);
                log.debug("SNS topic {} , response id {}", snsTopic, publishResult.getMessageId());
            } catch (AmazonSNSException e) {
                log.error("Error occurred sending message to ARN {}.  Details: {}", snsTopic, ExceptionUtils.getMessage(e));
            }
        }
    }

}