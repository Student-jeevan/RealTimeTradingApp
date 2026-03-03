package com.jeevan.TradingApp.controller;

import com.jeevan.TradingApp.modal.Coin;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.request.CreateOrderRequest;
import com.jeevan.TradingApp.response.TradeBreakdownResponse;
import com.jeevan.TradingApp.service.CoinService;
import com.jeevan.TradingApp.service.FeeService;
import com.jeevan.TradingApp.service.LedgerService;
import com.jeevan.TradingApp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/trade")
public class TradeBreakdownController {

    @Autowired
    private CoinService coinService;

    @Autowired
    private FeeService feeService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private UserService userService;

    @PostMapping("/breakdown")
    public ResponseEntity<TradeBreakdownResponse> getTradeBreakdown(@RequestHeader("Authorization") String jwt,
            @RequestBody CreateOrderRequest req) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);
        Coin coin = coinService.findById(req.getCoinId());

        BigDecimal currentPrice = BigDecimal.valueOf(coin.getCurrentPrice());
        BigDecimal quantity = BigDecimal.valueOf(req.getQuantity());
        BigDecimal tradeValue = currentPrice.multiply(quantity);

        BigDecimal fee = feeService.calculateFee(tradeValue);
        BigDecimal totalCost = tradeValue.add(fee); // Or subtract if sell, assuming buy here for generic "cost"

        BigDecimal currentAvailableBalance = ledgerService.calculateAvailableBalance(user.getId());
        BigDecimal availableBalanceAfterTrade = currentAvailableBalance.subtract(totalCost);

        TradeBreakdownResponse response = new TradeBreakdownResponse();
        response.setCurrentPrice(currentPrice);
        response.setTradeValue(tradeValue);
        response.setFee(fee);
        response.setTotalCost(totalCost);
        response.setAvailableBalanceAfterTrade(availableBalanceAfterTrade);

        return ResponseEntity.ok(response);
    }
}
