package com.jeevan.TradingApp.controller;

import com.jeevan.TradingApp.domain.OrderType;
import com.jeevan.TradingApp.modal.Coin;
import com.jeevan.TradingApp.modal.Order;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.request.CreateOrderRequest;
import com.jeevan.TradingApp.service.CoinService;
import com.jeevan.TradingApp.service.OrderService;
import com.jeevan.TradingApp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.sun.beans.introspect.PropertyInfo.Name.required;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private CoinService coinService;

//    @Autowired
//    private WalletTransactionalservice walletTransactionalservice;
    @PostMapping("/pay")
    public ResponseEntity<Order> payOrderPayment(@RequestHeader("Authorization") String jwt , @RequestBody CreateOrderRequest req) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);
        Coin coin = coinService.findById(req.getCoinId());

        Order order = orderService.processOrder(coin , req.getQuantity() , req.getOrderType() , user);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(@RequestHeader("Authorization") String jwtToken , @PathVariable Long orderId) throws Exception {
        User user = userService.findUserProfileByJwt(jwtToken);
        Order order = orderService.getOrderById(orderId);

        if(order.getUser().getId().equals(user.getId())){
            return ResponseEntity.ok(order);
        }
        else{
            throw new Exception("you don't have access");
        }
    }

    @GetMapping()
    public ResponseEntity<List<Order>> getAllOrdersForUser(@RequestHeader("Authorization") String jwt , @RequestParam(required = false) OrderType order_type , @RequestParam(required = false) String asset_symbol) throws Exception {
        Long userId = userService.findUserProfileByJwt(jwt).getId();

        List<Order> userOrders = orderService.getAllOrderUser(userId , order_type , asset_symbol);
        return ResponseEntity.ok(userOrders);
    }

}
