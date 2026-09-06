package ch.babyguess.event;

public class RevealNotReadyException extends RuntimeException {

    public RevealNotReadyException() {
        super("The event must be closed before actual details can be saved");
    }
}
