package ch.denic0la.controller;

import ch.denic0la.CurrentUserProvisioningService;
import ch.denic0la.model.AppUser;
import ch.denic0la.model.Gender;
import ch.denic0la.model.Room;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Comparator;
import java.util.List;

@Path("/api/rooms")
@Produces(MediaType.APPLICATION_JSON)
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
        return new RoomDto(
                room.id,
                room.camp != null ? room.camp.id : null,
                room.name,
                room.maxCapacity,
                room.gender,
                room.leaders.stream()
                        .sorted(Comparator.comparing((AppUser leader) -> leader.firstName == null ? "" : leader.firstName)
                                .thenComparing(leader -> leader.lastName == null ? "" : leader.lastName))
                        .map(leader -> new RoomLeaderDto(
                                leader.oidcSubject,
                                leader.firstName,
                                leader.lastName,
                                leader.pictureUrl))
                        .toList());
    }

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
