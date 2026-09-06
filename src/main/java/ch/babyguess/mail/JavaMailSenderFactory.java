package ch.babyguess.mail;

import java.util.Properties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

@Component
class JavaMailSenderFactory implements MailSenderFactory {

    private static final String TIMEOUT_MILLIS = "10000";

    @Override
    public JavaMailSender create(SmtpConnectionSettings settings) {
        var sender = new JavaMailSenderImpl();
        sender.setProtocol("smtp");
        sender.setHost(settings.host());
        sender.setPort(settings.port());
        if (settings.authenticationRequired()) {
            sender.setUsername(settings.username());
            sender.setPassword(settings.password());
        }

        Properties properties = sender.getJavaMailProperties();
        properties.setProperty("mail.smtp.auth", Boolean.toString(settings.authenticationRequired()));
        properties.setProperty("mail.smtp.connectiontimeout", TIMEOUT_MILLIS);
        properties.setProperty("mail.smtp.timeout", TIMEOUT_MILLIS);
        properties.setProperty("mail.smtp.writetimeout", TIMEOUT_MILLIS);
        if (settings.transportSecurity() == SmtpTransportSecurity.STARTTLS) {
            properties.setProperty("mail.smtp.starttls.enable", "true");
            properties.setProperty("mail.smtp.starttls.required", "true");
        } else if (settings.transportSecurity() == SmtpTransportSecurity.SSL_TLS) {
            properties.setProperty("mail.smtp.ssl.enable", "true");
        }
        return sender;
    }
}
