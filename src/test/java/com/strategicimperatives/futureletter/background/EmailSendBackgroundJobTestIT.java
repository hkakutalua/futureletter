package com.strategicimperatives.futureletter.background;

import com.strategicimperatives.futureletter.fixture.TestContainerFixture;
import com.strategicimperatives.futureletter.domain.Letter;
import com.strategicimperatives.futureletter.domain.LetterStatus;
import com.strategicimperatives.futureletter.repository.LettersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/** @noinspection OptionalGetWithoutIsPresent*/
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { EmailSendBackgroundJobTestIT.EmailBackgroundJobTestConfig.class },
        properties = "spring.main.allow-bean-definition-overriding=true"
)
class EmailSendBackgroundJobTestIT extends TestContainerFixture {
    @Autowired
    private LettersRepository lettersRepository;

    @MockBean
    private JavaMailSender mailSenderMock;

    @Autowired
    private EmailSendBackgroundJob emailSendBackgroundJob;

    @BeforeEach
    void beforeEach() {
        lettersRepository.deleteAll();
    }

    @Test
    void shouldSendEmailForPendingLetters() {
        var mockedStatic = Mockito.mockStatic(
                LocalDateTime.class,
                Mockito.withSettings().defaultAnswer(InvocationOnMock::callRealMethod));
        try (mockedStatic) {
            LocalDateTime currentDate = LocalDateTime.parse("2023-09-23T16:00:01");
            mockedStatic.when(() -> LocalDateTime.now(Mockito.any(ZoneId.class))).thenReturn(currentDate);
            mockedStatic.when(() -> LocalDateTime.now(Mockito.any(Clock.class))).thenReturn(currentDate);
            mockedStatic.when(LocalDateTime::now).thenReturn(currentDate);

            Letter letterToHenrick = new Letter.Builder()
                    .withTitle("Hello, world!")
                    .withBody("...from the future")
                    .toEmail("henrick.kakutalua@gmail.com")
                    .scheduleTo(LocalDateTime.parse("2023-09-23T15:35:00"))
                    .build();
            Letter letterToJohn = new Letter.Builder()
                    .withTitle("Hello again!")
                    .withBody("...from the future")
                    .toEmail("john.doe@gmail.com")
                    .scheduleTo(LocalDateTime.parse("2023-09-23T16:00:00"))
                    .build();
            lettersRepository.saveAll(Arrays.asList(letterToHenrick, letterToJohn));

            emailSendBackgroundJob.sendPendingEmails();

            Letter savedLetter1 = lettersRepository.findById(letterToHenrick.getId()).get();
            Letter savedLetter2 = lettersRepository.findById(letterToHenrick.getId()).get();
            assertThat(savedLetter1.getStatus(), equalTo(LetterStatus.SENDING));
            assertThat(savedLetter2.getStatus(), equalTo(LetterStatus.SENDING));

            var mailMessageArgCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            Mockito.verify(mailSenderMock, Mockito.times(2)).send(mailMessageArgCaptor.capture());

            var sentMailMessages = mailMessageArgCaptor.getAllValues();
            assertThat(sentMailMessages.size(), equalTo(2));

            var mailMessageToHenrick = sentMailMessages.stream()
                    .filter(simpleMailMessage -> simpleMailMessage.getTo()[0].equals("henrick.kakutalua@gmail.com"))
                    .findFirst().get();
            assertThat(mailMessageToHenrick.getFrom(), equalTo("henrick.kakutalua@gmail.com"));
            assertThat(mailMessageToHenrick.getSubject(), equalTo(letterToHenrick.getTitle()));
            assertThat(mailMessageToHenrick.getText(), equalTo(letterToHenrick.getContent()));

            var mailMessageToJohn = sentMailMessages.stream()
                    .filter(simpleMailMessage -> simpleMailMessage.getTo()[0].equals("john.doe@gmail.com"))
                    .findFirst().get();
            assertThat(mailMessageToJohn.getFrom(), equalTo("henrick.kakutalua@gmail.com"));
            assertThat(mailMessageToJohn.getSubject(), equalTo(letterToJohn.getTitle()));
            assertThat(mailMessageToJohn.getText(), equalTo(letterToJohn.getContent()));
        }
    }

    @Test
    void shouldRollbackLetterStatusChangeForLettersWithFailedEmailDelivery() {
        var mockedStatic = Mockito.mockStatic(
                LocalDateTime.class,
                Mockito.withSettings().defaultAnswer(InvocationOnMock::callRealMethod));
        try (mockedStatic) {
            LocalDateTime currentDate = LocalDateTime.parse("2023-09-23T16:00:01");
            mockedStatic.when(() -> LocalDateTime.now(Mockito.any(ZoneId.class))).thenReturn(currentDate);
            mockedStatic.when(() -> LocalDateTime.now(Mockito.any(Clock.class))).thenReturn(currentDate);
            mockedStatic.when(LocalDateTime::now).thenReturn(currentDate);

            Letter letterThatWillBeSent = new Letter.Builder()
                    .withTitle("Hello, world!")
                    .withBody("...from the future")
                    .toEmail("henrick.kakutalua@gmail.com")
                    .scheduleTo(LocalDateTime.parse("2023-09-23T15:35:00"))
                    .build();
            Letter letterThatWillFail = new Letter.Builder()
                    .withTitle("Hello again!")
                    .withBody("...from the future")
                    .toEmail("john.doe@gmail.com")
                    .scheduleTo(LocalDateTime.parse("2023-09-23T16:00:00"))
                    .build();
            lettersRepository.saveAll(Arrays.asList(letterThatWillBeSent, letterThatWillFail));

            Mockito.doNothing()
                    .doThrow(MailSendException.class)
                    .when(mailSenderMock).send(Mockito.any(SimpleMailMessage.class));

            try {
                emailSendBackgroundJob.sendPendingEmails();
            } catch (MailSendException ignored) {
            } finally {
                Letter sentLetter = lettersRepository.findById(letterThatWillBeSent.getId()).get();
                Letter unmodifiedLetter = lettersRepository.findById(letterThatWillFail.getId()).get();
                assertThat(sentLetter.getStatus(), equalTo(LetterStatus.SENDING));
                assertThat(unmodifiedLetter.getStatus(), equalTo(LetterStatus.PENDING));

                var mailMessageArgCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
                Mockito.verify(mailSenderMock, Mockito.times(2)).send(mailMessageArgCaptor.capture());

                var sentMailMessage = mailMessageArgCaptor.getAllValues().stream()
                        .filter(x -> x.getTo()[0].equals("henrick.kakutalua@gmail.com"))
                        .findFirst().get();
                assertThat(sentMailMessage.getFrom(), equalTo("henrick.kakutalua@gmail.com"));
                assertThat(sentMailMessage.getSubject(), equalTo(letterThatWillBeSent.getTitle()));
                assertThat(sentMailMessage.getText(), equalTo(letterThatWillBeSent.getContent()));
            }
        }
    }

    @Test
    void shouldNotSendEmailForPendingLettersThatShouldBeSentAfterCurrentDate() {
        var mockedStatic = Mockito.mockStatic(
                LocalDateTime.class,
                Mockito.withSettings().defaultAnswer(InvocationOnMock::callRealMethod));
        try (mockedStatic) {
            LocalDateTime currentDate = LocalDateTime.parse("2023-09-23T16:00:01");
            mockedStatic.when(() -> LocalDateTime.now(Mockito.any(ZoneId.class))).thenReturn(currentDate);
            mockedStatic.when(() -> LocalDateTime.now(Mockito.any(Clock.class))).thenReturn(currentDate);
            mockedStatic.when(LocalDateTime::now).thenReturn(currentDate);

            Letter letterInTheFuture1 = new Letter.Builder()
                    .withTitle("Hello, world!")
                    .withBody("...from the future")
                    .toEmail("henrick.kakutalua@gmail.com")
                    .scheduleTo(LocalDateTime.parse("2023-09-24T00:00:00"))
                    .build();
            Letter letterInTheFuture2 = new Letter.Builder()
                    .withTitle("Hello again!")
                    .withBody("...from the future")
                    .toEmail("john.doe@gmail.com")
                    .scheduleTo(LocalDateTime.parse("2023-09-24T00:00:00"))
                    .build();
            lettersRepository.saveAll(Arrays.asList(letterInTheFuture1, letterInTheFuture2));

            emailSendBackgroundJob.sendPendingEmails();

            Letter unmodifiedLetter1 = lettersRepository.findById(letterInTheFuture1.getId()).get();
            Letter unmodifiedLetter2 = lettersRepository.findById(letterInTheFuture1.getId()).get();
            assertThat(unmodifiedLetter1.getStatus(), equalTo(LetterStatus.PENDING));
            assertThat(unmodifiedLetter2.getStatus(), equalTo(LetterStatus.PENDING));

            Mockito.verify(mailSenderMock, Mockito.never()).send(Mockito.any(SimpleMailMessage.class));
        }
    }

    @Test
    void shouldNotSendEmailForLettersThatAreNotInPendingStatus() {
        var mockedStatic = Mockito.mockStatic(
                LocalDateTime.class,
                Mockito.withSettings().defaultAnswer(InvocationOnMock::callRealMethod));
        try (mockedStatic) {
            LocalDateTime currentDate = LocalDateTime.parse("2023-09-23T16:00:01");
            mockedStatic.when(() -> LocalDateTime.now(Mockito.any(ZoneId.class))).thenReturn(currentDate);
            mockedStatic.when(() -> LocalDateTime.now(Mockito.any(Clock.class))).thenReturn(currentDate);
            mockedStatic.when(LocalDateTime::now).thenReturn(currentDate);

            Letter letterInSentStatus = new Letter.Builder()
                    .withTitle("Hello, world!")
                    .withBody("...from the future (don't send)")
                    .toEmail("henrick.kakutalua@gmail.com")
                    .scheduleTo(LocalDateTime.parse("2023-09-23T15:35:00"))
                    .build();
            letterInSentStatus.setStatus(LetterStatus.SENT);
            Letter letterInSendingStatus = new Letter.Builder()
                    .withTitle("Hello again!")
                    .withBody("...from the future (don't send)")
                    .toEmail("john.doe@gmail.com")
                    .scheduleTo(LocalDateTime.parse("2023-09-23T16:00:00"))
                    .build();
            letterInSendingStatus.setStatus(LetterStatus.SENDING);
            lettersRepository.saveAll(Arrays.asList(letterInSentStatus, letterInSendingStatus));

            emailSendBackgroundJob.sendPendingEmails();

            Letter unmodifiedLetter1 = lettersRepository.findById(letterInSentStatus.getId()).get();
            Letter unmodifiedLetter2 = lettersRepository.findById(letterInSendingStatus.getId()).get();
            assertThat(unmodifiedLetter1.getStatus(), equalTo(LetterStatus.SENT));
            assertThat(unmodifiedLetter2.getStatus(), equalTo(LetterStatus.SENDING));

            Mockito.verify(mailSenderMock, Mockito.times(0)).send(Mockito.any(SimpleMailMessage.class));
        }
    }

    @TestConfiguration
    public static class EmailBackgroundJobTestConfig {
        @Bean
        @Primary
        public TaskExecutor taskExecutor() {
            return new SyncTaskExecutor();
        }
    }
}