package com.example.chatapp.controller;

import com.example.chatapp.dto.ChatMessageDto;
import com.example.chatapp.model.ChatMessage;
import com.example.chatapp.service.ChatMessageService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatWebSocketController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(ChatMessageService chatMessageService,
                                    SimpMessagingTemplate messagingTemplate) {
        this.chatMessageService = chatMessageService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Client sends to: /app/chat.sendMessage/{roomId}
     *
     * `principal` is resolved by Spring from the STOMP session, which was
     * bound to a real identity during the handshake (see
     * CustomHandshakeHandler). We deliberately ignore any "sender" field the
     * client includes in the payload — trusting it would let anyone post
     * messages as anyone else.
     */
    @MessageMapping("/chat.sendMessage/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId, ChatMessageDto incoming, Principal principal) {
        incoming.setSender(principal.getName());
        incoming.setType(ChatMessage.MessageType.CHAT);
        ChatMessageDto saved = chatMessageService.saveMessage(roomId, incoming);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, saved);
    }

    /**
     * Client sends to: /app/chat.join/{roomId} when a user enters the room.
     * Same principle: the joining user's name comes from the verified
     * Principal, not from the message body.
     */
    @MessageMapping("/chat.join/{roomId}")
    public void joinRoom(@DestinationVariable Long roomId, ChatMessageDto incoming, Principal principal) {
        incoming.setSender(principal.getName());
        incoming.setType(ChatMessage.MessageType.JOIN);
        incoming.setContent(principal.getName() + " joined the room");
        ChatMessageDto saved = chatMessageService.saveMessage(roomId, incoming);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, saved);
    }
}
