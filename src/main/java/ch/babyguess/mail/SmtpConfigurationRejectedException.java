package ch.babyguess.mail;

public class SmtpConfigurationRejectedException extends RuntimeException {

    private final SmtpConfigurationProblem problem;

    public SmtpConfigurationRejectedException(SmtpConfigurationProblem problem) {
        super("SMTP configuration was rejected: " + problem.name());
        this.problem = problem;
    }

    public SmtpConfigurationProblem getProblem() {
        return problem;
    }
}
