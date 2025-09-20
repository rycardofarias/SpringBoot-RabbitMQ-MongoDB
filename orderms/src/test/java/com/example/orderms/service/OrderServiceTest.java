package com.example.orderms.service;

import com.example.orderms.entity.OrderEntity;
import com.example.orderms.factory.OrderCreatedEventFactory;
import com.example.orderms.factory.OrderEntityFactory;
import com.example.orderms.repository.OrderRepository;
import org.bson.Document;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    MongoTemplate mongoTemplate;

    @InjectMocks
    OrderService orderService;

    @Captor
    ArgumentCaptor<OrderEntity> orderEntityCaptor;

    @Captor
    ArgumentCaptor<Aggregation> aggregationCaptor;

    @Nested
    class Save {

        @Test
        void shouldCallRepositorySave() {

            //ARRANGE
            var event = OrderCreatedEventFactory.buildWithOneItem();

            //ACT
            orderService.save(event);

            //ASSERT
            verify(orderRepository, times(1)).save(any());
        }

        @Test
        void shouldMapEventToEntityWithSuccess() {
            // ARRANGE
            var event = OrderCreatedEventFactory.buildWithOneItem();

            // ACT
            orderService.save(event);

            // ASSERT
            verify(orderRepository, times(1)).save(orderEntityCaptor.capture());

            var entity = orderEntityCaptor.getValue();

            assertEquals(event.codigoPedido(), entity.getOrderId());
            assertEquals(event.codigoCliente(), entity.getCustomerId());
            assertNotNull(entity.getTotal());
            assertEquals(event.itens().get(0).produto(), entity.getItems().get(0).getProduct());
            assertEquals(event.itens().get(0).quantidade(), entity.getItems().get(0).getQuantity());
            assertEquals(event.itens().get(0).preco(), entity.getItems().get(0).getPrice());
        }

        @Test
        void shouldCalculateOrderTotalWithSucess() {

            // ARRANGE
            var event = OrderCreatedEventFactory.buildWithTwoItens();
            var totalItem1 =
                    event.itens().get(0).preco().multiply(BigDecimal.valueOf(event.itens().get(0).quantidade()));
            var totalItem2 =
                    event.itens().get(1).preco().multiply(BigDecimal.valueOf(event.itens().get(1).quantidade()));
            var orderTotal = totalItem1.add(totalItem2);

            // ACT
            orderService.save(event);

            // ASSERT
            verify(orderRepository, times(1)).save(orderEntityCaptor.capture());

            var entity = orderEntityCaptor.getValue();

            assertNotNull(entity.getTotal());
            assertEquals(orderTotal, entity.getTotal());
        }

    }

    @Nested
    class FindAllByCustomerId {

        @Test
        void shouldCallRepository() {

            //ARRANGE
            var customerId = 1L;
            var pageRequest = PageRequest.of(0, 10);
            doReturn(OrderEntityFactory.buildWithPage())
                    .when(orderRepository).findAllByCustomerId(eq(customerId), eq(pageRequest));
            //ACT
            var response = orderService.findAllByCustomerId(customerId, pageRequest);

            //ASSERT
            verify(orderRepository, times(1)).findAllByCustomerId(eq(customerId), eq(pageRequest));

        }

        @Test
        void shouldMapResponse() {

            //ARRANGE
            var customerId = 1L;
            var pageRequest = PageRequest.of(0, 10);
            var page = OrderEntityFactory.buildWithPage();

            doReturn(page)
                    .when(orderRepository).findAllByCustomerId(anyLong(), any());
            //ACT
            var response = orderService.findAllByCustomerId(customerId, pageRequest);

            //ASSERT
            assertEquals(page.getTotalPages(), response.getTotalPages());
            assertEquals(page.getTotalElements(), response.getTotalElements());
            assertEquals(page.getSize(), response.getSize());
            assertEquals(page.getNumber(), response.getNumber());

            assertEquals(page.getContent().get(0).getOrderId(), response.getContent().get(0).orderId());
            assertEquals(page.getContent().get(0).getCustomerId(), response.getContent().get(0).customerId());
            assertEquals(page.getContent().get(0).getTotal(), response.getContent().get(0).total());
        }
    }

    @Nested
    class FindTotalOnOrdersByCustomerId {

        @Test
        void shouldCallMongoTemplate() {
            // ARRANGE
            var customerId = 1L;
            var totalExpected = BigDecimal.valueOf(1);
            var aggregationResult = mock(AggregationResults.class);
            doReturn(new Document("total",  totalExpected)).when(aggregationResult).getUniqueMappedResult();
            doReturn(aggregationResult).when(mongoTemplate).aggregate(any(Aggregation.class), anyString(), eq(Document.class));

            // ACT
            var total = orderService.findTotalOnOrdersByCustomerId(customerId);

            // ASSERT
            verify(mongoTemplate, times(1)).aggregate(any(Aggregation.class), anyString(), eq(Document.class));
            assertEquals(totalExpected, total);
        }

        @Test
        void shouldUseCorrectAggregation() {
            // ARRANGE
            var customerId = 1L;
            var totalExpected = BigDecimal.valueOf(1);
            var aggregationResult = mock(AggregationResults.class);
            doReturn(new Document("total",  totalExpected)).when(aggregationResult).getUniqueMappedResult();
            doReturn(aggregationResult).when(mongoTemplate).aggregate(aggregationCaptor.capture(), anyString(), eq(Document.class));

            // ACT
            orderService.findTotalOnOrdersByCustomerId(customerId);

            // ASSERT
            var aggregation = aggregationCaptor.getValue();
            var aggregationExpected = newAggregation(
                    match(Criteria.where("customerId").is(customerId)),
                    group().sum("total").as("total")
            );

            assertEquals(aggregationExpected.toString(), aggregation.toString());
        }

        @Test
        void shouldQueryCorrectTable() {
            // ARRANGE
            var customerId = 1L;
            var totalExpected = BigDecimal.valueOf(1);
            var aggregationResult = mock(AggregationResults.class);
            doReturn(new Document("total",  totalExpected)).when(aggregationResult).getUniqueMappedResult();
            doReturn(aggregationResult).when(mongoTemplate).aggregate(any(Aggregation.class), eq("tb_orders"), eq(Document.class));

            // ACT
            orderService.findTotalOnOrdersByCustomerId(customerId);

            // ASSERT
            verify(mongoTemplate, times(1)).aggregate(any(Aggregation.class), eq("tb_orders"), eq(Document.class));
        }
    }
}