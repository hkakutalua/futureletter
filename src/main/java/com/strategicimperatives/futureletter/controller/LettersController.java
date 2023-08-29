package com.strategicimperatives.futureletter.controller;

import com.strategicimperatives.futureletter.controller.requestbody.PatchLetterRequest;
import com.strategicimperatives.futureletter.controller.requestbody.PostLetterSendRequest;
import com.strategicimperatives.futureletter.controller.responsebody.GetSingleLetterResponse;
import com.strategicimperatives.futureletter.domain.Letter;
import com.strategicimperatives.futureletter.repository.LettersRepository;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping(path = "/v1/letters")
public class LettersController {
    private LettersRepository lettersRepository;
    private ModelMapper mapper;

    public LettersController(ModelMapper mapper, LettersRepository lettersRepository) {
        assert (lettersRepository != null);
        assert (mapper != null);
        this.lettersRepository = lettersRepository;
        this.mapper = mapper;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Object> scheduleLetterSend(
            @RequestBody @Valid
            PostLetterSendRequest request) {
        var letter = new Letter.Builder()
                .withTitle(request.getTitle())
                .withBody(request.getContent())
                .toEmail(request.getRecipientEmail())
                .scheduleTo(request.getSendDate())
                .build();

        lettersRepository.save(letter);

        return ResponseEntity
                .accepted()
                .location(URI.create(String.format("/v1/letters/%s", letter.getId())))
                .body(mapper.map(letter, GetSingleLetterResponse.class));
    }

    @GetMapping("{id}")
    public ResponseEntity<GetSingleLetterResponse> getSingleLetter(@PathVariable("id") UUID id) {

        var letterOptional = lettersRepository.findById(id);
        if (letterOptional.isEmpty()) {
            return ResponseEntity.notFound()
                    .build();
        }

        Letter letter = letterOptional.get();

        return ResponseEntity.ok(mapper.map(letter, GetSingleLetterResponse.class));
    }

    @PatchMapping("{id}")
    public ResponseEntity<Object> updateSingleLetter(
            @PathVariable("id")
            UUID id,

            @RequestBody @Valid
            PatchLetterRequest request) {

        var letterOptional = lettersRepository.findById(id);
        if (letterOptional.isEmpty()) {
            return ResponseEntity.notFound()
                    .build();
        }

        Letter letter = letterOptional.get();

        if (request.getTitle().isPresent()) {
            letter.setTitle(request.getTitle().get());
        }

        if (request.getContent().isPresent()) {
            letter.setContent(request.getContent().get());
        }

        if (request.getSendDate().isPresent()) {
            letter.setSendDate(request.getSendDate().get());
        }

        lettersRepository.save(letter);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Object> deleteLetter(@PathVariable("id") UUID id) {
        var letter = lettersRepository.findById(id);
        if (letter.isEmpty()) {
            return ResponseEntity.notFound()
                    .build();
        }

        lettersRepository.delete(letter.get());

        return ResponseEntity.noContent().build();
    }
}
