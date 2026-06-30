package com.example.eventboard.service;

import com.example.eventboard.exception.DuplicateRegistrationException;
import com.example.eventboard.exception.EventNotFoundException;
import com.example.eventboard.exception.NoSeatsAvailableException;
import com.example.eventboard.exception.ValidationException;
import com.example.eventboard.model.Event;
import com.example.eventboard.model.EventDetails;
import com.example.eventboard.model.EventSummary;
import com.example.eventboard.model.Participant;
import com.example.eventboard.repository.EventRepository;
import com.example.eventboard.repository.ParticipantRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.Locale;

/**
 * Service class responsible for handling business logic related to events
 * and participant registrations.
 */
public class EventService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;

    /**
     * Constructs a new {@code EventService} with the specified repositories.
     *
     * @param eventRepository       the repository used for event data access.
     * @param participantRepository the repository used for participant data access.
     * @throws NullPointerException if either repository is null.
     */
    public EventService(EventRepository eventRepository, ParticipantRepository participantRepository) {
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository must not be null");
        this.participantRepository = Objects.requireNonNull(participantRepository, "participantRepository must not be null");
    }

    /**
     * Retrieves a list of all upcoming events.
     *
     * @return a {@link List} of {@link EventSummary} representing future or current events.
     */
    public List<EventSummary> getUpcomingEvents() {
        return eventRepository.findUpcomingEvents();
    }

    /**
     * Retrieves comprehensive details for a specific event, including its participants.
     *
     * @param eventId the unique identifier of the event.
     * @return an {@link EventDetails} object containing the event and its participants.
     * @throws ValidationException    if the event ID is invalid (less than or equal to zero).
     * @throws EventNotFoundException if no event exists with the given ID.
     */
    public EventDetails getEventDetails(long eventId) {
        validateEventId(eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        List<Participant> participants = participantRepository.findByEventId(eventId);

        return new EventDetails(event, participants);
    }

    /**
     * Validates input data and creates a new event.
     *
     * @param title     the title of the event.
     * @param eventDate the scheduled date of the event.
     * @param maxSeats  the maximum number of seats available.
     * @return the created {@link Event} with its generated ID.
     * @throws ValidationException if the title is blank, date is null or in the past, or maxSeats is invalid.
     */
    public Event createEvent(String title, LocalDate eventDate, int maxSeats) {
        String normalizedTitle = validateTitle(title);
        validateEventDate(eventDate);
        validateMaxSeats(maxSeats);

        Event event = new Event(normalizedTitle, eventDate, maxSeats);

        return eventRepository.save(event);
    }

    /**
     * Registers a new participant for a specific event.
     * Validates the participant's data, checks for seat availability, and ensures no duplicate registrations.
     *
     * @param eventId      the unique identifier of the event.
     * @param studentName  the name of the student registering.
     * @param studentEmail the email of the student registering.
     * @return the saved {@link Participant} object.
     * @throws ValidationException            if the input data is invalid (e.g., malformed email, blank name).
     * @throws EventNotFoundException         if the event does not exist.
     * @throws DuplicateRegistrationException if a participant with the same email is already registered for this event.
     * @throws NoSeatsAvailableException      if the event has reached its maximum capacity.
     */
    public Participant registerParticipant(long eventId, String studentName, String studentEmail) {
        validateEventId(eventId);

        String normalizedName = validateStudentName(studentName);
        String normalizedEmail = validateStudentEmail(studentEmail);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        if (participantRepository.existsByEventIdAndEmail(eventId, normalizedEmail)) {
            throw new DuplicateRegistrationException(normalizedEmail);
        }

        int registeredCount = participantRepository.countByEventId(eventId);

        if (registeredCount >= event.getMaxSeats()) {
            throw new NoSeatsAvailableException(eventId);
        }

        Participant participant = new Participant(eventId, normalizedName, normalizedEmail);

        return participantRepository.save(participant);
    }

    /**
     * Validates the event ID.
     *
     * @param eventId the ID to validate.
     * @throws ValidationException if the ID is less than or equal to zero.
     */
    private void validateEventId(long eventId) {
        if (eventId <= 0) {
            throw new ValidationException("Event id must be greater than zero");
        }
    }

    /**
     * Validates and trims the event title.
     *
     * @param title the title to validate.
     * @return the trimmed title.
     * @throws ValidationException if the title is null or blank.
     */
    private String validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Event title must not be blank");
        }

        return title.trim();
    }

    /**
     * Validates the event date.
     *
     * @param eventDate the date to validate.
     * @throws ValidationException if the date is null or in the past.
     */
    private void validateEventDate(LocalDate eventDate) {
        if (eventDate == null) {
            throw new ValidationException("Event date must not be null");
        }

        if (eventDate.isBefore(LocalDate.now())) {
            throw new ValidationException("Event date must not be in the past");
        }
    }

    /**
     * Validates the maximum number of seats.
     *
     * @param maxSeats the seat count to validate.
     * @throws ValidationException if maxSeats is less than or equal to zero.
     */
    private void validateMaxSeats(int maxSeats) {
        if (maxSeats <= 0) {
            throw new ValidationException("Max seats must be greater than zero");
        }
    }

    /**
     * Validates and trims the student's name.
     *
     * @param studentName the name to validate.
     * @return the trimmed name.
     * @throws ValidationException if the name is null or blank.
     */
    private String validateStudentName(String studentName) {
        if (studentName == null || studentName.isBlank()) {
            throw new ValidationException("Student name must not be blank");
        }

        return studentName.trim();
    }

    /**
     * Validates, trims, and normalizes the student's email.
     *
     * @param studentEmail the email to validate.
     * @return the normalized (lowercase and trimmed) email.
     * @throws ValidationException if the email is null, blank, or does not match a standard email format.
     */
    private String validateStudentEmail(String studentEmail) {
        if (studentEmail == null || studentEmail.isBlank()) {
            throw new ValidationException("Student email must not be blank");
        }

        String normalizedEmail = studentEmail.trim().toLowerCase(Locale.ROOT);

        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new ValidationException("Student email has invalid format");
        }

        return normalizedEmail;
    }
}