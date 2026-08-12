package com.example.inventory.web.controllers;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.inventory.application.commands.CancelOrderCommand;
import com.example.inventory.application.commands.PlaceOrderCommand;
import com.example.inventory.application.commands.ProcessPaymentCommand;
import com.example.inventory.application.dto.OrderResponse;
import com.example.inventory.application.queries.GetOrderQuery;
import com.example.inventory.application.queries.ListOrdersQuery;
import com.example.inventory.application.handlers.CancelOrderCommandHandler;
import com.example.inventory.application.handlers.GetOrderQueryHandler;
import com.example.inventory.application.handlers.ListOrdersQueryHandler;
import com.example.inventory.application.handlers.PlaceOrderCommandHandler;
import com.example.inventory.application.handlers.ProcessPaymentCommandHandler;
import com.example.inventory.web.dto.CreateOrderRequest;
import com.example.inventory.web.mapper.OrderMapper;

@RestController
@RequestMapping({ "/api", "" })
@Validated
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private final PlaceOrderCommandHandler placeOrderCommandHandler;
    private final GetOrderQueryHandler getOrderQueryHandler;
    private final ListOrdersQueryHandler listOrdersQueryHandler;
    private final ProcessPaymentCommandHandler processPaymentCommandHandler;
    private final CancelOrderCommandHandler cancelOrderCommandHandler;
    private final OrderMapper orderMapper;
    private final com.example.inventory.infrastructure.websocket.OrderWebSocketService orderWebSocketService;

    public OrderController(PlaceOrderCommandHandler placeOrderCommandHandler,
            GetOrderQueryHandler getOrderQueryHandler,
            ListOrdersQueryHandler listOrdersQueryHandler,
            ProcessPaymentCommandHandler processPaymentCommandHandler,
            CancelOrderCommandHandler cancelOrderCommandHandler,
            OrderMapper orderMapper,
            com.example.inventory.infrastructure.websocket.OrderWebSocketService orderWebSocketService) {
        this.placeOrderCommandHandler = placeOrderCommandHandler;
        this.getOrderQueryHandler = getOrderQueryHandler;
        this.listOrdersQueryHandler = listOrdersQueryHandler;
        this.processPaymentCommandHandler = processPaymentCommandHandler;
        this.cancelOrderCommandHandler = cancelOrderCommandHandler;
        this.orderMapper = orderMapper;
        this.orderWebSocketService = orderWebSocketService;
    }

    @PostMapping("/orders")
    @Operation(summary = "Create a new order", description = "Creates a pending order from a valid request payload.")
    @ApiResponse(responseCode = "201", description = "Order created")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    public ResponseEntity<com.example.inventory.web.dto.OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        PlaceOrderCommand command = orderMapper.toPlaceOrderCommand(request);
        OrderResponse orderResponse = placeOrderCommandHandler.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderMapper.toWebResponse(orderResponse));
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Get one order", description = "Fetches an order by its identifier.")
    @ApiResponse(responseCode = "200", description = "Order found")
    public ResponseEntity<com.example.inventory.web.dto.OrderResponse> getOrder(
            @Parameter(description = "Unique order identifier") @PathVariable("id") String id) {
        OrderResponse orderResponse = getOrderQueryHandler.handle(new GetOrderQuery(id));
        return ResponseEntity.ok(orderMapper.toWebResponse(orderResponse));
    }

    @GetMapping("/orders")
    @Operation(summary = "List orders", description = "Lists orders for a customer, optionally filtered by status.")
    @ApiResponse(responseCode = "200", description = "Orders listed")
    public ResponseEntity<List<com.example.inventory.web.dto.OrderResponse>> listOrders(
            @Parameter(description = "Optional customer identifier") @RequestParam(required = false) String customerId,
            @Parameter(description = "Optional status filter") @RequestParam(required = false) String status) {
        List<OrderResponse> responses = listOrdersQueryHandler.handle(new ListOrdersQuery(customerId, status));
        return ResponseEntity.ok(responses.stream().map(orderMapper::toWebResponse).toList());
    }

    @GetMapping("/orders/status/{orderId}")
    @Operation(summary = "Get order status", description = "Returns the current status of an order.")
    @ApiResponse(responseCode = "200", description = "Order status returned")
    public ResponseEntity<Map<String, String>> getOrderStatus(
            @Parameter(description = "Unique order identifier") @PathVariable("orderId") String orderId) {
        com.example.inventory.web.dto.OrderResponse orderResponse = orderMapper.toWebResponse(
                getOrderQueryHandler.handle(new GetOrderQuery(orderId)));
        return ResponseEntity.ok(Map.of("orderId", orderId, "status", orderResponse.status()));
    }

    @PostMapping("/orders/{id}/payment")
    @Operation(summary = "Process payment for an order", description = "Transitions a pending order to a paid state.")
    @ApiResponse(responseCode = "200", description = "Payment processed")
    public ResponseEntity<com.example.inventory.web.dto.OrderResponse> processPayment(
            @Parameter(description = "Unique order identifier") @PathVariable("id") String id) {
        OrderResponse orderResponse = processPaymentCommandHandler.handle(new ProcessPaymentCommand(id));
        return ResponseEntity.ok(orderMapper.toWebResponse(orderResponse));
    }

    @PostMapping("/orders/{id}/cancel")
    @Operation(summary = "Cancel an order", description = "Cancels an order when allowed by the domain lifecycle.")
    @ApiResponse(responseCode = "200", description = "Order cancelled")
    public ResponseEntity<com.example.inventory.web.dto.OrderResponse> cancelOrder(
            @Parameter(description = "Unique order identifier") @PathVariable("id") String id) {
        OrderResponse orderResponse = cancelOrderCommandHandler.handle(new CancelOrderCommand(id));
        return ResponseEntity.ok(orderMapper.toWebResponse(orderResponse));
    }

    @PostMapping("/orders/{id}/notify-test")
    public ResponseEntity<Void> notifyTest(@PathVariable("id") String id) {
        orderWebSocketService.publishOrderUpdate(id, Map.of("orderId", id, "status", "TEST_UPDATE"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/orders/{id}/notify-user/{username}")
    public ResponseEntity<Void> notifyUser(@PathVariable("id") String id, @PathVariable("username") String username) {
        orderWebSocketService.publishToUser(username, Map.of("orderId", id, "status", "USER_UPDATE"));
        return ResponseEntity.ok().build();
    }
}
