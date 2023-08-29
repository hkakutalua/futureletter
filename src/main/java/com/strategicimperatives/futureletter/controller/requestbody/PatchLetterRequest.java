package com.strategicimperatives.futureletter.controller.requestbody;

import com.strategicimperatives.futureletter.controller.validators.NotBlankOptional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Optional;

public class PatchLetterRequest {
    private Optional<@NotBlankOptional @Size(min = 3) String> title = Optional.empty();

    private Optional<@NotBlankOptional @Size(min = 10) String> content = Optional.empty();

    private Optional<LocalDateTime> sendDate = Optional.empty();

    public PatchLetterRequest() { }

    public Optional<String> getTitle() {
        return title;
    }

    public void setTitle(Optional<String> title) {
        this.title = title;
    }

    public Optional<String> getContent() {
        return content;
    }

    public void setContent(Optional<String> content) {
        this.content = content;
    }

    public Optional<LocalDateTime> getSendDate() {
        return sendDate;
    }

    public void setSendDate(Optional<LocalDateTime> sendDate) {
        this.sendDate = sendDate;
    }
}
