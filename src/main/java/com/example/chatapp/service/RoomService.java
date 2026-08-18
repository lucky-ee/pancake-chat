package com.example.chatapp.service;

import com.example.chatapp.exception.ApiExceptions.ConflictException;
import com.example.chatapp.exception.ApiExceptions.NotFoundException;
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.repository.ChatRoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomService {

    private final ChatRoomRepository roomRepository;

    public RoomService(ChatRoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public ChatRoom createRoom(String name) {
        if (roomRepository.existsByName(name)) {
            throw new ConflictException("Room '" + name + "' already exists");
        }
        return roomRepository.save(new ChatRoom(name));
    }

    public List<ChatRoom> listRooms() {
        return roomRepository.findAll();
    }

    public ChatRoom getRoomOrThrow(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Room " + id + " not found"));
    }
}
