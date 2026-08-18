package com.example.chatapp.controller;

import com.example.chatapp.dto.ChatMessageDto;
import com.example.chatapp.dto.PresenceDtos.ReadReceiptDto;
import com.example.chatapp.dto.PresenceDtos.TypingEventDto;
import com.example.chatapp.model.ChatMessage;
import com.example.chatapp.service.ChatMessageService;
import com.example.chatapp.service.ReadReceiptService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatWebSocketController {

    private final ChatMessageService chatMessageService;
    private final ReadReceiptService readReceiptService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(ChatMessageService chatMessageService,
                                    ReadReceiptService readReceiptService,
                                    SimpMessagingTemplate messagingTemplate) {
        this.chatMessageService = chatMessageService;
        this.readReceiptService = readReceiptService;
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

    /**
     * Client sends to: /app/chat.typing/{roomId} with { "typing": true|false }
     * whenever the user starts/stops typing. Purely ephemeral — never
     * touches the database, just relayed live to everyone else in the room
     * on a separate topic so it can't be confused with real messages.
     * As with sendMessage, the username comes from the verified Principal.
     */
    @MessageMapping("/chat.typing/{roomId}")
    public void typing(@DestinationVariable Long roomId, TypingEventDto incoming, Principal principal) {
        TypingEventDto event = new TypingEventDto(principal.getName(), incoming.isTyping());
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/typing", event);
    }

    /**
     * Client sends to: /app/chat.read/{roomId} with { "lastReadMessageId": <id> }
     * whenever the user views the latest message in a room. Persisted (one
     * row per room+user, see ReadReceipt) so it survives reconnects, then
     * broadcast to the room so other clients can show a "seen" marker.
     */
    @MessageMapping("/chat.read/{roomId}")
    public void markRead(@DestinationVariable Long roomId, ReadReceiptDto incoming, Principal principal) {
        ReadReceiptDto saved = readReceiptService.markRead(roomId, principal.getName(), incoming.getLastReadMessageId());
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/receipts", saved);
    }
}