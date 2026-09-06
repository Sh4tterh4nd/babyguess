package ch.babyguess.branding;

public class BrandingUploadException extends RuntimeException {

    private final BrandingUploadProblem problem;

    public BrandingUploadException(BrandingUploadProblem problem) {
        super("Branding upload rejected: " + problem.name());
        this.problem = problem;
    }

    public BrandingUploadProblem getProblem() {
        return problem;
    }
}
