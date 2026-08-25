package com.gryffindor.excalibur.model.event;

import com.gryffindor.excalibur.model.constants.OrderStatus;
import com.gryffindor.excalibur.model.db.Order;

/**
 * Domain event published when an order's status transitions. Handled after transaction commit to
 * dispatch notifications.
 */
public record OrderStatusUpdatedEvent(Order order, OrderStatus newStatus) {}
