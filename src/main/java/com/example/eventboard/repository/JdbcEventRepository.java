package com.example.eventboard.repository;

import com.example.eventboard.config.ConnectionFactory;
import com.example.eventboard.model.Event;
import com.example.eventboard.model.EventSummary;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of the {@link EventRepository}.
 * Provides methods to interact with the underlying relational database for managing {@link Event} entities.
 */
public class JdbcEventRepository implements EventRepository {
    private static final String FIND_UPCOMING_EVENTS_SQL = """
            SELECT
                e.id,
                e.title,
                e.event_date,
                e.max_seats,
                COUNT(p.id) AS registered_count
            FROM events e
            LEFT JOIN participants p ON p.event_id = e.id
            WHERE e.event_date >= CURRENT_DATE
            GROUP BY e.id, e.title, e.event_date, e.max_seats
            ORDER BY e.event_date ASC
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT id, title, event_date, max_seats
            FROM events
            WHERE id = ?
            """;

    private static final String SAVE_SQL = """
            INSERT INTO events (title, event_date, max_seats)
            VALUES (?, ?, ?)
            """;

    private final ConnectionFactory connectionFactory;

    /**
     * Constructs a new JDBC event repository.
     *
     * @param connectionFactory the factory used to obtain database connections.
     */
    public JdbcEventRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Validates the properties of an event before attempting to save it to the database.
     *
     * @param event the {@link Event} to validate.
     * @throws IllegalStateException if the event is null, or if its title, date, or max seats are invalid.
     */
    private void validateEventForSave(Event event) {
        if (event == null) {
            throw new IllegalStateException("Event must not be null");
        }

        if (event.getTitle() == null || event.getTitle().isBlank()) {
            throw new IllegalStateException("Event title must not be blank");
        }

        if (event.getEventDate() == null) {
            throw new IllegalStateException("Event date must not be null");
        }

        if (event.getMaxSeats() <= 0) {
            throw new IllegalStateException("Event max seats must be greater than zero");
        }
    }

    /**
     * Retrieves a list of all upcoming events (events scheduled for today or in the future).
     * Each summary includes the basic event details and the current number of registered participants.
     *
     * @return a {@link List} of {@link EventSummary} objects representing upcoming events, ordered by date ascending.
     * @throws IllegalStateException if a database access error occurs.
     */
    @Override
    public List<EventSummary> findUpcomingEvents() {
        List<EventSummary> events = new ArrayList<>();

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_UPCOMING_EVENTS_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                events.add(mapEventSummary(resultSet));
            }

            return events;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find upcoming events", e);
        }
    }

    /**
     * Retrieves an event by its unique identifier.
     *
     * @param id the unique identifier of the event to find.
     * @return an {@link Optional} containing the found {@link Event}, or an empty {@link Optional} if no event was found.
     * @throws IllegalStateException if a database access error occurs.
     */
    @Override
    public Optional<Event> findById(long id) {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapEvent(resultSet));
                }

                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find event by id: " + id, e);
        }
    }

    /**
     * Saves a new event to the database.
     * Validates the event data prior to insertion and populates the returned event object with the auto-generated database ID.
     *
     * @param event the {@link Event} object to be saved.
     * @return the saved {@link Event} object, updated with its generated database ID.
     * @throws IllegalStateException if validation fails, the generated ID is not returned, or a database access error occurs.
     */
    @Override
    public Event save(Event event) {
        validateEventForSave(event);

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(SAVE_SQL, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, event.getTitle());
            statement.setDate(2, Date.valueOf(event.getEventDate()));
            statement.setInt(3, event.getMaxSeats());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    event.setId(generatedKeys.getLong(1));
                    return event;
                }

                throw new IllegalStateException("Failed to save event: generated id was not returned");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save event", e);
        }
    }

    /**
     * Maps a single row from the provided {@link ResultSet} to an {@link Event} object.
     *
     * @param resultSet the result set containing the database row.
     * @return a mapped {@link Event} object.
     * @throws SQLException if a database access error occurs or the column labels are not found.
     */
    private Event mapEvent(ResultSet resultSet) throws SQLException {
        return new Event(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getObject("event_date", LocalDate.class),
                resultSet.getInt("max_seats")
        );
    }

    /**
     * Maps a single row from the provided {@link ResultSet} to an {@link EventSummary} object.
     *
     * @param resultSet the result set containing the database row.
     * @return a mapped {@link EventSummary} object.
     * @throws SQLException if a database access error occurs or the column labels are not found.
     */
    private EventSummary mapEventSummary(ResultSet resultSet) throws SQLException {
        return new EventSummary(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getObject("event_date", LocalDate.class),
                resultSet.getInt("max_seats"),
                resultSet.getInt("registered_count")
        );
    }
}