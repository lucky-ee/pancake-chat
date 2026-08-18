package com.example.chatapp.service;

import com.example.chatapp.dto.PresenceDtos.ReadReceiptDto;
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.ReadReceipt;
import com.example.chatapp.repository.ReadReceiptRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ReadReceiptService {

    private final ReadReceiptRepository readReceiptRepository;
    private final RoomService roomService;

    public ReadReceiptService(ReadReceiptRepository readReceiptRepository, RoomService roomService) {
        this.readReceiptRepository = readReceiptRepository;
        this.roomService = roomService;
    }

    /** Creates or updates the caller's read position for a room. One row per (room, user). */
    public ReadReceiptDto markRead(Long roomId, String username, Long lastReadMessageId) {
        ChatRoom room = roomService.getRoomOrThrow(roomId);
        ReadReceipt receipt = readReceiptRepository.findByRoomIdAndUsername(roomId, username)
                .orElseGet(() -> new ReadReceipt(room, username, lastReadMessageId));
        receipt.setRoom(room);
        receipt.setUsername(username);
        receipt.setLastReadMessageId(lastReadMessageId);
        receipt.setUpdatedAt(Instant.now());
        ReadReceipt saved = readReceiptRepository.save(receipt);
        return toDto(saved);
    }

    public List<ReadReceiptDto> getReceipts(Long roomId) {
        roomService.getRoomOrThrow(roomId);
        return readReceiptRepository.findByRoomId(roomId).stream()
                .map(this::toDto)
                .toList();
    }

    private ReadReceiptDto toDto(ReadReceipt r) {
        return new ReadReceiptDto(r.getUsername(), r.getLastReadMessageId(), r.getUpdatedAt());
    }
}