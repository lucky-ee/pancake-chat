package com.example.chatapp.controller;

import com.example.chatapp.dto.ChatMessageDto;
import com.example.chatapp.service.ChatMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomId}/messages")
public class MessageController {

    private final ChatMessageService chatMessageService;

    public MessageController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    /**
     * GET /api/rooms/{roomId}/messages?page=0&size=50
     * page 0 = most recent `size` messages, returned oldest-first for easy rendering.
     */
    @GetMapping
    public ResponseEntity<List<ChatMessageDto>> getHistory(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(chatMessageService.getHistory(roomId, page, size));
    }
}
