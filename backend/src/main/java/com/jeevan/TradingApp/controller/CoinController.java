package com.jeevan.TradingApp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeevan.TradingApp.modal.Coin;
import com.jeevan.TradingApp.service.CoinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/coins")
public class CoinController {
    @Autowired
    private CoinService coinService;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping
    ResponseEntity<List<Coin>> getCoinList(@RequestParam(required = false, name = "page") int page) {
        List<Coin> coin = coinService.getCoinList(page);
        return new ResponseEntity<>(coin, HttpStatus.ACCEPTED);

    }

    @GetMapping("/{coinId}/chart")
    ResponseEntity<JsonNode> getMarketChart(@PathVariable String coinId, @RequestParam("days") int days) {
        String res = coinService.getMarketChart(coinId, days);
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(res);
        } catch (Exception e) {
            throw new com.jeevan.TradingApp.exception.CustomException("Error parsing market chart", "JSON_ERROR");
        }
        return new ResponseEntity<>(jsonNode, HttpStatus.ACCEPTED);

    }

    @GetMapping("/search")
    ResponseEntity<JsonNode> searchCoin(@RequestParam("q") String keyword) {
        String coin = coinService.searchCoin(keyword);
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(coin);
        } catch (Exception e) {
            throw new com.jeevan.TradingApp.exception.CustomException("Error parsing search results", "JSON_ERROR");
        }
        return ResponseEntity.ok(jsonNode);
    }

    @GetMapping("/top50")
    ResponseEntity<JsonNode> getTop50CoinByMarketCap() {
        String coin = coinService.getTop50CoinsByMarketCapRank();
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(coin);
        } catch (Exception e) {
            throw new com.jeevan.TradingApp.exception.CustomException("Error parsing top coins", "JSON_ERROR");
        }
        return ResponseEntity.ok(jsonNode);
    }

    @GetMapping("/trending")
    ResponseEntity<JsonNode> getTreadingCoin() {
        String coin = coinService.getTreadingCoins();
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(coin);
        } catch (Exception e) {
            throw new com.jeevan.TradingApp.exception.CustomException("Error parsing trending coins", "JSON_ERROR");
        }
        return ResponseEntity.ok(jsonNode);
    }

    @GetMapping("/details/{coinId}")
    ResponseEntity<JsonNode> getCoinDetails(@PathVariable String coinId) {
        String coin = coinService.getCoinDetails(coinId);
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(coin);
        } catch (Exception e) {
            throw new com.jeevan.TradingApp.exception.CustomException("Error parsing coin details", "JSON_ERROR");
        }
        return ResponseEntity.ok(jsonNode);
    }

}
