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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
import com.example.inventory.application.handlers.DeliverOrderCommandHandler;
import com.example.inventory.application.handlers.GetOrderQueryHandler;
import com.example.inventory.application.handlers.ListOrdersQueryHandler;
import com.example.inventory.application.handlers.PlaceOrderCommandHandler;
import com.example.inventory.application.handlers.ProcessPaymentCommandHandler;
import com.example.inventory.application.handlers.ShipOrderCommandHandler;
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
    private final ShipOrderCommandHandler shipOrderCommandHandler;
    private final DeliverOrderCommandHandler deliverOrderCommandHandler;
    private final OrderMapper orderMapper;
    private final com.example.inventory.infrastructure.websocket.OrderWebSocketService orderWebSocketService;

    public OrderController(PlaceOrderCommandHandler placeOrderCommandHandler,
            GetOrderQueryHandler getOrderQueryHandler,
            ListOrdersQueryHandler listOrdersQueryHandler,
            ProcessPaymentCommandHandler processPaymentCommandHandler,
            CancelOrderCommandHandler cancelOrderCommandHandler,
            ShipOrderCommandHandler shipOrderCommandHandler,
            DeliverOrderCommandHandler deliverOrderCommandHandler,
            OrderMapper orderMapper,
            com.example.inventory.infrastructure.websocket.OrderWebSocketService orderWebSocketService) {
        this.placeOrderCommandHandler = placeOrderCommandHandler;
        this.getOrderQueryHandler = getOrderQueryHandler;
        this.listOrdersQueryHandler = listOrdersQueryHandler;
        this.processPaymentCommandHandler = processPaymentCommandHandler;
        this.cancelOrderCommandHandler = cancelOrderCommandHandler;
        this.shipOrderCommandHandler = shipOrderCommandHandler;
        this.deliverOrderCommandHandler = deliverOrderCommandHandler;
        this.orderMapper = orderMapper;
        this.orderWebSocketService = orderWebSocketService;
    }

    @PostMapping("/orders")
    @Operation(summary = "Create a new order", description = "Creates a pending order from a valid request payload.")
    @ApiResponse(responseCode = "201", description = "Order created")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    public ResponseEntity<com.example.inventory.web.dto.OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request, Authentication authentication) {
        CreateOrderRequest effectiveRequest = request;
        if (authentication != null && !isAdmin(authentication)) {
            effectiveRequest = new CreateOrderRequest(authentication.getName(), request.items());
        }
        PlaceOrderCommand command = orderMapper.toPlaceOrderCommand(effectiveRequest);
        OrderResponse orderResponse = placeOrderCommandHandler.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderMapper.toWebResponse(orderResponse));
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Get one order", description = "Fetches an order by its identifier.")
    @ApiResponse(responseCode = "200", description = "Order found")
    public ResponseEntity<com.example.inventory.web.dto.OrderResponse> getOrder(
            @Parameter(description = "Unique order identifier") @PathVariable("id") String id,
            Authentication authentication) {
        OrderResponse orderResponse = getOrderQueryHandler.handle(new GetOrderQuery(id));
        assertOwnership(orderMapper.toWebResponse(orderResponse), authentication);
        return ResponseEntity.ok(orderMapper.toWebResponse(orderResponse));
    }

    @GetMapping("/orders")
    @Operation(summary = "List orders", description = "Lists orders for a customer, optionally filtered by status. Non-admin callers are always scoped to their own orders.")
    @ApiResponse(responseCode = "200", description = "Orders listed")
    public ResponseEntity<List<com.example.inventory.web.dto.OrderResponse>> listOrders(
            @Parameter(description = "Optional customer identifier (admin only)") @RequestParam(required = false) String customerId,
            @Parameter(description = "Optional status filter") @RequestParam(required = false) String status,
            Authentication authentication) {
        String effectiveCustomerId = customerId;
        if (!isAdmin(authentication)) {
            effectiveCustomerId = authentication != null ? authentication.getName() : null;
        }
        List<OrderResponse> responses = listOrdersQueryHandler
                .handle(new ListOrdersQuery(effectiveCustomerId, status));
        return ResponseEntity.ok(responses.stream().map(orderMapper::toWebResponse).toList());
    }

    @GetMapping("/orders/status/{orderId}")
    @Operation(summary = "Get order status", description = "Returns the current status of an order.")
    @ApiResponse(responseCode = "200", description = "Order status returned")
    public ResponseEntity<Map<String, Object>> getOrderStatus(
            @Parameter(description = "Unique order identifier") @PathVariable("orderId") String orderId,
            Authentication authentication) {
        com.example.inventory.web.dto.OrderResponse orderResponse = orderMapper.toWebResponse(
                getOrderQueryHandler.handle(new GetOrderQuery(orderId)));
        assertOwnership(orderResponse, authentication);
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("orderId", orderId);
        payload.put("status", orderResponse.status());
        payload.put("reservationSecondsRemaining", orderResponse.reservationSecondsRemaining());
        return ResponseEntity.ok(payload);
    }

    @PostMapping("/orders/{id}/payment")
    @Operation(summary = "Process payment for an order", description = "Transitions a pending order to a paid state.")
    @ApiResponse(responseCode = "200", description = "Payment processed")
    public ResponseEntity<com.example.inventory.web.dto.OrderResponse> processPayment(
            @Parameter(description = "Unique order identifier") @PathVariable("id") String id,
            Authentication authentication) {
        OrderResponse orderResponse = processPaymentCommandHandler.handle(new ProcessPaymentCommand(id));
        assertOwnership(orderMapper.toWebResponse(orderResponse), authentication);
        return ResponseEntity.ok(orderMapper.toWebResponse(orderResponse));
    }

    @PostMapping("/orders/{id}/cancel")
    @Operation(summary = "Cancel an order", description = "Cancels an order when allowed by the domain lifecycle.")
    @ApiResponse(responseCode = "200", description = "Order cancelled")
    public ResponseEntity<com.example.inventory.web.dto.OrderResponse> cancelOrder(
            @Parameter(description = "Unique order identifier") @PathVariable("id") String id,
            Authentication authentication) {
        OrderResponse orderResponse = cancelOrderCommandHandler.handle(new CancelOrderCommand(id));
        assertOwnership(orderMapper.toWebResponse(orderResponse), authentication);
        return ResponseEntity.ok(orderMapper.toWebResponse(orderResponse));
    }

    @PostMapping("/orders/{id}/notify-test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> notifyTest(@PathVariable("id") String id) {
        orderWebSocketService.publishOrderUpdate(id, Map.of("orderId", id, "status", "TEST_UPDATE"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/orders/{id}/notify-user/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> notifyUser(@PathVariable("id") String id, @PathVariable("username") String username) {
        orderWebSocketService.publishToUser(username, Map.of("orderId", id, "status", "USER_UPDATE"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/orders/{id}/ship")
    @PreAuthorize("hasAnyRole('WAREHOUSE', 'ADMIN')")
    @Operation(summary = "Ship a paid order", description = "Transitions a paid order to the shipped state.")
    @ApiResponse(responseCode = "200", description = "Order shipped")
    public ResponseEntity<com.example.inventory.web.dto.OrderResponse> shipOrder(
            @Parameter(description = "Unique order identifier") @PathVariable("id") String id) {
        OrderResponse orderResponse = shipOrderCommandHandler.handle(
                new com.example.inventory.application.commands.ShipOrderCommand(id));
        orderWebSocketService.publishOrderUpdate(id, Map.of("orderId", id, "status", orderResponse.status()));
        return ResponseEntity.ok(orderMapper.toWebResponse(orderResponse));
    }

    @PostMapping("/orders/{id}/deliver")
    @PreAuthorize("hasAnyRole('WAREHOUSE', 'ADMIN')")
    @Operation(summary = "Deliver a shipped order", description = "Transitions a shipped order to the delivered state.")
    @ApiResponse(responseCode = "200", description = "Order delivered")
    public ResponseEntity<com.example.inventory.web.dto.OrderResponse> deliverOrder(
            @Parameter(description = "Unique order identifier") @PathVariable("id") String id) {
        OrderResponse orderResponse = deliverOrderCommandHandler.handle(
                new com.example.inventory.application.commands.DeliverOrderCommand(id));
        orderWebSocketService.publishOrderUpdate(id, Map.of("orderId", id, "status", orderResponse.status()));
        return ResponseEntity.ok(orderMapper.toWebResponse(orderResponse));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private void assertOwnership(com.example.inventory.web.dto.OrderResponse response, Authentication authentication) {
        if (isAdmin(authentication)) {
            return;
        }
        String principalName = authentication != null ? authentication.getName() : null;
        if (principalName == null || !response.customerId().equals(principalName)) {
            throw new com.example.inventory.application.ResourceNotFoundException(
                    "Order not found: " + response.id());
        }
    }
}
