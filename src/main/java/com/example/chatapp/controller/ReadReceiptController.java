package com.example.chatapp.controller;

import com.example.chatapp.dto.PresenceDtos.ReadReceiptDto;
import com.example.chatapp.service.ReadReceiptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomId}/receipts")
public class ReadReceiptController {

    private final ReadReceiptService readReceiptService;

    public ReadReceiptController(ReadReceiptService readReceiptService) {
        this.readReceiptService = readReceiptService;
    }

    /**
     * GET /api/rooms/{roomId}/receipts
     * Current read position for every user who has read anything in this room.
     * Called once when a client opens a room, so "seen" state is correct
     * immediately \u2014 without waiting for someone to send a new receipt.
     */
    @GetMapping
    public ResponseEntity<List<ReadReceiptDto>> getReceipts(@PathVariable Long roomId) {
        return ResponseEntity.ok(readReceiptService.getReceipts(roomId));
    }
}