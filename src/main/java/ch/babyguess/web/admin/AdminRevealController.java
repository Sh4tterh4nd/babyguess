package ch.babyguess.web.admin;

import ch.babyguess.event.EventConfigurationService;
import ch.babyguess.event.RevealNotReadyException;
import ch.babyguess.event.StaleEventConfigurationException;
import ch.babyguess.name.NameMatchDecisionType;
import ch.babyguess.mail.LinkDeliveryStatus;
import ch.babyguess.mail.NoRetryableRevealDeliveryException;
import ch.babyguess.mail.RevealEmailDeliveryService;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminRevealController {

    private final EventConfigurationService eventService;
    private final AdminRevealService revealService;
    private final RevealPublicationService publicationService;
    private final RevealEmailDeliveryService revealEmailDeliveryService;

    public AdminRevealController(
            EventConfigurationService eventService,
            AdminRevealService revealService,
            RevealPublicationService publicationService,
            RevealEmailDeliveryService revealEmailDeliveryService) {
        this.eventService = eventService;
        this.revealService = revealService;
        this.publicationService = publicationService;
        this.revealEmailDeliveryService = revealEmailDeliveryService;
    }

    @GetMapping("/admin/reveal")
    String reveal(Model model, Locale locale) {
        addPageModel(model, locale);
        return "admin/reveal";
    }

    @PostMapping("/admin/reveal/details")
    String saveDetails(
            @Valid @ModelAttribute("actualDetailsForm") ActualDetailsForm form,
            BindingResult bindingResult,
            Model model,
            Locale locale,
            RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                eventService.updateActualDetails(form.getVersion(), form.toActualBaby());
            } catch (RevealNotReadyException exception) {
                bindingResult.reject("admin.reveal.error.open");
            } catch (StaleEventConfigurationException exception) {
                bindingResult.reject("admin.error.stale");
            }
        }
        if (bindingResult.hasErrors()) {
            if (!model.containsAttribute("variantForm")) {
                model.addAttribute("variantForm", new NameVariantForm());
            }
            addPageModel(model, locale);
            return "admin/reveal";
        }
        redirectAttributes.addFlashAttribute("detailsSaved", true);
        return "redirect:/admin/reveal";
    }

    @PostMapping("/admin/reveal/candidates")
    String decideCandidate(
            @RequestParam String variant,
            @RequestParam NameMatchDecisionType decision,
            RedirectAttributes redirectAttributes) {
        try {
            revealService.reviewCandidate(variant, decision);
            redirectAttributes.addFlashAttribute("decisionSaved", true);
        } catch (IllegalArgumentException | RevealNotReadyException exception) {
            redirectAttributes.addFlashAttribute("decisionUnavailable", true);
        }
        return "redirect:/admin/reveal#name-review";
    }

    @PostMapping("/admin/reveal/variants")
    String addVariant(
            @Valid @ModelAttribute("variantForm") NameVariantForm form,
            BindingResult bindingResult,
            Model model,
            Locale locale,
            RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                revealService.acceptManualVariant(form.getVariant());
            } catch (IllegalArgumentException | RevealNotReadyException exception) {
                bindingResult.reject("admin.reveal.variant.unavailable");
            }
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("actualDetailsForm", ActualDetailsForm.from(eventService.get()));
            addPageModel(model, locale);
            return "admin/reveal";
        }
        redirectAttributes.addFlashAttribute("decisionSaved", true);
        return "redirect:/admin/reveal#name-review";
    }

    @PostMapping("/admin/reveal/publish")
    String publish(
            @RequestParam long version,
            @RequestParam(defaultValue = "false") boolean confirmed,
            RedirectAttributes redirectAttributes) {
        if (!confirmed) {
            redirectAttributes.addFlashAttribute("publicationConfirmationRequired", true);
            return "redirect:/admin/reveal#publish-reveal";
        }
        try {
            var result = publicationService.publish(version);
            if (!result.firstPublication()) {
                redirectAttributes.addFlashAttribute("alreadyPublished", true);
                return "redirect:/admin/reveal#publish-reveal";
            }
            int sent = 0;
            int failed = 0;
            for (var deliveryId : result.deliveryIds()) {
                if (revealEmailDeliveryService.deliver(deliveryId) == LinkDeliveryStatus.SENT) {
                    sent++;
                } else {
                    failed++;
                }
            }
            redirectAttributes.addFlashAttribute("published", true);
            redirectAttributes.addFlashAttribute("revealEmailsSent", sent);
            redirectAttributes.addFlashAttribute("revealEmailsFailed", failed);
        } catch (RevealNotReadyException exception) {
            redirectAttributes.addFlashAttribute("publicationNotReady", true);
        } catch (StaleEventConfigurationException exception) {
            redirectAttributes.addFlashAttribute("publicationStale", true);
        }
        return "redirect:/admin/reveal#publish-reveal";
    }

    @PostMapping("/admin/reveal/deliveries/{deliveryId}/retry")
    String retryRevealEmail(
            @org.springframework.web.bind.annotation.PathVariable UUID deliveryId,
            RedirectAttributes redirectAttributes) {
        try {
            var status = revealEmailDeliveryService.retry(deliveryId);
            redirectAttributes.addFlashAttribute("revealEmailRetried", status == LinkDeliveryStatus.SENT);
            redirectAttributes.addFlashAttribute("revealEmailRetryFailed", status == LinkDeliveryStatus.FAILED);
        } catch (NoRetryableRevealDeliveryException exception) {
            redirectAttributes.addFlashAttribute("revealEmailRetryUnavailable", true);
        }
        return "redirect:/admin/reveal#publish-reveal";
    }

    @PostMapping("/admin/reveal/decisions")
    String changeDecision(
            @RequestParam String variant,
            @RequestParam NameMatchDecisionType decision,
            RedirectAttributes redirectAttributes) {
        try {
            revealService.changeDecision(variant, decision);
            redirectAttributes.addFlashAttribute("decisionSaved", true);
        } catch (IllegalArgumentException | RevealNotReadyException exception) {
            redirectAttributes.addFlashAttribute("decisionUnavailable", true);
        }
        return "redirect:/admin/reveal#name-review";
    }

    private void addPageModel(Model model, Locale locale) {
        if (!model.containsAttribute("actualDetailsForm")) {
            model.addAttribute("actualDetailsForm", ActualDetailsForm.from(eventService.get()));
        }
        if (!model.containsAttribute("variantForm")) {
            model.addAttribute("variantForm", new NameVariantForm());
        }
        model.addAttribute("reveal", revealService.view(locale));
        model.addAttribute("publication", publicationService.status(locale));
    }
}
