package ch.denic0la.controller;

import ch.denic0la.model.Camp;
import ch.denic0la.model.CampParticipant;
import ch.denic0la.model.CampParticipantMedication;
import ch.denic0la.model.Room;
import ch.denic0la.model.SignUp;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Path("/api/camps")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CampController {

    @Inject
    EntityManager entityManager;

    @GET
    @Transactional
    public List<CampDto> getAll() {
        return Camp.<Camp>listAll().stream()
                .sorted(Comparator
                        .comparing((Camp camp) -> camp.startDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(camp -> camp.id))
                .map(this::toDto)
                .toList();
    }

    @GET
    @Path("/{id}")
    @Transactional
    public CampDto getById(@PathParam("id") String id) {
        Camp camp = Camp.findById(id);
        if (camp == null) {
            throw new NotFoundException("Camp not found");
        }
        return toDto(camp);
    }

    @POST
    @Transactional
    @RolesAllowed("ADMIN")
    public CampDto create(CampInput input) {
        if (input == null || input.id() == null || input.id().isBlank()) {
            throw new BadRequestException("id is required");
        }
        String id = input.id().trim();
        if (Camp.findById(id) != null) {
            throw new BadRequestException("Camp already exists");
        }
        Camp camp = new Camp();
        camp.id = id;
        apply(camp, input);
        camp.persist();
        return toDto(camp);
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @RolesAllowed("ADMIN")
    public CampDto update(@PathParam("id") String id, CampInput input) {
        Camp camp = Camp.findById(id);
        if (camp == null) {
            throw new NotFoundException("Camp not found");
        }
        apply(camp, input);
        return toDto(camp);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @RolesAllowed("ADMIN")
    public void delete(@PathParam("id") String id, DeleteCampInput input) {
        Camp camp = Camp.findById(id);
        if (camp == null) {
            throw new NotFoundException("Camp not found");
        }
        if (camp.endDate == null || !camp.endDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Only ended camps can be deleted");
        }

        Map<SignUp.State, String> bulkFeedback = input == null || input.feedbackByState() == null
                ? Map.of()
                : input.feedbackByState();
        List<SignUp> signUps = SignUp.list("camp", camp);
        for (SignUp signUp : signUps) {
            signUp.archivedCampId = camp.id;
            signUp.archivedCampTitle = camp.title;
            signUp.archivedCampStartDate = camp.startDate;
            signUp.archivedCampEndDate = camp.endDate;
            String feedback = blankToNull(bulkFeedback.get(signUp.state));
            if (feedback != null) {
                signUp.feedback = feedback;
            }
            signUp.camp = null;
        }
        entityManager.flush();

        List<CampParticipant> campParticipants = CampParticipant.list("camp", camp);
        for (CampParticipant campParticipant : campParticipants) {
            CampParticipantMedication.delete("campParticipant", campParticipant);
            campParticipant.delete();
        }
        entityManager.createNativeQuery("""
                        DELETE FROM room_leader_assignment
                        WHERE room_id IN (SELECT id FROM room WHERE camp_id = ?1)
                        """)
                .setParameter(1, camp.id)
                .executeUpdate();
        List<Room> rooms = Room.list("camp", camp);
        for (Room room : rooms) {
            room.delete();
        }
        entityManager.flush();
        camp.delete();
    }

    private void apply(Camp camp, CampInput input) {
        if (input == null) {
            throw new BadRequestException("camp input is required");
        }
        camp.title = blankToNull(input.title());
        if (camp.title == null) {
            throw new BadRequestException("title is required");
        }
        camp.description = blankToNull(input.description());
        camp.startDate = input.startDate();
        camp.endDate = input.endDate();
        camp.signupEndDate = input.signupEndDate();
        camp.isJugendUndSport = input.isJugendUndSport();
        camp.priceFirst = input.priceFirst();
        camp.priceSecond = input.priceSecond();
        camp.priceThird = input.priceThird();
    }

    private CampDto toDto(Camp camp) {
        return new CampDto(
                camp.id,
                camp.title,
                camp.description,
                camp.startDate,
                camp.endDate,
                camp.signupEndDate,
                camp.isJugendUndSport,
                camp.priceFirst,
                camp.priceSecond,
                camp.priceThird);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record CampInput(
            String id,
            String title,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate signupEndDate,
            boolean isJugendUndSport,
            BigDecimal priceFirst,
            BigDecimal priceSecond,
            BigDecimal priceThird) {}

    public record DeleteCampInput(Map<SignUp.State, String> feedbackByState) {}

    public record CampDto(
            String id,
            String title,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate signupEndDate,
            boolean isJugendUndSport,
            BigDecimal priceFirst,
            BigDecimal priceSecond,
            BigDecimal priceThird) {}
}
