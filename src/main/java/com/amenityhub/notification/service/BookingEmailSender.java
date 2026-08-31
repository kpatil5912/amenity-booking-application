package com.amenityhub.notification.service;
import com.amenityhub.booking.entity.Booking;

import com.amenityhub.config.AppProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Sends booking emails asynchronously. Kept intentionally simple: the booking
 * service calls these methods directly and the {@code @Async} annotation moves
 * the work off the request thread. Mail failures are logged and swallowed so a
 * mail outage never surfaces as an API error.
 */
@Component
public class BookingEmailSender {

    private static final Logger log = LoggerFactory.getLogger(BookingEmailSender.class);

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final AppProperties appProperties;

    public BookingEmailSender(
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            AppProperties appProperties) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.appProperties = appProperties;
    }

    @Async
    public void sendBookingConfirmed(Booking booking) {
        sendHtml(booking, "Your AmenityHub booking is confirmed", "booking-confirmed");
    }

    @Async
    public void sendBookingCancelled(Booking booking) {
        sendHtml(booking, "Your AmenityHub booking was cancelled", "booking-cancelled");
    }

    @Async
    public void sendWaitlistPromoted(Booking booking) {
        sendHtml(booking, "A spot opened up — you're booked!", "waitlist-promoted");
    }

    private void sendHtml(Booking booking, String subject, String template) {
        String to = booking.getUser().getEmail();
        try {
            Context context = new Context();
            context.setVariables(model(booking));
            String html = templateEngine.process(template, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(appProperties.from());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Sent '{}' email to {}", subject, to);
        } catch (MessagingException | MailException ex) {
            log.warn("Failed to send '{}' email to {}: {}", subject, to, ex.getMessage());
        }
    }

    private Map<String, Object> model(Booking booking) {
        Map<String, Object> model = new HashMap<>();
        model.put("fullName", booking.getUser().getFullName());
        model.put("bookingReference", booking.getBookingReference());
        model.put("resourceName", booking.getSlot().getResource().getName());
        model.put("startTime", format(booking.getSlot().getStartTime()));
        model.put("endTime", format(booking.getSlot().getEndTime()));
        return model;
    }

    private String format(Instant instant) {
        return instant == null ? "" : FORMATTER.format(instant);
    }
}
