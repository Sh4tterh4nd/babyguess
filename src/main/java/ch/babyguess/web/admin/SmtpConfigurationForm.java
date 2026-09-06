package ch.babyguess.web.admin;

import ch.babyguess.mail.SmtpConfigurationStatus;
import ch.babyguess.mail.SmtpTransportSecurity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SmtpConfigurationForm {

    private long version;
    private boolean enabled;

    @Size(max = 255, message = "{admin.email.validation.hostSize}")
    private String host;

    @NotNull(message = "{admin.email.validation.portRequired}")
    @Min(value = 1, message = "{admin.email.validation.portRange}")
    @Max(value = 65535, message = "{admin.email.validation.portRange}")
    private Integer port;

    @NotNull(message = "{admin.email.validation.securityRequired}")
    private SmtpTransportSecurity transportSecurity;

    private boolean authenticationRequired;

    @Email(message = "{admin.email.validation.senderEmail}")
    @Size(max = 320, message = "{admin.email.validation.addressSize}")
    private String senderAddress;

    @Size(max = 320, message = "{admin.email.validation.addressSize}")
    private String username;

    @Size(max = 1024, message = "{admin.email.validation.passwordSize}")
    private String replacementPassword;

    public static SmtpConfigurationForm from(SmtpConfigurationStatus status) {
        var form = new SmtpConfigurationForm();
        form.version = status.version();
        form.enabled = status.enabled();
        form.host = status.host();
        form.port = status.port();
        form.transportSecurity = status.transportSecurity();
        form.authenticationRequired = status.authenticationRequired();
        form.senderAddress = status.senderAddress();
        form.username = status.username();
        return form;
    }

    public void clearReplacementPassword() {
        replacementPassword = null;
    }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
    public SmtpTransportSecurity getTransportSecurity() { return transportSecurity; }
    public void setTransportSecurity(SmtpTransportSecurity transportSecurity) { this.transportSecurity = transportSecurity; }
    public boolean isAuthenticationRequired() { return authenticationRequired; }
    public void setAuthenticationRequired(boolean authenticationRequired) { this.authenticationRequired = authenticationRequired; }
    public String getSenderAddress() { return senderAddress; }
    public void setSenderAddress(String senderAddress) { this.senderAddress = senderAddress; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getReplacementPassword() { return replacementPassword; }
    public void setReplacementPassword(String replacementPassword) { this.replacementPassword = replacementPassword; }
}
