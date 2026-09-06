package ch.babyguess.web.admin;

import ch.babyguess.event.EventConfigurationService;
import ch.babyguess.event.StaleEventConfigurationException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminEventController {

    private static final List<String> TIMEZONE_SUGGESTIONS = List.of(
            "Europe/Zurich",
            "Europe/Berlin",
            "Europe/London",
            "America/New_York",
            "America/Los_Angeles",
            "America/Sao_Paulo",
            "Asia/Kolkata",
            "Asia/Singapore",
            "Australia/Sydney",
            "UTC");

    private final EventConfigurationService configurationService;

    public AdminEventController(EventConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @GetMapping("/admin")
    String edit(Model model) {
        if (!model.containsAttribute("eventForm")) {
            model.addAttribute("eventForm", EventConfigurationForm.from(configurationService.get()));
        }
        addReferenceData(model);
        return "admin/index";
    }

    @PostMapping("/admin")
    String save(
            @Valid @ModelAttribute("eventForm") EventConfigurationForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addReferenceData(model);
            return "admin/index";
        }
        try {
            configurationService.update(form.toUpdate());
        } catch (StaleEventConfigurationException exception) {
            bindingResult.reject("admin.error.stale");
            addReferenceData(model);
            return "admin/index";
        }
        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/admin";
    }

    private void addReferenceData(Model model) {
        model.addAttribute("timezoneSuggestions", TIMEZONE_SUGGESTIONS);
    }
}
