package com.example.orderms.factory;

import com.example.orderms.listener.dto.OrderCreatedEvent;
import com.example.orderms.listener.dto.OrderItemEvent;

import java.math.BigDecimal;
import java.util.List;

public class OrderCreatedEventFactory {

    public static OrderCreatedEvent buildWithOneItem() {

        var itens = new OrderItemEvent("notebook", 1, BigDecimal.valueOf(20.50));
        var event = new OrderCreatedEvent(1L, 2L, List.of(itens));

        return event;
    }

    public static OrderCreatedEvent buildWithTwoItens() {

        var item1 = new OrderItemEvent("notebook", 1, BigDecimal.valueOf(20.50));
        var item2 = new OrderItemEvent("monitor", 1, BigDecimal.valueOf(35.50));
        var event = new OrderCreatedEvent(1L, 2L, List.of(item1, item2));

        return event;
    }
}
