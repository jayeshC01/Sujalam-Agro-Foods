package com.gryffindor.excalibur.model.event;

import com.gryffindor.excalibur.model.db.Order;

/**
 * Domain event published when an order is created. Handled after transaction commit to dispatch
 * notifications.
 */
public record OrderPlacedEvent(Order order) {}
