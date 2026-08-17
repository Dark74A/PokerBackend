package com.example.backend.web;

import com.example.backend.aggregate.SessionAggregate;
import com.example.backend.controller.SessionController;
import com.example.backend.exception.*;
import com.example.backend.handlers.*;
import com.example.backend.helpers.CurrentUserProvider;
import com.example.backend.helpers.IdGenerator;
import com.example.backend.projections.ProjectionRebuilder;
import com.example.backend.repositories.HistoryEntryRepository;
import com.example.backend.repositories.SessionProjectionRepository;
import com.example.backend.repositories.SessionRepository;
import com.example.backend.repositories.UserRepository;
import com.example.backend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SessionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean private CurrentUserProvider currentUserProvider;
    @MockitoBean private IdGenerator idGenerator;
    @MockitoBean private CreateSessionHandler createSessionHandler;
    @MockitoBean private AddPlayerHandler addPlayerHandler;
    @MockitoBean private AddBuyInHandler addBuyInHandler;
    @MockitoBean private AddCashOutHandler addCashOutHandler;
    @MockitoBean private RemovePlayerHandler removePlayerHandler;
    @MockitoBean private CloseSessionHandler closeSessionHandler;
    @MockitoBean private ArchiveSessionHandler archiveSessionHandler;
    @MockitoBean private ReopenSessionHandler reopenSessionHandler;
    @MockitoBean private SessionProjectionRepository sessionProjectionRepository;
    @MockitoBean private SessionRepository sessionRepository;
    @MockitoBean private ProjectionRebuilder projectionRebuilder;
    @MockitoBean private RecordHandHandler recordHandHandler;
    @MockitoBean private HistoryEntryRepository historyEntryRepository;



    private static final String CURRENT_USER_ID = "host-user-1";

    private void mockAsCurrentUser(String userId) {
        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
    }

    @Test
    void createSessionWithValidRequestReturns201WithLocationHeader() throws Exception {
        mockAsCurrentUser(CURRENT_USER_ID);
        when(idGenerator.nextId()).thenReturn("session-abc");

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Friday Night","description":"casual game"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/sessions/session-abc"))
                .andExpect(jsonPath("$.sessionId").value("session-abc"));

        verify(createSessionHandler).handle(argThat(cmd ->
                cmd.sessionId().equals("session-abc") && cmd.hostId().equals(CURRENT_USER_ID)));
    }

    @Test
    void createSessionWithBlankNameReturns400() throws Exception {
        mockAsCurrentUser(CURRENT_USER_ID);

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","description":"casual game"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createSessionHandler);
    }

    @Test
    void getSessionForUnknownIdReturns404WithCleanErrorBody() throws Exception {
        when(sessionProjectionRepository.findById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/sessions/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }


    @Test
    void addCashOutExceedingChipStackReturns400() throws Exception {
        mockAsCurrentUser(CURRENT_USER_ID);
        when(addCashOutHandler.handle(any()))
                .thenThrow(new ValidationException("Cannot cash out more than the player's current chip stack."));

        mockMvc.perform(post("/api/sessions/session-abc/cashouts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerId":"player-1","amount":9999}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot cash out more than the player's current chip stack."));
    }

    @Test
    void addPlayerAsNonHostReturns403() throws Exception {
        mockAsCurrentUser("not-the-host");
        when(addPlayerHandler.handle(any()))
                .thenThrow(new UnauthorizedActionException("Only the host can manage this session."));

        mockMvc.perform(post("/api/sessions/session-abc/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Raj","linkedUserId":null}
                                """))
                .andExpect(status().isForbidden());
    }


    @Test
    void rebuildProjectionsAsNonHostReturns403AndDoesNotRunTheRebuild() throws Exception {
        mockAsCurrentUser("not-the-host");

        SessionAggregate aggregateWithDifferentHost = mock(SessionAggregate.class);
        when(aggregateWithDifferentHost.getId()).thenReturn("session-abc");
        when(aggregateWithDifferentHost.getHostId()).thenReturn("the-real-host");
        when(sessionRepository.load("session-abc")).thenReturn(aggregateWithDifferentHost);

        mockMvc.perform(post("/api/sessions/session-abc/rebuild-projections"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(projectionRebuilder);
    }

    @Test
    void rebuildProjectionsForNonexistentSessionReturns404() throws Exception {
        mockAsCurrentUser(CURRENT_USER_ID);

        SessionAggregate blankAggregate = mock(SessionAggregate.class);
        when(blankAggregate.getId()).thenReturn(null);
        when(sessionRepository.load("nonexistent")).thenReturn(blankAggregate);

        mockMvc.perform(post("/api/sessions/nonexistent/rebuild-projections"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(projectionRebuilder);
    }

    @Test
    void rebuildProjectionsAsHostReturns204AndTriggersTheRebuild() throws Exception {
        mockAsCurrentUser(CURRENT_USER_ID);

        SessionAggregate hostsAggregate = mock(SessionAggregate.class);
        when(hostsAggregate.getId()).thenReturn("session-abc");
        when(hostsAggregate.getHostId()).thenReturn(CURRENT_USER_ID);
        when(sessionRepository.load("session-abc")).thenReturn(hostsAggregate);

        mockMvc.perform(post("/api/sessions/session-abc/rebuild-projections"))
                .andExpect(status().isNoContent());

        verify(projectionRebuilder).rebuildForSession("session-abc");
    }
}
