package com.strategicimperatives.futureletter.repository;

import com.strategicimperatives.futureletter.domain.Letter;
import com.strategicimperatives.futureletter.domain.LetterStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public interface LettersRepository extends CrudRepository<Letter, UUID> {
    @Transactional(readOnly = true)
    Stream<Letter> findAllByStatusAndSendDateIsLessThanEqual(LetterStatus status, LocalDateTime sendDate);
}
