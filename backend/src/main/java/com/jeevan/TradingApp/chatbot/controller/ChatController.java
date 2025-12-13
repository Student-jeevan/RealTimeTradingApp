package com.jeevan.TradingApp.chatbot.controller;

import com.jeevan.TradingApp.chatbot.dto.ChatRequest;
import com.jeevan.TradingApp.chatbot.dto.ChatResponse;
import com.jeevan.TradingApp.chatbot.service.ChatService;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for chatbot endpoints
 * Handles authenticated chat requests from frontend
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final UserService userService;

    public ChatController(ChatService chatService, UserService userService) {
        this.chatService = chatService;
        this.userService = userService;
    }

    /**
     * POST endpoint for processing chat messages
     * Requires JWT authentication
     *
     * @param request ChatRequest containing user message
     * @param jwt JWT token from Authorization header
     * @return ChatResponse with bot's answer
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request, @RequestHeader("Authorization") String jwt) {
        try {
            logger.info("Received chat request");

            // Authenticate user via JWT
            User user = userService.findUserProfileByJwt(jwt);

            // Validate request
            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ChatResponse("Please provide a valid message."));
            }

            // Process message through chat service
            ChatResponse response = chatService.processMessage(request.getMessage(), user);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error handling chat request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatResponse("An error occurred while processing your request. Please try again."));
        }
    }
}

