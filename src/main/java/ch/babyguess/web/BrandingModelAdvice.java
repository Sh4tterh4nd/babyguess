package ch.babyguess.web;

import ch.babyguess.branding.BrandingPresentation;
import ch.babyguess.branding.BrandingService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class BrandingModelAdvice {

    private final BrandingService brandingService;

    public BrandingModelAdvice(BrandingService brandingService) {
        this.brandingService = brandingService;
    }

    @ModelAttribute("branding")
    BrandingPresentation branding() {
        return brandingService.presentation();
    }
}
