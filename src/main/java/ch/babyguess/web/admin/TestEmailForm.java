package ch.babyguess.web.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TestEmailForm {

    @NotBlank(message = "{admin.email.validation.testRecipientRequired}")
    @Email(message = "{admin.email.validation.testRecipientEmail}")
    @Size(max = 320, message = "{admin.email.validation.addressSize}")
    private String recipient;

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }
}
