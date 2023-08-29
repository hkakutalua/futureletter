CREATE TABLE letters (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    recipient_email TEXT NOT NULL,
    send_date TIMESTAMP NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'SENDING', 'SENT'))
);