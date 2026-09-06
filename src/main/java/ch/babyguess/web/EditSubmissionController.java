package ch.babyguess.web;

import ch.babyguess.event.EventConfiguration;
import ch.babyguess.event.EventConfigurationService;
import ch.babyguess.submission.EventClosedException;
import ch.babyguess.submission.ParticipantSubmissionService;
import jakarta.validation.Valid;
import java.time.Clock;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class EditSubmissionController {

    private final ParticipantSubmissionService submissionService;
    private final EventConfigurationService eventService;
    private final EditSubmissionFormValidator formValidator;
    private final Clock clock;

    public EditSubmissionController(
            ParticipantSubmissionService submissionService,
            EventConfigurationService eventService,
            EditSubmissionFormValidator formValidator,
            Clock clock) {
        this.submissionService = submissionService;
        this.eventService = eventService;
        this.formValidator = formValidator;
        this.clock = clock;
    }

    @GetMapping("/edit/{token}")
    String edit(@PathVariable String token, Model model) {
        var event = eventService.get();
        var view = submissionService.getForEdit(token);
        model.addAttribute("editForm", EditSubmissionForm.from(view, event.getMaximumNameGuesses()));
        addPageModel(model, event, view.displayName(), view.emailAddress());
        return "edit";
    }

    @PostMapping("/edit/{token}")
    String save(
            @PathVariable String token,
            @Valid @ModelAttribute("editForm") EditSubmissionForm form,
            BindingResult bindingResult,
            Model model) {
        var event = eventService.get();
        var view = submissionService.getForEdit(token);
        formValidator.validate(form, event.getMaximumNameGuesses(), bindingResult);
        if (!isOpen(event)) {
            bindingResult.reject("submission.closed");
        }
        if (bindingResult.hasErrors()) {
            form.ensureNameSlots(event.getMaximumNameGuesses());
            addPageModel(model, event, view.displayName(), view.emailAddress());
            return "edit";
        }
        try {
            submissionService.edit(token, form.toPredictionValues());
        } catch (EventClosedException exception) {
            bindingResult.reject("submission.closed");
            form.ensureNameSlots(event.getMaximumNameGuesses());
            addPageModel(model, event, view.displayName(), view.emailAddress());
            return "edit";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("submission.invalid");
            form.ensureNameSlots(event.getMaximumNameGuesses());
            addPageModel(model, event, view.displayName(), view.emailAddress());
            return "edit";
        }
        return "redirect:/edit/" + token + "?saved";
    }

    private void addPageModel(Model model, EventConfiguration event, String displayName, String emailAddress) {
        model.addAttribute("eventTitle", event.getTitle());
        model.addAttribute("eventOpen", isOpen(event));
        model.addAttribute("maximumNameGuesses", event.getMaximumNameGuesses());
        model.addAttribute("sexEnabled", event.isSexEnabled());
        model.addAttribute("birthDateEnabled", event.isBirthDateEnabled());
        model.addAttribute("birthWeightEnabled", event.isBirthWeightEnabled());
        model.addAttribute("displayName", displayName);
        model.addAttribute("emailAddress", emailAddress);
    }

    private boolean isOpen(EventConfiguration event) {
        return event.getSubmissionDeadline() != null && clock.instant().isBefore(event.getSubmissionDeadline());
    }
}
