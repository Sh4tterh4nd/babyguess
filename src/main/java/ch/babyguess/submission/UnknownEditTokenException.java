package ch.babyguess.submission;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UnknownEditTokenException extends RuntimeException {

    public UnknownEditTokenException() {
        super("Unknown participant edit token");
    }
}
