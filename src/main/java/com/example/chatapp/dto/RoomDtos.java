package com.example.chatapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public class RoomDtos {

    public static class CreateRoomRequest {
        @NotBlank
        @Size(min = 2, max = 100)
        private String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class RoomResponse {
        private Long id;
        private String name;
        private Instant createdAt;

        public RoomResponse(Long id, String name, Instant createdAt) {
            this.id = id;
            this.name = name;
            this.createdAt = createdAt;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public Instant getCreatedAt() { return createdAt; }
    }
}
