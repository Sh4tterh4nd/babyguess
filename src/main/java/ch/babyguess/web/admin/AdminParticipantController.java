package ch.babyguess.web.admin;

import ch.babyguess.config.PublicUrlProperties;
import ch.babyguess.mail.LinkDeliveryStatus;
import ch.babyguess.mail.ParticipantLinkDeliveryService;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminParticipantController {

    private final AdminParticipantService participantService;
    private final ParticipantLinkDeliveryService deliveryService;
    private final PublicUrlProperties publicUrlProperties;

    public AdminParticipantController(
            AdminParticipantService participantService,
            ParticipantLinkDeliveryService deliveryService,
            PublicUrlProperties publicUrlProperties) {
        this.participantService = participantService;
        this.deliveryService = deliveryService;
        this.publicUrlProperties = publicUrlProperties;
    }

    @GetMapping("/admin/participants")
    String participants(Model model, Locale locale) {
        model.addAttribute("dashboard", participantService.dashboard(locale));
        return "admin/participants";
    }

    @PostMapping("/admin/participants/{participantId}/retry-link")
    String retryLink(
            @PathVariable UUID participantId,
            RedirectAttributes redirectAttributes) {
        try {
            var receipt = participantService.prepareRetry(participantId);
            var status = deliveryService.deliver(receipt, publicUrlProperties.baseUrl());
            var attribute = status == LinkDeliveryStatus.SENT ? "deliverySent" : "deliveryFailed";
            redirectAttributes.addFlashAttribute(attribute, receipt.participant().getDisplayName());
        } catch (NoRetryableLinkDeliveryException exception) {
            redirectAttributes.addFlashAttribute("deliveryUnavailable", true);
        }
        return "redirect:/admin/participants";
    }
}
