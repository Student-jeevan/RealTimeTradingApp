package com.jeevan.TradingApp.controller;

import com.jeevan.TradingApp.modal.PriceAlert;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.request.CreateAlertRequest;
import com.jeevan.TradingApp.service.PriceAlertService;
import com.jeevan.TradingApp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class PriceAlertController {

    @Autowired
    private PriceAlertService priceAlertService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<PriceAlert> createAlert(
            @RequestHeader("Authorization") String jwt,
            @RequestBody CreateAlertRequest req) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);
        PriceAlert alert = priceAlertService.createAlert(user, req.getCoinId(), req.getTargetPrice(),
                req.getCondition());
        return ResponseEntity.ok(alert);
    }

    @GetMapping("/active")
    public ResponseEntity<List<PriceAlert>> getActiveAlerts(@RequestHeader("Authorization") String jwt)
            throws Exception {
        User user = userService.findUserProfileByJwt(jwt);
        return ResponseEntity.ok(priceAlertService.getActiveAlerts(user.getId()));
    }

    @GetMapping("/triggered")
    public ResponseEntity<List<PriceAlert>> getTriggeredAlerts(@RequestHeader("Authorization") String jwt)
            throws Exception {
        User user = userService.findUserProfileByJwt(jwt);
        return ResponseEntity.ok(priceAlertService.getTriggeredAlerts(user.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@RequestHeader("Authorization") String jwt, @PathVariable Long id)
            throws Exception {
        User user = userService.findUserProfileByJwt(jwt);
        priceAlertService.deleteAlert(id, user.getId());
        return ResponseEntity.ok().build();
    }
}
