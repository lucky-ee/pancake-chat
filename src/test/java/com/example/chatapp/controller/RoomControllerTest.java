package com.example.chatapp.controller;

import com.example.chatapp.exception.ApiExceptions.NotFoundException;
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.security.TokenStore;
import com.example.chatapp.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest loads only the web layer (no real DB). RoomService is mocked so
 * these tests prove the controller's HTTP contract, not persistence.
 *
 * TokenStore is also mocked: WebConfig registers TokenAuthInterceptor on
 * /api/rooms/**, and the web-layer test slice pulls that interceptor in
 * automatically (it's a HandlerInterceptor bean) but doesn't provide a real
 * TokenStore for it, so a mock is required just to let the context start —
 * which conveniently also lets each test control auth outcome directly.
 */
@WebMvcTest(RoomController.class)
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomService roomService;

    @MockBean
    private TokenStore tokenStore;

    @Test
    void createRoom_withValidToken_returnsCreatedRoom() throws Exception {
        when(tokenStore.validate("valid-token")).thenReturn(Optional.of("alice"));
        ChatRoom saved = new ChatRoom("General");
        saved.setId(1L);
        when(roomService.createRoom("General")).thenReturn(saved);

        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"General\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("General"));
    }

    @Test
    void getRoom_withValidToken_existingRoom_returnsRoomBody() throws Exception {
        when(tokenStore.validate("valid-token")).thenReturn(Optional.of("alice"));
        ChatRoom room = new ChatRoom("General");
        room.setId(42L);
        when(roomService.getRoomOrThrow(42L)).thenReturn(room);

        mockMvc.perform(get("/api/rooms/42")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.name").value("General"));
    }

    @Test
    void getRoom_withValidToken_nonexistentRoom_returnsNotFound() throws Exception {
        when(tokenStore.validate("valid-token")).thenReturn(Optional.of("alice"));
        when(roomService.getRoomOrThrow(999L)).thenThrow(new NotFoundException("Room 999 not found"));

        mockMvc.perform(get("/api/rooms/999")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Room 999 not found"));
    }

    @Test
    void listRooms_withoutAuthorizationHeader_isRejectedBeforeReachingTheController() throws Exception {
        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(roomService);
    }

    @Test
    void getRoom_withInvalidToken_isRejectedBeforeReachingTheController() throws Exception {
        when(tokenStore.validate("garbage")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/rooms/1")
                        .header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(roomService);
    }
}
