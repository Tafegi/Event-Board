package com.example.eventboard.repository;

import com.example.eventboard.model.Event;
import com.example.eventboard.model.EventSummary;

import java.util.List;
import java.util.Optional;

public interface EventRepository {
    List<EventSummary> findUpcomingEvents();

    Optional<Event> findById(long id);

    Event save(Event event);
}