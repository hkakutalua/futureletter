package com.strategicimperatives.futureletter.domain;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "letters")
public class Letter {
    @Id
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "send_date", nullable = false)
    private LocalDateTime sendDate;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private LetterStatus status;

    public Letter(UUID id, LocalDateTime createdAt, String title, String content, String recipientEmail, LocalDateTime sendDate) {
        this.id = id;
        this.createdAt = createdAt;
        this.title = title;
        this.content = content;
        this.recipientEmail = recipientEmail;
        this.sendDate = sendDate;
        this.status = LetterStatus.PENDING;
    }

    /** @noinspection unused
     * default no-args constructor for persistence
     * */
    Letter() { }

    public UUID getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public LocalDateTime getSendDate() {
        return sendDate;
    }

    public LetterStatus getStatus() {
        return status;
    }

    public void setStatus(LetterStatus status) {
        this.status = status;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setSendDate(LocalDateTime sendDate) {
        this.sendDate = sendDate;
    }

    public static class Builder {
        private String title;
        private String content;
        private String email;
        private LocalDateTime sendDate;


        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withBody(String content) {
            this.content = content;
            return this;
        }

        public Builder toEmail(String recipientEmail) {
            this.email = recipientEmail;
            return this;
        }

        public Builder scheduleTo(LocalDateTime sendDate) {
            this.sendDate = sendDate;
            return this;
        }

        public Letter build() {
            return new Letter(
                    UUID.randomUUID(),
                    LocalDateTime.now(ZoneOffset.UTC),
                    title,
                    content,
                    email,
                    sendDate
            );
        }
    }
}
