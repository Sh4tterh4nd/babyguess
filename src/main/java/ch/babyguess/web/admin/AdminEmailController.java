package ch.babyguess.web.admin;

import ch.babyguess.mail.SmtpConfigurationProblem;
import ch.babyguess.mail.SmtpConfigurationRejectedException;
import ch.babyguess.mail.SmtpConfigurationService;
import ch.babyguess.mail.SmtpTestService;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminEmailController {

    private final SmtpConfigurationService configurationService;
    private final SmtpTestService testService;

    public AdminEmailController(
            SmtpConfigurationService configurationService,
            SmtpTestService testService) {
        this.configurationService = configurationService;
        this.testService = testService;
    }

    @GetMapping("/admin/email")
    String email(Model model) {
        if (!model.containsAttribute("smtpForm")) {
            model.addAttribute("smtpForm", SmtpConfigurationForm.from(configurationService.status()));
        }
        if (!model.containsAttribute("testEmailForm")) {
            model.addAttribute("testEmailForm", new TestEmailForm());
        }
        addReferenceData(model);
        return "admin/email";
    }

    @PostMapping("/admin/email")
    String save(
            @Valid @ModelAttribute("smtpForm") SmtpConfigurationForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            if (!bindingResult.hasErrors()) {
                configurationService.update(
                        form.getVersion(),
                        form.isEnabled(),
                        form.getHost(),
                        form.getPort(),
                        form.getTransportSecurity(),
                        form.isAuthenticationRequired(),
                        form.getSenderAddress(),
                        form.getUsername(),
                        form.getReplacementPassword());
            }
        } catch (SmtpConfigurationRejectedException exception) {
            addConfigurationError(bindingResult, exception.getProblem());
        } catch (OptimisticLockingFailureException exception) {
            bindingResult.reject("admin.email.error.stale");
        } finally {
            form.clearReplacementPassword();
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("testEmailForm", new TestEmailForm());
            addReferenceData(model);
            return "admin/email";
        }
        redirectAttributes.addFlashAttribute("smtpSaved", true);
        return "redirect:/admin/email";
    }

    @PostMapping("/admin/email/test")
    String test(
            @Valid @ModelAttribute("testEmailForm") TestEmailForm form,
            BindingResult bindingResult,
            Model model,
            Locale locale,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("smtpForm", SmtpConfigurationForm.from(configurationService.status()));
            addReferenceData(model);
            return "admin/email";
        }
        redirectAttributes.addFlashAttribute(
                "emailTestResult", testService.send(form.getRecipient(), locale).name());
        return "redirect:/admin/email";
    }

    private void addReferenceData(Model model) {
        model.addAttribute("smtpStatus", configurationService.status());
    }

    private void addConfigurationError(BindingResult bindingResult, SmtpConfigurationProblem problem) {
        switch (problem) {
            case STALE -> bindingResult.reject("admin.email.error.stale");
            case HOST_REQUIRED -> bindingResult.rejectValue("host", "admin.email.validation.hostRequired");
            case SENDER_REQUIRED -> bindingResult.rejectValue(
                    "senderAddress", "admin.email.validation.senderRequired");
            case USERNAME_REQUIRED -> bindingResult.rejectValue(
                    "username", "admin.email.validation.usernameRequired");
            case PASSWORD_REQUIRED -> bindingResult.rejectValue(
                    "replacementPassword", "admin.email.validation.passwordRequired");
            case ENCRYPTION_KEY_REQUIRED -> bindingResult.rejectValue(
                    "replacementPassword", "admin.email.validation.encryptionKeyRequired");
        }
    }
}
