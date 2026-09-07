package ch.babyguess.web;

import ch.babyguess.event.EventConfiguration;
import ch.babyguess.event.EventConfigurationService;
import ch.babyguess.config.PublicUrlProperties;
import ch.babyguess.mail.ParticipantLinkDeliveryService;
import ch.babyguess.submission.EventClosedException;
import ch.babyguess.submission.ParticipantSubmissionService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    private final EventConfigurationService eventService;
    private final ParticipantSubmissionService submissionService;
    private final ParticipantLinkDeliveryService deliveryService;
    private final SubmissionFormValidator formValidator;
    private final PublicUrlProperties publicUrlProperties;
    private final CaptchaVerifier captchaVerifier;
    private final Clock clock;

    public HomeController(
            EventConfigurationService eventService,
            ParticipantSubmissionService submissionService,
            ParticipantLinkDeliveryService deliveryService,
            SubmissionFormValidator formValidator,
            PublicUrlProperties publicUrlProperties,
            CaptchaVerifier captchaVerifier,
            Clock clock) {
        this.eventService = eventService;
        this.submissionService = submissionService;
        this.deliveryService = deliveryService;
        this.formValidator = formValidator;
        this.publicUrlProperties = publicUrlProperties;
        this.captchaVerifier = captchaVerifier;
        this.clock = clock;
    }

    @GetMapping("/")
    String home(Model model, Locale locale) {
        var event = eventService.get();
        if (event.getRevealedAt() != null) {
            return "redirect:/results";
        }
        if (!model.containsAttribute("submissionForm")) {
            var form = new SubmissionForm();
            form.ensureNameSlots(event.getMaximumNameGuesses());
            model.addAttribute("submissionForm", form);
        }
        addEventModel(model, event, locale);
        return "home";
    }

    @PostMapping("/submit")
    String submit(
            @Valid @ModelAttribute("submissionForm") SubmissionForm form,
            BindingResult bindingResult,
            @RequestParam(value = "cap-token", required = false) String captchaToken,
            Model model,
            Locale locale,
            RedirectAttributes redirectAttributes) {
        var event = eventService.get();
        formValidator.validate(form, event.getMaximumNameGuesses(), bindingResult);
        if (!isOpen(event)) {
            bindingResult.reject("submission.closed");
        }
        if (bindingResult.hasErrors()) {
            form.ensureNameSlots(event.getMaximumNameGuesses());
            addEventModel(model, event, locale);
            return "home";
        }

        if (captchaVerifier.enabled()) {
            try {
                captchaVerifier.verify(captchaToken);
            } catch (CaptchaVerifier.CaptchaVerificationException exception) {
                bindingResult.reject(exception.failure() == CaptchaVerifier.CaptchaFailure.UNAVAILABLE
                        ? "submission.captchaUnavailable"
                        : "submission.captchaRejected");
                form.ensureNameSlots(event.getMaximumNameGuesses());
                addEventModel(model, event, locale);
                return "home";
            }
        }

        boolean existingParticipant;
        try {
            var receipt = submissionService.submit(form.toSubmission(locale));
            existingParticipant = receipt.existingParticipant();
            deliveryService.deliver(receipt, publicUrlProperties.baseUrl());
        } catch (EventClosedException exception) {
            bindingResult.reject("submission.closed");
            form.ensureNameSlots(event.getMaximumNameGuesses());
            addEventModel(model, event, locale);
            return "home";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("submission.invalid");
            form.ensureNameSlots(event.getMaximumNameGuesses());
            addEventModel(model, event, locale);
            return "home";
        }

        redirectAttributes.addFlashAttribute("submitted", true);
        redirectAttributes.addFlashAttribute("existingParticipant", existingParticipant);
        return "redirect:/thanks";
    }

    @GetMapping("/thanks")
    String thanks() {
        return "thanks";
    }

    @GetMapping("/login")
    String login() {
        return "login";
    }

    private void addEventModel(Model model, EventConfiguration event, Locale locale) {
        boolean configured = event.getSubmissionDeadline() != null;
        model.addAttribute("eventTitle", event.getTitle());
        model.addAttribute("eventConfigured", configured);
        model.addAttribute("eventOpen", isOpen(event));
        model.addAttribute("maximumNameGuesses", event.getMaximumNameGuesses());
        model.addAttribute("rankedNames", event.isRankedNameScoring());
        model.addAttribute("sexEnabled", event.isSexEnabled());
        model.addAttribute("birthDateEnabled", event.isBirthDateEnabled());
        model.addAttribute("birthWeightEnabled", event.isBirthWeightEnabled());
        model.addAttribute("captchaEnabled", captchaVerifier.enabled());
        if (captchaVerifier.enabled()) {
            model.addAttribute("captchaEndpoint", captchaVerifier.widgetEndpoint());
        }
        if (configured) {
            var zone = ZoneId.of(event.getEventTimezone());
            var formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale);
            model.addAttribute("deadline", formatter.format(event.getSubmissionDeadline().atZone(zone)));
        }
    }

    private boolean isOpen(EventConfiguration event) {
        return event.getSubmissionDeadline() != null && clock.instant().isBefore(event.getSubmissionDeadline());
    }
}
