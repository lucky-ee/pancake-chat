package com.example.chatapp.dto;

import com.example.chatapp.model.ChatMessage;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public class ChatMessageDto {

    private Long id;

    @NotBlank
    private String sender;

    @NotBlank
    private String content;

    private ChatMessage.MessageType type = ChatMessage.MessageType.CHAT;

    private Instant createdAt;

    public ChatMessageDto() {}

    public ChatMessageDto(Long id, String sender, String content, ChatMessage.MessageType type, Instant createdAt) {
        this.id = id;
        this.sender = sender;
        this.content = content;
        this.type = type;
        this.createdAt = createdAt;
    }

    public static ChatMessageDto fromEntity(ChatMessage m) {
        return new ChatMessageDto(m.getId(), m.getSender(), m.getContent(), m.getType(), m.getCreatedAt());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public ChatMessage.MessageType getType() { return type; }
    public void setType(ChatMessage.MessageType type) { this.type = type; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
