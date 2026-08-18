package com.example.chatapp.controller;

import com.example.chatapp.dto.RoomDtos.CreateRoomRequest;
import com.example.chatapp.dto.RoomDtos.RoomResponse;
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        ChatRoom room = roomService.createRoom(request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(room));
    }

    @GetMapping
    public ResponseEntity<List<RoomResponse>> listRooms() {
        List<RoomResponse> rooms = roomService.listRooms().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable Long id) {
        return ResponseEntity.ok(toDto(roomService.getRoomOrThrow(id)));
    }

    private RoomResponse toDto(ChatRoom room) {
        return new RoomResponse(room.getId(), room.getName(), room.getCreatedAt());
    }
}
