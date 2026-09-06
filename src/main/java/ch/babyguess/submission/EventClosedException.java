package ch.babyguess.submission;

public class EventClosedException extends RuntimeException {

    public EventClosedException() {
        super("Submissions are closed");
    }
}
