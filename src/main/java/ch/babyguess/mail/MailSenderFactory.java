package ch.babyguess.mail;

import org.springframework.mail.javamail.JavaMailSender;

interface MailSenderFactory {
    JavaMailSender create(SmtpConnectionSettings settings);
}
