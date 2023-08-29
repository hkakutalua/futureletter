package com.strategicimperatives.futureletter.controller.responsebody;

import com.strategicimperatives.futureletter.domain.LetterStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class GetSingleLetterResponse {
    private UUID id;
    private LocalDateTime createdAt;
    private String title;
    private String content;
    private String recipientEmail;
    private LocalDateTime sendDate;
    private LetterStatus status;

    /** @noinspection unused
     * no-arg constructor for mapping
     * */
    public GetSingleLetterResponse() { }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public LocalDateTime getSendDate() {
        return sendDate;
    }

    public void setSendDate(LocalDateTime sendDate) {
        this.sendDate = sendDate;
    }

    public LetterStatus getStatus() {
        return status;
    }

    public void setStatus(LetterStatus status) {
        this.status = status;
    }
}
