package com.strategicimperatives.futureletter.domain;

import org.springframework.context.ApplicationEvent;

public class SendLetterEvent extends ApplicationEvent {
    private final Letter letter;

    public SendLetterEvent(Letter letter, Object source) {
        super(source);
        this.letter = letter;
    }

    public Letter getLetter() {
        return letter;
    }
}
