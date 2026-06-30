package com.example.eventboard.service;

import com.example.eventboard.exception.DuplicateRegistrationException;
import com.example.eventboard.exception.EventNotFoundException;
import com.example.eventboard.exception.NoSeatsAvailableException;
import com.example.eventboard.exception.ValidationException;
import com.example.eventboard.model.Event;
import com.example.eventboard.model.Participant;
import com.example.eventboard.repository.EventRepository;
import com.example.eventboard.repository.ParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

/**
 * Unit tests for the {@link EventService} class.
 * This class uses Mockito to isolate the service layer from the database layer,
 * ensuring that the business logic (validation, seat checking, duplicate checking)
 * works correctly under various scenarios.
 */
class EventServiceTest {
    private EventRepository eventRepository;
    private ParticipantRepository participantRepository;
    private EventService eventService;

    /**
     * Initializes the test environment before each test method runs.
     * Mocks the dependencies (repositories) and creates a fresh instance of the service.
     */
    @BeforeEach
    void setUp() {
        eventRepository = org.mockito.Mockito.mock(EventRepository.class);
        participantRepository = org.mockito.Mockito.mock(ParticipantRepository.class);
        eventService = new EventService(eventRepository, participantRepository);
    }

    /**
     * Verifies that when valid event data is provided, the service normalizes the title,
     * creates an event object, and successfully delegates saving to the repository.
     */
    @Test
    void createEventSavesValidEvent() {
        LocalDate date = LocalDate.now().plusDays(5);
        Event savedEvent = new Event(1L, "Java Pro", date, 20);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        when(eventRepository.save(captor.capture())).thenReturn(savedEvent);

        Event result = eventService.createEvent(" Java Pro ", date, 20);

        assertEquals(savedEvent, result);
        Event saved = captor.getValue();
        assertEquals("Java Pro", saved.getTitle());
        assertEquals(date, saved.getEventDate());
        assertEquals(20, saved.getMaxSeats());
    }

    /**
     * Verifies that attempting to create an event with a blank title throws a
     * {@link ValidationException} and prevents any interaction with the repository.
     */
    @Test
    void createEventRejectsBlankTitle() {
        assertThrows(ValidationException.class, () -> eventService.createEvent(" ", LocalDate.now().plusDays(1), 10));

        verify(eventRepository, never()).save(any(Event.class));
    }

    /**
     * Verifies that attempting to create an event with a date in the past throws a
     * {@link ValidationException} and prevents the event from being saved.
     */
    @Test
    void createEventRejectsPastDate() {
        assertThrows(ValidationException.class, () -> eventService.createEvent("Java Pro", LocalDate.now().minusDays(1), 10));

        verify(eventRepository, never()).save(any(Event.class));
    }

    /**
     * Verifies that attempting to create an event with zero or negative max seats throws a
     * {@link ValidationException} and prevents the event from being saved.
     */
    @Test
    void createEventRejectsInvalidMaxSeats() {
        assertThrows(ValidationException.class, () -> eventService.createEvent("Java Pro", LocalDate.now().plusDays(1), 0));

        verify(eventRepository, never()).save(any(Event.class));
    }

    /**
     * Verifies that a valid participant is successfully registered when the event exists,
     * the email is not a duplicate, and there is capacity. Also checks that the participant's
     * name is trimmed and email is normalized to lowercase.
     */
    @Test
    void registerParticipantSavesWhenSeatsAreAvailable() {
        long eventId = 1L;
        Event event = new Event(eventId, "Java Pro", LocalDate.now().plusDays(3), 2);
        Participant savedParticipant = new Participant(10L, eventId, "Alice", "alice@example.com");
        ArgumentCaptor<Participant> captor = ArgumentCaptor.forClass(Participant.class);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(participantRepository.existsByEventIdAndEmail(eventId, "alice@example.com")).thenReturn(false);
        when(participantRepository.countByEventId(eventId)).thenReturn(1);
        when(participantRepository.save(captor.capture())).thenReturn(savedParticipant);

        Participant result = eventService.registerParticipant(eventId, " Alice ", "ALICE@example.com");

        assertEquals(savedParticipant, result);
        Participant saved = captor.getValue();
        assertEquals("Alice", saved.getStudentName());   // whitespace trimmed
        assertEquals("alice@example.com", saved.getStudentEmail());  // lowercased
        assertEquals(eventId, saved.getEventId());
    }

    /**
     * Verifies that attempting to register a participant for a non-existent event ID throws an
     * {@link EventNotFoundException} and halts the registration process.
     */
    @Test
    void registerParticipantRejectsMissingEvent() {
        long eventId = 99L;

        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class, () -> eventService.registerParticipant(eventId, "Alice", "alice@example.com"));

        verify(participantRepository, never()).save(any(Participant.class));
    }

    /**
     * Verifies that attempting to register an email that is already registered for the specific event
     * throws a {@link DuplicateRegistrationException} and halts the registration process.
     */
    @Test
    void registerParticipantRejectsDuplicateEmail() {
        long eventId = 1L;
        Event event = new Event(eventId, "Java Pro", LocalDate.now().plusDays(3), 2);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(participantRepository.existsByEventIdAndEmail(eventId, "alice@example.com")).thenReturn(true);

        assertThrows(DuplicateRegistrationException.class, () -> eventService.registerParticipant(eventId, "Alice", "alice@example.com"));

        verify(participantRepository, never()).save(any(Participant.class));
    }

    /**
     * Verifies that attempting to register a participant when the event has reached its maximum
     * capacity throws a {@link NoSeatsAvailableException} and halts the registration process.
     */
    @Test
    void registerParticipantRejectsWhenNoSeatsAvailable() {
        long eventId = 1L;
        Event event = new Event(eventId, "Java Pro", LocalDate.now().plusDays(3), 2);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(participantRepository.existsByEventIdAndEmail(eventId, "alice@example.com")).thenReturn(false);
        when(participantRepository.countByEventId(eventId)).thenReturn(2);

        assertThrows(NoSeatsAvailableException.class, () -> eventService.registerParticipant(eventId, "Alice", "alice@example.com"));

        verify(participantRepository, never()).save(any(Participant.class));
    }

    /**
     * Verifies that providing an improperly formatted email address during registration throws a
     * {@link ValidationException} and halts the registration process.
     */
    @Test
    void registerParticipantRejectsInvalidEmail() {
        assertThrows(ValidationException.class, () -> eventService.registerParticipant(1L, "Alice", "invalid-email"));

        verify(participantRepository, never()).save(any(Participant.class));
    }

    /**
     * Verifies that providing a blank string for the student's name during registration throws a
     * {@link ValidationException} and halts the registration process.
     */
    @Test
    void registerParticipantRejectsBlankStudentName() {
        assertThrows(ValidationException.class, () -> eventService.registerParticipant(1L, " ", "alice@example.com"));

        verify(participantRepository, never()).save(any(Participant.class));
    }
}