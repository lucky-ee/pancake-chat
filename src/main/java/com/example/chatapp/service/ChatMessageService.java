package com.example.chatapp.service;

import com.example.chatapp.dto.ChatMessageDto;
import com.example.chatapp.model.ChatMessage;
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.repository.ChatMessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ChatMessageService {

    private final ChatMessageRepository messageRepository;
    private final RoomService roomService;

    public ChatMessageService(ChatMessageRepository messageRepository, RoomService roomService) {
        this.messageRepository = messageRepository;
        this.roomService = roomService;
    }

    public ChatMessageDto saveMessage(Long roomId, ChatMessageDto dto) {
        ChatRoom room = roomService.getRoomOrThrow(roomId);
        ChatMessage entity = new ChatMessage(room, dto.getSender(), dto.getContent(), dto.getType());
        ChatMessage saved = messageRepository.save(entity);
        return ChatMessageDto.fromEntity(saved);
    }

    /** Returns messages oldest-first for a given page (page 0 = most recent N messages). */
    public List<ChatMessageDto> getHistory(Long roomId, int page, int size) {
        roomService.getRoomOrThrow(roomId); // 404 if room missing
        Page<ChatMessage> result = messageRepository.findByRoomIdOrderByCreatedAtDesc(
                roomId, PageRequest.of(page, size));

        List<ChatMessageDto> dtos = new ArrayList<>(result.getContent().stream()
                .map(ChatMessageDto::fromEntity)
                .toList());
        // reverse so the caller gets chronological (oldest -> newest) order
        Collections.reverse(dtos);
        return dtos;
    }
}