package com.gryffindor.excalibur.services;

import com.gryffindor.excalibur.model.db.Address;
import com.gryffindor.excalibur.model.db.Order;
import com.gryffindor.excalibur.model.db.OrderDetails;
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
import org.springframework.stereotype.Service;

@Service
public class EmailService {
  private static final Logger log = LoggerFactory.getLogger(EmailService.class);

  private final JavaMailSender mailSender;
  private final String fromAddress;

  @Autowired
  EmailService(JavaMailSender mailSender, @Value("${app.mail.from}") String fromAddress) {
    this.mailSender = mailSender;
    this.fromAddress = fromAddress;
  }

  public void sendOrderConfirmationEmail(Order order) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
      helper.setFrom(fromAddress, "Sujalam Agro Foods");
      helper.setTo(order.getUser().getEmail());
      helper.setSubject("Order Confirmation - #" + order.getOrderId());
      helper.setText(buildOrderConfirmationBody(order), true);
      mailSender.send(message);
    } catch (MessagingException | UnsupportedEncodingException | MailException e) {
      log.warn("Failed to send order confirmation email for order {}", order.getOrderId(), e);
    }
  }

  private String buildOrderConfirmationBody(Order order) {
    StringBuilder itemRows = new StringBuilder();
    for (OrderDetails item : order.getOrderDetails()) {
      itemRows
          .append("<tr>")
          .append("<td style=\"padding:6px 10px;border:1px solid #ddd;\">")
          .append(item.getProduct().getName())
          .append("</td>")
          .append("<td style=\"padding:6px 10px;border:1px solid #ddd;text-align:center;\">")
          .append(item.getOrderedQty())
          .append("</td>")
          .append("<td style=\"padding:6px 10px;border:1px solid #ddd;text-align:right;\">₹")
          .append(item.getUnitPrice())
          .append("</td>")
          .append("<td style=\"padding:6px 10px;border:1px solid #ddd;text-align:right;\">₹")
          .append(item.getSubTotal())
          .append("</td>")
          .append("</tr>");
    }

    Address address = order.getShippingAddress();
    String addressLine2 =
        address.getAddressLine2() == null || address.getAddressLine2().isBlank()
            ? ""
            : address.getAddressLine2() + "<br>";

    return "<div style=\"font-family:Arial,sans-serif;color:#333;\">"
        + "<p>Hi "
        + order.getUser().getFirstName()
        + ",</p>"
        + "<p>Thank you for shopping with Sujalam Agro Foods! Your order has been placed "
        + "successfully and will be paid via <b>Cash on Delivery</b>.</p>"
        + "<p><b>Order ID:</b> "
        + order.getOrderId()
        + "</p>"
        + "<table style=\"border-collapse:collapse;width:100%;margin:12px 0;\">"
        + "<tr style=\"background:#f5f5f5;\">"
        + "<th style=\"padding:6px 10px;border:1px solid #ddd;text-align:left;\">Product</th>"
        + "<th style=\"padding:6px 10px;border:1px solid #ddd;\">Qty</th>"
        + "<th style=\"padding:6px 10px;border:1px solid #ddd;\">Unit Price</th>"
        + "<th style=\"padding:6px 10px;border:1px solid #ddd;\">Subtotal</th>"
        + "</tr>"
        + itemRows
        + "</table>"
        + "<p><b>Order Total: ₹"
        + order.getOrderTotal()
        + "</b></p>"
        + "<p><b>Shipping Address:</b><br>"
        + address.getRecipientName()
        + "<br>"
        + address.getAddressLine1()
        + "<br>"
        + addressLine2
        + address.getCity()
        + ", "
        + address.getState()
        + " "
        + address.getPostalCode()
        + "<br>"
        + address.getCountry()
        + "</p>"
        + "<p>We'll notify you once your order is out for delivery. Thank you for choosing us!</p>"
        + "<p>&mdash; Team Sujalam Agro Foods</p>"
        + "</div>";
  }
}
