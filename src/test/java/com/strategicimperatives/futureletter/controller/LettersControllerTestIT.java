package com.strategicimperatives.futureletter.controller;

import com.strategicimperatives.futureletter.fixture.TestContainerFixture;
import com.strategicimperatives.futureletter.domain.Letter;
import com.strategicimperatives.futureletter.repository.LettersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LettersControllerTestIT extends TestContainerFixture {
    @LocalServerPort
    private Integer port;

    @Autowired
    private LettersRepository lettersRepository;

    private static WebTestClient client;

    @BeforeEach
    void beforeEach() {
        client = WebTestClient.bindToServer()
                .baseUrl(String.format("http://localhost:%s", port))
                .build();
    }

    @Test
    void postLetterShouldScheduleEmailFromLetterAndProduceAcceptedResponse() {
        var requestBody = "{\n" +
                "  \"title\": \"Hello, from the past\",\n" +
                "  \"content\": \"This letter was send a while ago\",\n" +
                "  \"recipient_email\": \"henrick.kakutalua@gmail.com\",\n" +
                "  \"send_date\": \"2023-09-23T15:35:00Z\"\n" +
                "}";

        var result = client.post().uri("/v1/letters")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.id").value(not(blankOrNullString()))
                .jsonPath("$.created_at").value(not(blankOrNullString()))
                .jsonPath("$.title").value(equalTo("Hello, from the past"))
                .jsonPath("$.content").value(equalTo("This letter was send a while ago"))
                .jsonPath("$.recipient_email").value(equalTo("henrick.kakutalua@gmail.com"))
                .jsonPath("$.send_date").value(equalTo("2023-09-23T15:35:00"))
                .jsonPath("$.status").value(equalTo("PENDING"))
                .returnResult();

        var actualLocation = result.getResponseHeaders().getLocation();
        assertNotNull(actualLocation);
        assertThat(actualLocation.toString(), startsWith("/v1/letters/"));

        String actualLocationValue = actualLocation.toString();
        var letterId =
                UUID.fromString(actualLocationValue.substring(actualLocationValue.lastIndexOf("/") + 1));

        Optional<Letter> savedLetter = lettersRepository.findById(letterId);
        assertTrue(savedLetter.isPresent());
        assertThat(savedLetter.get().getTitle(), equalTo("Hello, from the past"));
        assertThat(savedLetter.get().getContent(), equalTo("This letter was send a while ago"));
        assertThat(savedLetter.get().getRecipientEmail(), equalTo("henrick.kakutalua@gmail.com"));
        assertThat(savedLetter.get().getSendDate(), equalTo(LocalDateTime.parse("2023-09-23T15:35:00")));
    }

    @Test
    void getLetterShouldRetrieveLetterAndProduceOkResponse() {
        var letter = new Letter.Builder()
                .withTitle("Hello, world!")
                .withBody("...from the future")
                .toEmail("henrick.kakutalua@gmail.com")
                .scheduleTo(LocalDateTime.parse("2023-09-23T15:35:00"))
                .build();
        lettersRepository.save(letter);

        client.get().uri(String.format("/v1/letters/%s", letter.getId()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").value(equalTo(letter.getId().toString()))
                .jsonPath("$.created_at").value(equalTo(DateTimeFormatter.ISO_DATE_TIME.format(letter.getCreatedAt())))
                .jsonPath("$.title").value(equalTo(letter.getTitle()))
                .jsonPath("$.content").value(equalTo(letter.getContent()))
                .jsonPath("$.recipient_email").value(equalTo(letter.getRecipientEmail()))
                .jsonPath("$.send_date").value(equalTo(DateTimeFormatter.ISO_DATE_TIME.format(letter.getSendDate())))
                .jsonPath("$.status").value(equalTo("PENDING"));
    }

    @Test
    void getLetterShouldProduceNotFoundResponseWhenLetterIsNotFound() {
        client.get().uri(String.format("/v1/letters/%s", UUID.randomUUID()))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void patchLetterShouldUpdateLetterFromDatabaseAndProduceNoContentResponse() {
        var letter = new Letter.Builder()
                .withTitle("Hello, world!")
                .withBody("...from the future")
                .toEmail("henrick.kakutalua@gmail.com")
                .scheduleTo(LocalDateTime.parse("2023-09-23T15:35:00"))
                .build();
        lettersRepository.save(letter);

        var requestBody = "{\n" +
                "  \"title\": \"Updated title\",\n" +
                "  \"content\": \"Updated content\",\n" +
                "  \"send_date\": \"2023-09-25T15:35:00Z\"\n" +
                "}";

        client.patch().uri(String.format("/v1/letters/%s", letter.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isNoContent();

        Optional<Letter> updatedLetter = lettersRepository.findById(letter.getId());
        assertTrue(updatedLetter.isPresent());
        assertThat(updatedLetter.get().getTitle(), equalTo("Updated title"));
        assertThat(updatedLetter.get().getContent(), equalTo("Updated content"));
        assertThat(updatedLetter.get().getSendDate(), equalTo(LocalDateTime.parse("2023-09-25T15:35:00")));
    }

    @Test
    void patchLetterShouldNotUpdateOmittedFieldsOfLetterAndProduceNoContentResponse() {
        var letter = new Letter.Builder()
                .withTitle("Hello, world!")
                .withBody("...from the future")
                .toEmail("henrick.kakutalua@gmail.com")
                .scheduleTo(LocalDateTime.parse("2023-09-23T15:35:00"))
                .build();
        lettersRepository.save(letter);

        var requestBody = "{\n" +
                "  \"send_date\": \"2023-09-25T15:35:00Z\"\n" +
                "}";

        client.patch().uri(String.format("/v1/letters/%s", letter.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isNoContent();

        Optional<Letter> updatedLetter = lettersRepository.findById(letter.getId());
        assertTrue(updatedLetter.isPresent());
        assertThat(updatedLetter.get().getTitle(), equalTo(letter.getTitle()));
        assertThat(updatedLetter.get().getContent(), equalTo(letter.getContent()));
        assertThat(updatedLetter.get().getSendDate(), equalTo(LocalDateTime.parse("2023-09-25T15:35:00")));
    }

    @Test
    void deleteLetterShouldDeleteLetterFromDatabaseAndProduceNoContentResponse() {
        var letter = new Letter.Builder()
                .withTitle("Hello, world!")
                .withBody("...from the future")
                .toEmail("henrick.kakutalua@gmail.com")
                .scheduleTo(LocalDateTime.parse("2023-09-23T15:35:00"))
                .build();
        lettersRepository.save(letter);

        client.delete().uri(String.format("/v1/letters/%s", letter.getId()))
                .exchange()
                .expectStatus().isNoContent();

        assertTrue(lettersRepository.findById(letter.getId()).isEmpty());
    }

    @Test
    void deleteLetterShouldProduceNotFoundResponseWhenLetterIsNotFound() {
        client.delete().uri(String.format("/v1/letters/%s", UUID.randomUUID()))
                .exchange()
                .expectStatus().isNotFound();
    }
}