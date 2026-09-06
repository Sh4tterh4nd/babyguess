package ch.babyguess.event;

public class StaleEventConfigurationException extends RuntimeException {

    public StaleEventConfigurationException() {
        super("The event configuration was changed by another request");
    }
}
