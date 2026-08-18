package com.example.chatapp.repository;

import com.example.chatapp.model.ReadReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReadReceiptRepository extends JpaRepository<ReadReceipt, Long> {
    List<ReadReceipt> findByRoomId(Long roomId);
    Optional<ReadReceipt> findByRoomIdAndUsername(Long roomId, String username);
}