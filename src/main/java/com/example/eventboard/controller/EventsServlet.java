package com.example.eventboard.controller;

import com.example.eventboard.config.ConnectionFactory;
import com.example.eventboard.exception.ValidationException;
import com.example.eventboard.repository.EventRepository;
import com.example.eventboard.repository.JdbcEventRepository;
import com.example.eventboard.repository.JdbcParticipantRepository;
import com.example.eventboard.repository.ParticipantRepository;
import com.example.eventboard.service.EventService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Servlet responsible for handling requests related to the general events listing
 * and the creation of new events.
 */
@WebServlet("/events")
public class EventsServlet extends HttpServlet {
    private static final String EVENTS_VIEW = "/WEB-INF/views/events.jsp";

    private EventService eventService;

    /**
     * Initializes the servlet by instantiating the necessary database connection factory,
     * repositories, and the event service required for processing event-related requests.
     */
    @Override
    public void init() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        EventRepository eventRepository = new JdbcEventRepository(connectionFactory);
        ParticipantRepository participantRepository = new JdbcParticipantRepository(connectionFactory);

        this.eventService = new EventService(eventRepository, participantRepository);
    }

    /**
     * Handles HTTP GET requests. Retrieves the list of upcoming events and displays
     * the events page.
     *
     * @param request  the {@link HttpServletRequest} object that contains the request the client made to the servlet.
     * @param response the {@link HttpServletResponse} object that contains the response the servlet returns to the client.
     * @throws ServletException if the request for the GET could not be handled.
     * @throws IOException      if an input or output error is detected when the servlet handles the GET request.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        showEventsPage(request, response);
    }

    /**
     * Handles HTTP POST requests for creating a new event.
     * Extracts the event details (title, date, max seats) from the request, attempts to create the event,
     * and handles any validation or parsing errors by returning to the events page with an error message
     * and the previously submitted data.
     *
     * @param request  the {@link HttpServletRequest} object containing the submitted form data.
     * @param response the {@link HttpServletResponse} object used to redirect or forward the response.
     * @throws ServletException if the request for the POST could not be handled.
     * @throws IOException      if an input or output error is detected when the servlet handles the POST request.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String title = request.getParameter("title");
        String eventDateValue = request.getParameter("eventDate");
        String maxSeatsValue = request.getParameter("maxSeats");

        try {
            LocalDate eventDate = parseEventDate(eventDateValue);
            int maxSeats = Integer.parseInt(maxSeatsValue);

            eventService.createEvent(title, eventDate, maxSeats);

            response.sendRedirect(request.getContextPath() + "/events");
        } catch (ValidationException | DateTimeParseException | NumberFormatException e) {
            request.setAttribute("errorMessage", "Please provide valid event data");
            request.setAttribute("formTitle", title);
            request.setAttribute("formEventDate", eventDateValue);
            request.setAttribute("formMaxSeats", maxSeatsValue);

            showEventsPage(request, response);
        }
    }

    /**
     * Retrieves the list of upcoming events from the service and forwards the request
     * to the JSP view responsible for rendering the events page.
     *
     * @param request  the {@link HttpServletRequest} object.
     * @param response the {@link HttpServletResponse} object.
     * @throws ServletException if a servlet-specific error occurs during forwarding.
     * @throws IOException      if an I/O error occurs during forwarding.
     */
    private void showEventsPage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("events", eventService.getUpcomingEvents());
        request.getRequestDispatcher(EVENTS_VIEW).forward(request, response);
    }

    /**
     * Parses the provided date string into a {@link LocalDate} object.
     *
     * @param eventDateValue the string representation of the date to parse.
     * @return the parsed {@link LocalDate} object.
     * @throws ValidationException if the provided date string is null or blank.
     * @throws DateTimeParseException if the text cannot be parsed to a date.
     */
    private LocalDate parseEventDate(String eventDateValue) {
        if (eventDateValue == null || eventDateValue.isBlank()) {
            throw new ValidationException("Event date is required");
        }

        return LocalDate.parse(eventDateValue);
    }
}