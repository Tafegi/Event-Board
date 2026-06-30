package com.example.eventboard.controller;

import com.example.eventboard.config.ConnectionFactory;
import com.example.eventboard.exception.DuplicateRegistrationException;
import com.example.eventboard.exception.EventNotFoundException;
import com.example.eventboard.exception.NoSeatsAvailableException;
import com.example.eventboard.exception.ValidationException;
import com.example.eventboard.model.EventDetails;
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

/**
 * Servlet responsible for handling requests related to specific events.
 * It supports viewing event details and registering new participants.
 */
@WebServlet("/event")
public class EventServlet extends HttpServlet {
    private static final String EVENT_VIEW = "/WEB-INF/views/event.jsp";

    private EventService eventService;

    /**
     * Initializes the servlet by instantiating the connection factory,
     * repositories, and the event service required for processing requests.
     */
    @Override
    public void init() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        EventRepository eventRepository = new JdbcEventRepository(connectionFactory);
        ParticipantRepository participantRepository = new JdbcParticipantRepository(connectionFactory);

        this.eventService = new EventService(eventRepository, participantRepository);
    }

    /**
     * Handles HTTP GET requests. Parses the event ID from the request
     * and displays the corresponding event details page.
     *
     * @param request  the {@link HttpServletRequest} object that contains the request the client made to the servlet.
     * @param response the {@link HttpServletResponse} object that contains the response the servlet returns to the client.
     * @throws ServletException if the request for the GET could not be handled.
     * @throws IOException      if an input or output error is detected when the servlet handles the GET request.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long eventId = parseEventId(request, response);

        if (eventId <= 0) {
            return;
        }

        showEventPage(eventId, request, response);
    }

    /**
     * Handles HTTP POST requests for participant registration.
     * Extracts the event ID and participant details, attempts to register the participant,
     * and handles validation, capacity, or duplication errors by returning to the event page with an error message.
     *
     * @param request  the {@link HttpServletRequest} object that contains the request the client made to the servlet.
     * @param response the {@link HttpServletResponse} object that contains the response the servlet returns to the client.
     * @throws ServletException if the request for the POST could not be handled.
     * @throws IOException      if an input or output error is detected when the servlet handles the POST request.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long eventId = parseEventId(request, response);

        if (eventId <= 0) {
            return;
        }

        String studentName = request.getParameter("studentName");
        String studentEmail = request.getParameter("studentEmail");

        try {
            eventService.registerParticipant(eventId, studentName, studentEmail);

            response.sendRedirect(request.getContextPath() + "/event?id=" + eventId);
        } catch (ValidationException e) {
            request.setAttribute("errorMessage", "Please provide valid registration data");
            request.setAttribute("studentName", studentName);
            request.setAttribute("studentEmail", studentEmail);

            showEventPage(eventId, request, response);
        } catch (NoSeatsAvailableException e) {
            request.setAttribute("errorMessage", "No seats available for this event");

            showEventPage(eventId, request, response);
        } catch (DuplicateRegistrationException e) {
            request.setAttribute("errorMessage", "This email is already registered for this event");
            request.setAttribute("studentName", studentName);
            request.setAttribute("studentEmail", studentEmail);

            showEventPage(eventId, request, response);
        } catch (EventNotFoundException e) {
            showEventPage(eventId, request, response);
        }
    }

    /**
     * Parses and validates the event ID from the request parameters.
     *
     * @param request  the {@link HttpServletRequest} containing the "id" parameter.
     * @param response the {@link HttpServletResponse} used to send an error if validation fails.
     * @return the parsed event ID if valid, or -1 if the ID is missing, non-positive, or not a number.
     * @throws IOException if an error occurs while sending the error response.
     */
    private long parseEventId(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String idValue = request.getParameter("id");

        if (idValue == null || idValue.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Event id is required");
            return -1;
        }

        try {
            long eventId = Long.parseLong(idValue);

            if (eventId <= 0) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Event id must be greater than zero");
                return -1;
            }

            return eventId;
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Event id must be a number");
            return -1;
        }
    }

    /**
     * Retrieves event details and forwards the request to the event view page.
     * Sends a 404 Not Found error if the event does not exist.
     *
     * @param eventId  the ID of the event to display.
     * @param request  the {@link HttpServletRequest} object.
     * @param response the {@link HttpServletResponse} object.
     * @throws ServletException if a servlet-specific error occurs during forwarding.
     * @throws IOException      if an I/O error occurs during forwarding or sending the error response.
     */
    private void showEventPage(long eventId, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            EventDetails eventDetails = eventService.getEventDetails(eventId);

            request.setAttribute("eventDetails", eventDetails);
            request.getRequestDispatcher(EVENT_VIEW).forward(request, response);
        } catch (EventNotFoundException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Event not found");
        }
    }
}