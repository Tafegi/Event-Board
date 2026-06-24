package com.example.eventboard.model;

import java.util.List;

public class EventDetails {
    private final Event event;
    private final List<Participant> participants;
    private final int availableSeats;

    public EventDetails(Event event, List<Participant> participants) {
        this.event = event;
        this.participants = List.copyOf(participants);
        this.availableSeats = event.getMaxSeats() - participants.size();
    }

    public Event getEvent() {
        return event;
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public boolean hasAvailableSeats() {
        return availableSeats > 0;
    }
}