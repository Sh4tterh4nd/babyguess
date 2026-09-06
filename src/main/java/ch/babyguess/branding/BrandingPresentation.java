package ch.babyguess.branding;

public record BrandingPresentation(
        String eventTitle,
        long configurationVersion,
        boolean logoAvailable,
        boolean backgroundAvailable) {
}
