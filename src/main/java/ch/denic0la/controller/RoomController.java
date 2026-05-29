package ch.denic0la.controller;

import ch.denic0la.CurrentUserProvisioningService;
import ch.denic0la.model.AppUser;
import ch.denic0la.model.Camp;
import ch.denic0la.model.CampParticipant;
import ch.denic0la.model.Gender;
import ch.denic0la.model.Room;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Path("/api/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class RoomController {

    @Inject
    CurrentUserProvisioningService provisioningService;

    @Inject
    EntityManager entityManager;

    @GET
    @Path("/{id}")
    @Transactional
    public RoomDto getById(@PathParam("id") Long id) {
        AppUser user = provisioningService.ensureCurrentUser();
        Room room = Room.findById(id);
        if (!provisioningService.canViewRoom(room, user)) {
            throw new NotFoundException("Room not found");
        }
        return toDto(room, assignedCount(room));
    }

    @GET
    @Path("/camp/{campId}")
    @Transactional
    @RolesAllowed({"Jungschiteam", "ADMIN"})
    public List<RoomDto> getForCamp(@PathParam("campId") String campId) {
        Camp camp = findCamp(campId);
        Map<Long, Long> assignedCounts = assignedCountsForCamp(camp);
        return Room.<Room>list("camp", camp).stream()
                .sorted(Comparator.comparing((Room room) -> room.name == null ? "" : room.name)
                        .thenComparing(room -> room.id))
                .map(room -> toDto(room, assignedCounts.getOrDefault(room.id, 0L)))
                .toList();
    }

    @GET
    @Path("/camp/{campId}/available-for/{campParticipantId}")
    @Transactional
    @RolesAllowed({"Jungschiteam", "ADMIN"})
    public List<RoomDto> getAvailableForCampParticipant(
            @PathParam("campId") String campId,
            @PathParam("campParticipantId") Long campParticipantId) {
        Camp camp = findCamp(campId);
        CampParticipant campParticipant = CampParticipant.findById(campParticipantId);
        if (campParticipant == null
                || campParticipant.camp == null
                || !Objects.equals(campParticipant.camp.id, camp.id)) {
            throw new NotFoundException("Camp participant not found");
        }
        Map<Long, Long> assignedCounts = assignedCountsForCamp(camp);
        return Room.<Room>list("camp", camp).stream()
                .filter(room -> isAssignable(room, campParticipant, assignedCounts))
                .sorted(Comparator.comparing((Room room) -> room.name == null ? "" : room.name)
                        .thenComparing(room -> room.id))
                .map(room -> toDto(room, assignedCounts.getOrDefault(room.id, 0L)))
                .toList();
    }

    @POST
    @Transactional
    @RolesAllowed("ADMIN")
    public RoomDto create(RoomInput input) {
        Room room = new Room();
        apply(room, input);
        room.persist();
        return toDto(room, 0L);
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @RolesAllowed("ADMIN")
    public RoomDto update(@PathParam("id") Long id, RoomInput input) {
        Room room = Room.findById(id);
        if (room == null) {
            throw new NotFoundException("Room not found");
        }
        apply(room, input);
        return toDto(room, assignedCount(room));
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @RolesAllowed("ADMIN")
    public void delete(@PathParam("id") Long id) {
        Room room = Room.findById(id);
        if (room == null) {
            throw new NotFoundException("Room not found");
        }
        if (CampParticipant.count("room", room) > 0) {
            throw new BadRequestException("Room still has assigned camp participants");
        }
        room.delete();
    }

    private void apply(Room room, RoomInput input) {
        if (input == null || input.campId() == null || input.campId().isBlank()) {
            throw new BadRequestException("campId is required");
        }
        room.camp = findCamp(input.campId());
        room.name = blankToNull(input.name());
        if (room.name == null) {
            throw new BadRequestException("name is required");
        }
        room.maxCapacity = input.maxCapacity();
        if (room.maxCapacity != null && room.maxCapacity < 1) {
            throw new BadRequestException("maxCapacity must be positive");
        }
        room.gender = input.gender();
        room.leaders.clear();
        if (input.leaderEmails() != null) {
            for (String rawEmail : input.leaderEmails()) {
                String email = provisioningService.normalizeEmail(rawEmail);
                if (email == null || email.isBlank()) {
                    continue;
                }
                AppUser leader = AppUser.findById(email);
                if (leader == null || !leader.hasRole("Jungschiteam")) {
                    throw new BadRequestException("Leader must be a Jungschiteam user");
                }
                if (room.leaders.stream().noneMatch(existing -> Objects.equals(existing.email, leader.email))) {
                    room.leaders.add(leader);
                }
            }
        }
    }

    private Camp findCamp(String campId) {
        Camp camp = Camp.findById(campId);
        if (camp == null) {
            throw new NotFoundException("Camp not found");
        }
        return camp;
    }

    private RoomDto toDto(Room room, long assignedCount) {
        Integer remainingCapacity = room.maxCapacity == null
                ? null
                : Math.max(0, room.maxCapacity - Math.toIntExact(assignedCount));
        return new RoomDto(
                room.id,
                room.camp != null ? room.camp.id : null,
                room.name,
                room.maxCapacity,
                Math.toIntExact(assignedCount),
                remainingCapacity,
                room.gender,
                room.leaders.stream()
                        .sorted(Comparator.comparing((AppUser leader) -> leader.firstName == null ? "" : leader.firstName)
                                .thenComparing(leader -> leader.lastName == null ? "" : leader.lastName)
                                .thenComparing(leader -> leader.email))
                        .map(leader -> new RoomLeaderDto(
                                leader.email,
                                leader.firstName,
                                leader.lastName,
                                leader.pictureUrl))
                        .toList());
    }

    private long assignedCount(Room room) {
        return room == null ? 0 : CampParticipant.count("room", room);
    }

    private Map<Long, Long> assignedCountsForCamp(Camp camp) {
        List<Object[]> rows = entityManager
                .createQuery("""
                        select cp.room.id, count(cp)
                        from CampParticipant cp
                        where cp.room is not null and cp.room.camp = :camp
                        group by cp.room.id
                        """, Object[].class)
                .setParameter("camp", camp)
                .getResultList();
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private boolean isAssignable(
            Room room,
            CampParticipant campParticipant,
            Map<Long, Long> assignedCounts) {
        if (room == null || campParticipant == null || campParticipant.participant == null) {
            return false;
        }
        boolean currentlyAssigned = campParticipant.room != null
                && Objects.equals(campParticipant.room.id, room.id);
        if (room.gender != null && !room.gender.equals(campParticipant.participant.gender)) {
            return currentlyAssigned;
        }
        if (room.maxCapacity == null) {
            return true;
        }
        long assignedCount = assignedCounts.getOrDefault(room.id, 0L);
        return assignedCount < room.maxCapacity || currentlyAssigned;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record RoomInput(
            String campId,
            String name,
            Integer maxCapacity,
            Gender gender,
            List<String> leaderEmails) {}

    public record RoomDto(
            Long id,
            String campId,
            String name,
            Integer maxCapacity,
            int assignedCount,
            Integer remainingCapacity,
            Gender gender,
            List<RoomLeaderDto> leaders) {}

    public record RoomLeaderDto(
            String id,
            String firstName,
            String lastName,
            String pictureUrl) {}
}
