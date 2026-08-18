package com.example.chatapp.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "read_receipts", uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "username"}))
public class ReadReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private Long lastReadMessageId;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public ReadReceipt() {}

    public ReadReceipt(ChatRoom room, String username, Long lastReadMessageId) {
        this.room = room;
        this.username = username;
        this.lastReadMessageId = lastReadMessageId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ChatRoom getRoom() { return room; }
    public void setRoom(ChatRoom room) { this.room = room; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Long getLastReadMessageId() { return lastReadMessageId; }
    public void setLastReadMessageId(Long lastReadMessageId) { this.lastReadMessageId = lastReadMessageId; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}