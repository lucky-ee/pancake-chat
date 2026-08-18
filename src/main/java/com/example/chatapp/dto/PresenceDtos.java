package com.example.chatapp.dto;

import java.time.Instant;

public class PresenceDtos {

    /** Sent by a client over WebSocket to report how far they've read, and broadcast to the room. */
    public static class ReadReceiptDto {
        private String username;
        private Long lastReadMessageId;
        private Instant updatedAt;

        public ReadReceiptDto() {}

        public ReadReceiptDto(String username, Long lastReadMessageId, Instant updatedAt) {
            this.username = username;
            this.lastReadMessageId = lastReadMessageId;
            this.updatedAt = updatedAt;
        }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public Long getLastReadMessageId() { return lastReadMessageId; }
        public void setLastReadMessageId(Long lastReadMessageId) { this.lastReadMessageId = lastReadMessageId; }

        public Instant getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    }

    /** Ephemeral typing-state broadcast \u2014 never persisted, just relayed to the room. */
    public static class TypingEventDto {
        private String username;
        private boolean typing;

        public TypingEventDto() {}

        public TypingEventDto(String username, boolean typing) {
            this.username = username;
            this.typing = typing;
        }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public boolean isTyping() { return typing; }
        public void setTyping(boolean typing) { this.typing = typing; }
    }
}