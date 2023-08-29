package com.strategicimperatives.futureletter.background;

import com.strategicimperatives.futureletter.config.EnvironmentVariables;
import com.strategicimperatives.futureletter.domain.Letter;
import com.strategicimperatives.futureletter.domain.LetterStatus;
import com.strategicimperatives.futureletter.domain.SendLetterEvent;
import com.strategicimperatives.futureletter.repository.LettersRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.Stream;

/** @noinspection unused*/
@Configuration
@EnableAsync
@EnableScheduling
public class EmailSendBackgroundJob implements ApplicationListener<SendLetterEvent> {
    private static final Logger logger = LoggerFactory.getLogger(EmailSendBackgroundJob.class);

    private final JavaMailSender mailSender;
    private final LettersRepository lettersRepository;
    private final TransactionTemplate transactionTemplate;
    private final TaskExecutor taskExecutor;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    @Value(EnvironmentVariables.SMTP_USERNAME)
    private String fromEmailAddress;

    public EmailSendBackgroundJob(
            JavaMailSender mailSender,
            LettersRepository lettersRepository,
            TransactionTemplate transactionTemplate,
            TaskExecutor taskExecutor,
            ApplicationEventPublisher eventPublisher,
            EntityManager entityManager) {
        this.mailSender = mailSender;
        this.lettersRepository = lettersRepository;
        this.transactionTemplate = transactionTemplate;
        this.taskExecutor = taskExecutor;
        this.eventPublisher = eventPublisher;
        this.entityManager = entityManager;
    }

    @Scheduled(cron = EnvironmentVariables.EMAIL_SEND_CRON)
    @Transactional(readOnly = true)
    public void sendPendingEmails() {
        logger.info("Background task to send pending emails started.");

        LocalDateTime currentDateTimeUtc = LocalDateTime.now(ZoneOffset.UTC);

        Stream<Letter> pendingLettersStream =
                lettersRepository.findAllByStatusAndSendDateIsLessThanEqual(LetterStatus.PENDING, currentDateTimeUtc);

        try(pendingLettersStream) {
            pendingLettersStream.forEach((pendingLetter) ->
                    eventPublisher.publishEvent(new SendLetterEvent(pendingLetter, this)));
        }

        logger.info("Background task to send pending emails finished.");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onApplicationEvent(SendLetterEvent event) {
        logger.info(String.format("Sending email to '%s'.", event.getLetter().getRecipientEmail()));
        Letter pendingLetter = event.getLetter();
        pendingLetter.setStatus(LetterStatus.SENDING);
        lettersRepository.save(pendingLetter);

        SimpleMailMessage mailMessage = createMailMessageForLetter(pendingLetter);
        mailSender.send(mailMessage);
    }

    @Bean
    public ApplicationEventMulticaster applicationEventMulticaster() {
        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
        eventMulticaster.setTaskExecutor(taskExecutor);
        return eventMulticaster;
    }

    private SimpleMailMessage createMailMessageForLetter(Letter pendingLetter) {
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setFrom(fromEmailAddress);
        simpleMailMessage.setTo(pendingLetter.getRecipientEmail());
        simpleMailMessage.setSubject(pendingLetter.getTitle());
        simpleMailMessage.setText(pendingLetter.getContent());

        return simpleMailMessage;
    }
}
