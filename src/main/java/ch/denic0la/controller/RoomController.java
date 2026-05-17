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
import java.util.List;
import java.util.Objects;

@Path("/api/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class RoomController {

    @Inject
    CurrentUserProvisioningService provisioningService;

    @GET
    @Path("/{id}")
    @Transactional
    public RoomDto getById(@PathParam("id") Long id) {
        AppUser user = provisioningService.ensureCurrentUser();
        Room room = Room.findById(id);
        if (!provisioningService.canViewRoom(room, user)) {
            throw new NotFoundException("Room not found");
        }
        return toDto(room);
    }

    @GET
    @Path("/camp/{campId}")
    @Transactional
    @RolesAllowed({"Jungschiteam", "ADMIN"})
    public List<RoomDto> getForCamp(@PathParam("campId") String campId) {
        Camp camp = findCamp(campId);
        return Room.<Room>list("camp", camp).stream()
                .sorted(Comparator.comparing((Room room) -> room.name == null ? "" : room.name)
                        .thenComparing(room -> room.id))
                .map(this::toDto)
                .toList();
    }

    @POST
    @Transactional
    @RolesAllowed("ADMIN")
    public RoomDto create(RoomInput input) {
        Room room = new Room();
        apply(room, input);
        room.persist();
        return toDto(room);
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
        return toDto(room);
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

    private RoomDto toDto(Room room) {
        return new RoomDto(
                room.id,
                room.camp != null ? room.camp.id : null,
                room.name,
                room.maxCapacity,
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
            Gender gender,
            List<RoomLeaderDto> leaders) {}

    public record RoomLeaderDto(
            String id,
            String firstName,
            String lastName,
            String pictureUrl) {}
}
