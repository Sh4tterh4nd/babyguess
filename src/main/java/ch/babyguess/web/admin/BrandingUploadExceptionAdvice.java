package ch.babyguess.web.admin;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class BrandingUploadExceptionAdvice {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    String uploadTooLarge(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("brandingError", "TOO_LARGE");
        return "redirect:/admin/branding";
    }
}
