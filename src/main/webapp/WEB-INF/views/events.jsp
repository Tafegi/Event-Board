<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Event Board</title>
</head>
<body>
<main>
    <h1>Event Board</h1>

    <section>
        <h2>Create Event</h2>

        <c:if test="${not empty errorMessage}">
            <p role="alert">${errorMessage}</p>
        </c:if>

        <form method="post" action="${pageContext.request.contextPath}/events">
            <div>
                <label for="title">Title</label>
                <input
                        type="text"
                        id="title"
                        name="title"
                        value="${formTitle}"
                        required>
            </div>

            <div>
                <label for="eventDate">Event date</label>
                <input
                        type="date"
                        id="eventDate"
                        name="eventDate"
                        value="${formEventDate}"
                        required>
            </div>

            <div>
                <label for="maxSeats">Max seats</label>
                <input
                        type="number"
                        id="maxSeats"
                        name="maxSeats"
                        min="1"
                        value="${formMaxSeats}"
                        required>
            </div>

            <button type="submit">Create event</button>
        </form>
    </section>

    <section>
        <h2>Upcoming Events</h2>

        <c:choose>
            <c:when test="${empty events}">
                <p>No upcoming events yet.</p>
            </c:when>
            <c:otherwise>
                <table>
                    <thead>
                    <tr>
                        <th>Title</th>
                        <th>Date</th>
                        <th>Max seats</th>
                        <th>Registered</th>
                        <th>Available</th>
                        <th>Details</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="event" items="${events}">
                        <tr>
                            <td>${event.title}</td>
                            <td>${event.eventDate}</td>
                            <td>${event.maxSeats}</td>
                            <td>${event.registeredCount}</td>
                            <td>${event.availableSeats}</td>
                            <td>
                                <a href="${pageContext.request.contextPath}/event?id=${event.id}">
                                    Open
                                </a>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </section>
</main>
</body>
</html>