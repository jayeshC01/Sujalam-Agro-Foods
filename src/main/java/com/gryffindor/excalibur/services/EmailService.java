package com.gryffindor.excalibur.services;

import com.gryffindor.excalibur.model.constants.OrderStatus;
import com.gryffindor.excalibur.model.db.Order;
import com.gryffindor.excalibur.model.event.OrderPlacedEvent;
import com.gryffindor.excalibur.model.event.OrderStatusUpdatedEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {
  private static final Logger log = LoggerFactory.getLogger(EmailService.class);

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;
  private final String fromAddress;
  private final String adminEmail;

  @Autowired
  EmailService(
      JavaMailSender mailSender,
      TemplateEngine templateEngine,
      @Value("${app.mail.from}") String fromAddress,
      @Value("${app.mail.admin:${app.mail.from}}") String adminEmail) {
    this.mailSender = mailSender;
    this.templateEngine = templateEngine;
    this.fromAddress = fromAddress;
    this.adminEmail = adminEmail;
  }

  /**
   * Listens for OrderPlacedEvent after the DB transaction has committed, and sends both customer
   * confirmation and admin alerts asynchronously.
   */
  @Async("mailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderPlacedEvent(OrderPlacedEvent event) {
    log.info("Handling OrderPlacedEvent after commit for order {}", event.order().getOrderId());
    try {
      sendOrderConfirmationEmail(event.order());
    } catch (Exception e) {
      log.error(
          "Order confirmation dispatch failed after retries for order {}",
          event.order().getOrderId(),
          e);
    }
    try {
      sendNewOrderAdminNotification(event.order());
    } catch (Exception e) {
      log.error(
          "Admin order alert dispatch failed after retries for order {}",
          event.order().getOrderId(),
          e);
    }
  }

  /**
   * Listens for OrderStatusUpdatedEvent after the DB transaction has committed, and sends status
   * transition email to the customer asynchronously.
   */
  @Async("mailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderStatusUpdatedEvent(OrderStatusUpdatedEvent event) {
    log.info(
        "Handling OrderStatusUpdatedEvent after commit for order {} (status: {})",
        event.order().getOrderId(),
        event.newStatus());
    try {
      sendOrderStatusUpdateEmail(event.order(), event.newStatus());
    } catch (Exception e) {
      log.error(
          "Order status update dispatch failed after retries for order {}",
          event.order().getOrderId(),
          e);
    }
  }

  /**
   * Sends an order confirmation email to the customer who placed the order with retry on SMTP
   * failure.
   */
  @Retryable(
      retryFor = {MailException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 2000, multiplier = 2.0))
  public void sendOrderConfirmationEmail(Order order) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
      helper.setFrom(fromAddress, "Sujalam Agro Foods");
      helper.setTo(order.getUser().getEmail());
      helper.setSubject("Order Confirmation - #" + order.getOrderId());

      Context context = new Context();
      context.setVariable("order", order);
      String htmlContent = templateEngine.process("mail/order-confirmation", context);
      helper.setText(htmlContent, true);

      mailSender.send(message);
      log.info(
          "Sent order confirmation email to {} for order {}",
          order.getUser().getEmail(),
          order.getOrderId());
    } catch (MessagingException | UnsupportedEncodingException e) {
      log.error("Failed to construct order confirmation email for order {}", order.getOrderId(), e);
    } catch (MailException e) {
      log.warn(
          "SMTP attempt failed for order confirmation email (order: {}). Retrying if attempts remain...",
          order.getOrderId(),
          e);
      throw e;
    }
  }

  @Recover
  public void recoverOrderConfirmationEmail(MailException e, Order order) {
    log.error(
        "All retry attempts exhausted for order confirmation email (order: {})",
        order.getOrderId(),
        e);
  }

  /**
   * Sends an alert email to the business/admin when a new order is received with retry on SMTP
   * failure.
   */
  @Retryable(
      retryFor = {MailException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 2000, multiplier = 2.0))
  public void sendNewOrderAdminNotification(Order order) {
    if (adminEmail == null || adminEmail.isBlank()) {
      log.info(
          "Admin email is not configured; skipping admin new order alert for order {}",
          order.getOrderId());
      return;
    }
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
      helper.setFrom(fromAddress, "Sujalam Agro Foods Alerts");
      helper.setTo(adminEmail);
      helper.setSubject(
          "[New Order Alert] #" + order.getOrderId() + " - ₹" + order.getGrandTotal());

      String customerName =
          (order.getUser().getFirstName() != null ? order.getUser().getFirstName() : "")
              + " "
              + (order.getUser().getLastName() != null ? order.getUser().getLastName() : "");

      Context context = new Context();
      context.setVariable("order", order);
      context.setVariable("customerName", customerName.trim());
      String htmlContent = templateEngine.process("mail/admin-order-alert", context);
      helper.setText(htmlContent, true);

      mailSender.send(message);
      log.info(
          "Sent new order admin notification to {} for order {}", adminEmail, order.getOrderId());
    } catch (MessagingException | UnsupportedEncodingException e) {
      log.error("Failed to construct admin order alert for order {}", order.getOrderId(), e);
    } catch (MailException e) {
      log.warn(
          "SMTP attempt failed for admin order alert (order: {}). Retrying if attempts remain...",
          order.getOrderId(),
          e);
      throw e;
    }
  }

  @Recover
  public void recoverNewOrderAdminNotification(MailException e, Order order) {
    log.error(
        "All retry attempts exhausted for admin order alert (order: {})", order.getOrderId(), e);
  }

  /**
   * Sends a status update email to the customer when the order status transitions with retry on
   * SMTP failure.
   */
  @Retryable(
      retryFor = {MailException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 2000, multiplier = 2.0))
  public void sendOrderStatusUpdateEmail(Order order, OrderStatus newStatus) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
      helper.setFrom(fromAddress, "Sujalam Agro Foods");
      helper.setTo(order.getUser().getEmail());
      helper.setSubject(
          "Order #" + order.getOrderId() + " Status Update: " + formatStatusName(newStatus));

      Context context = new Context();
      context.setVariable("order", order);
      context.setVariable("statusMessage", getStatusMessage(newStatus));
      context.setVariable("statusName", formatStatusName(newStatus));
      context.setVariable("statusColor", getStatusColor(newStatus));
      String htmlContent = templateEngine.process("mail/order-status-update", context);
      helper.setText(htmlContent, true);

      mailSender.send(message);
      log.info(
          "Sent status update ({}) email to {} for order {}",
          newStatus,
          order.getUser().getEmail(),
          order.getOrderId());
    } catch (MessagingException | UnsupportedEncodingException e) {
      log.error("Failed to construct status update email for order {}", order.getOrderId(), e);
    } catch (MailException e) {
      log.warn(
          "SMTP attempt failed for order status update email (order: {}, status: {}). Retrying if attempts remain...",
          order.getOrderId(),
          newStatus,
          e);
      throw e;
    }
  }

  @Recover
  public void recoverOrderStatusUpdateEmail(MailException e, Order order, OrderStatus newStatus) {
    log.error(
        "All retry attempts exhausted for status update email (order: {}, status: {})",
        order.getOrderId(),
        newStatus,
        e);
  }

  private String getStatusMessage(OrderStatus status) {
    return switch (status) {
      case COMPLETED ->
          "Your order has been <b>delivered and completed</b>. We hope you enjoy your purchase!";
      case CANCELED ->
          "Your order has been <b>cancelled</b>. Any reserved product stock has been restored.";
      case PENDING -> "Your order is currently <b>pending confirmation</b>.";
    };
  }

  private String getStatusColor(OrderStatus status) {
    return switch (status) {
      case COMPLETED -> "#2e7d32";
      case CANCELED -> "#c62828";
      case PENDING -> "#ef6c00";
    };
  }

  private String formatStatusName(OrderStatus status) {
    return switch (status) {
      case COMPLETED -> "Completed / Delivered";
      case CANCELED -> "Cancelled";
      case PENDING -> "Pending";
    };
  }
}
