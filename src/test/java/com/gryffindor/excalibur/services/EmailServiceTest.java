package com.gryffindor.excalibur.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.gryffindor.excalibur.model.constants.OrderStatus;
import com.gryffindor.excalibur.model.constants.PaymentMethod;
import com.gryffindor.excalibur.model.constants.PaymentStatus;
import com.gryffindor.excalibur.model.db.Address;
import com.gryffindor.excalibur.model.db.Order;
import com.gryffindor.excalibur.model.db.OrderDetails;
import com.gryffindor.excalibur.model.db.Product;
import com.gryffindor.excalibur.model.db.User;
import com.gryffindor.excalibur.model.event.OrderPlacedEvent;
import com.gryffindor.excalibur.model.event.OrderStatusUpdatedEvent;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

  @Mock private JavaMailSender mailSender;
  @Mock private TemplateEngine templateEngine;

  private EmailService emailService;
  private Order sampleOrder;

  @BeforeEach
  void setUp() {
    emailService =
        new EmailService(
            mailSender, templateEngine, "orders@sujalamagro.com", "admin@sujalamagro.com");

    lenient()
        .when(templateEngine.process(any(String.class), any(Context.class)))
        .thenReturn("<html>Email Content</html>");

    lenient()
        .when(mailSender.createMimeMessage())
        .thenReturn(new MimeMessage(Session.getInstance(new Properties())));

    User user = new User();
    user.setId("u1");
    user.setFirstName("Ramesh");
    user.setLastName("Patel");
    user.setEmail("ramesh@example.com");

    Product product = new Product();
    product.setId("p1");
    product.setName("Cashews 500g");
    product.setPrice(new BigDecimal("200.00"));
    product.setGstRate(new BigDecimal("0.05"));

    OrderDetails orderDetail = new OrderDetails();
    orderDetail.setProduct(product);
    orderDetail.setOrderedQty(2);
    orderDetail.setUnitPrice(new BigDecimal("200.00"));
    orderDetail.setSubTotal(new BigDecimal("400.00"));

    Address address = new Address();
    address.setRecipientName("Ramesh Patel");
    address.setAddressLine1("123 Farm Road");
    address.setCity("Nashik");
    address.setState("Maharashtra");
    address.setPostalCode("422001");
    address.setCountry("India");
    address.setPhoneNumber("9876543210");

    sampleOrder = new Order();
    sampleOrder.setOrderId("ORD-12345");
    sampleOrder.setUser(user);
    sampleOrder.setOrderStatus(OrderStatus.PROCESSING);
    sampleOrder.setPaymentMethod(PaymentMethod.UPI);
    sampleOrder.setPaymentStatus(PaymentStatus.PAID);
    sampleOrder.setOrderDetails(List.of(orderDetail));
    sampleOrder.setSubTotal(new BigDecimal("400.00"));
    sampleOrder.setTaxAmount(new BigDecimal("19.05"));
    sampleOrder.setDeliveryCharge(new BigDecimal("100.00"));
    sampleOrder.setGrandTotal(new BigDecimal("500.00"));
    sampleOrder.setShippingAddress(address);
  }

  @Test
  @DisplayName("sendOrderConfirmationEmail constructs and sends mime message to customer")
  void sendOrderConfirmationEmail_sendsToCustomer() {
    emailService.sendOrderConfirmationEmail(sampleOrder);

    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine).process(eq("mail/order-confirmation"), contextCaptor.capture());
    Order contextOrder = (Order) contextCaptor.getValue().getVariable("order");
    assertThat(contextOrder.getPaymentMethod()).isEqualTo(PaymentMethod.UPI);
    assertThat(contextOrder.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);

    verify(mailSender).send(any(MimeMessage.class));
  }

  @Test
  @DisplayName("sendNewOrderAdminNotification sends alert to admin email")
  void sendNewOrderAdminNotification_sendsToAdmin() {
    emailService.sendNewOrderAdminNotification(sampleOrder);

    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine).process(eq("mail/admin-order-alert"), contextCaptor.capture());
    Order contextOrder = (Order) contextCaptor.getValue().getVariable("order");
    assertThat(contextOrder.getPaymentMethod()).isEqualTo(PaymentMethod.UPI);
    assertThat(contextOrder.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);

    verify(mailSender).send(any(MimeMessage.class));
  }

  @Test
  @DisplayName("sendNewOrderAdminNotification skips sending when admin email is null or blank")
  void sendNewOrderAdminNotification_skips_whenAdminEmailBlank() {
    EmailService serviceWithoutAdmin =
        new EmailService(mailSender, templateEngine, "orders@sujalamagro.com", "");

    serviceWithoutAdmin.sendNewOrderAdminNotification(sampleOrder);

    verify(mailSender, never()).send(any(MimeMessage.class));
  }

  @Test
  @DisplayName("sendOrderStatusUpdateEmail sends update email for COMPLETED status")
  void sendOrderStatusUpdateEmail_sendsCompletedUpdate() {
    emailService.sendOrderStatusUpdateEmail(sampleOrder, OrderStatus.COMPLETED);

    verify(mailSender).send(any(MimeMessage.class));
  }

  @Test
  @DisplayName("sendOrderStatusUpdateEmail sends update email for CANCELED status")
  void sendOrderStatusUpdateEmail_sendsCanceledUpdate() {
    emailService.sendOrderStatusUpdateEmail(sampleOrder, OrderStatus.CANCELED);

    verify(mailSender).send(any(MimeMessage.class));
  }

  @Test
  @DisplayName("handleOrderPlacedEvent sends both confirmation and admin alert")
  void handleOrderPlacedEvent_dispatchesBothCustomerAndAdminEmails() {
    emailService.handleOrderPlacedEvent(new OrderPlacedEvent(sampleOrder));

    // verify both customer confirmation and admin alert emails were sent
    verify(mailSender, times(2)).send(any(MimeMessage.class));
  }

  @Test
  @DisplayName("handleOrderStatusUpdatedEvent sends status update email to customer")
  void handleOrderStatusUpdatedEvent_dispatchesStatusEmail() {
    emailService.handleOrderStatusUpdatedEvent(
        new OrderStatusUpdatedEvent(sampleOrder, OrderStatus.COMPLETED));

    verify(mailSender).send(any(MimeMessage.class));
  }

  @Test
  @DisplayName("sendOrderConfirmationEmail propagates MailException so Spring Retry can retry")
  void sendOrderConfirmationEmail_propagatesMailExceptionForRetry() {
    doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

    assertThatThrownBy(() -> emailService.sendOrderConfirmationEmail(sampleOrder))
        .isInstanceOf(MailSendException.class);
  }

  @Test
  @DisplayName(
      "handleOrderPlacedEvent gracefully handles exhausted retries without crashing listener")
  void handleOrderPlacedEvent_handlesExhaustedRetriesGracefully() {
    doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

    assertThatNoException()
        .isThrownBy(() -> emailService.handleOrderPlacedEvent(new OrderPlacedEvent(sampleOrder)));
  }

  @Test
  @DisplayName("recover methods execute without throwing")
  void recoverMethods_executeCleanly() {
    MailSendException exception = new MailSendException("Failed");

    assertThatNoException()
        .isThrownBy(() -> emailService.recoverOrderConfirmationEmail(exception, sampleOrder));
    assertThatNoException()
        .isThrownBy(() -> emailService.recoverNewOrderAdminNotification(exception, sampleOrder));
    assertThatNoException()
        .isThrownBy(
            () ->
                emailService.recoverOrderStatusUpdateEmail(
                    exception, sampleOrder, OrderStatus.COMPLETED));
  }
}
