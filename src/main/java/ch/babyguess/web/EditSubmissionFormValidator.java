package ch.babyguess.web;

import ch.babyguess.name.NameMatcher;
import java.util.HashSet;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@Component
public class EditSubmissionFormValidator {

    private final NameMatcher nameMatcher;

    public EditSubmissionFormValidator(NameMatcher nameMatcher) {
        this.nameMatcher = nameMatcher;
    }

    public void validate(EditSubmissionForm form, int maximumNameGuesses, Errors errors) {
        var distinct = new HashSet<String>();
        int populated = 0;
        boolean foundTrailingBlank = false;
        if (form.getNameGuesses() != null) {
            for (String guess : form.getNameGuesses()) {
                if (guess == null || guess.isBlank()) {
                    if (populated > 0) {
                        foundTrailingBlank = true;
                    }
                    continue;
                }
                populated++;
                if (foundTrailingBlank) {
                    errors.rejectValue("nameGuesses", "submission.names.gap");
                    break;
                }
                if (!distinct.add(nameMatcher.cosmeticForm(guess))) {
                    errors.rejectValue("nameGuesses", "submission.names.duplicate");
                    break;
                }
            }
        }
        if (populated == 0) {
            errors.rejectValue("nameGuesses", "submission.names.required");
        } else if (populated > maximumNameGuesses) {
            errors.rejectValue("nameGuesses", "submission.names.maximum", new Object[] {maximumNameGuesses}, null);
        }
    }
}
