package ch.denic0la.team;

import ch.denic0la.model.Participant;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Path("/api/team/participants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("Jungschiteam")
public class TeamParticipantsController {

    @GET
    public List<TeamParticipantsController.ParticipantDto> getAll() {
        return Participant.listAll().stream()
                .map(p -> (Participant) p)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    @Transactional
    public DetailedParticipantDto getById(@PathParam("id") Long id) {
        Participant p = Participant.findById(id);
        if (p == null) {
            throw new NotFoundException("Participant not found");
        }
        return toDetailedDto(p);
    }

    private TeamParticipantsController.ParticipantDto toDto(Participant p) {
        return new TeamParticipantsController.ParticipantDto(p.id, p.firstname, p.lastname, p.dateOfBirth, p.lastUpdatedAt);
    }

    private DetailedParticipantDto toDetailedDto(Participant p) {
        AppUserDto userDto = p.user != null ? new AppUserDto(p.user.firstName, p.user.lastName, p.user.email) : null;
        HealthStatsDto healthStatsDto = p.healthStats != null ? new HealthStatsDto(p.healthStats.isHealthy, p.healthStats.healthyReason, p.healthStats.excludedActivities) : null;
        CampStatsDto campStatsDto = p.campStats != null ? new CampStatsDto(p.campStats.isTickVaccinated, p.campStats.drugConsent, p.campStats.ahv, p.campStats.krankenkasse, p.campStats.notes) : null;
        List<IntoleranceSelectionDto> intoleranceSelectionDtos = p.intoleranceSelections.stream()
                .map(i -> {
                    IntoleranceDto intoleranceDto = null;
                    if (i.intolerance != null) {
                        intoleranceDto = new IntoleranceDto(i.intolerance.id, i.intolerance.label, i.intolerance.definitionValue, i.intolerance.category != null ? i.intolerance.category.name() : null);
                    }
                    return new IntoleranceSelectionDto(
                            i.id,
                            intoleranceDto,
                            i.customText,
                            i.severity != null ? i.severity.name() : null);
                })
                .collect(Collectors.toList());

        return new DetailedParticipantDto(p.id, p.firstname, p.lastname, p.dateOfBirth, p.lastUpdatedAt, userDto, healthStatsDto, campStatsDto, intoleranceSelectionDtos);
    }

    public record ParticipantDto(Long id, String firstname, String lastname, LocalDate dateOfBirth, LocalDateTime lastUpdatedAt) {}

    public record DetailedParticipantDto(
            Long id,
            String firstname,
            String lastname,
            LocalDate dateOfBirth,
            LocalDateTime lastUpdatedAt,
            AppUserDto user,
            HealthStatsDto healthStats,
            CampStatsDto campStats,
            List<IntoleranceSelectionDto> intoleranceSelections
    ) {}

    public record AppUserDto(String firstName, String lastName, String email) {}

    public record HealthStatsDto(boolean isHealthy, String healthyReason, String excludedActivities) {}

    public record CampStatsDto(boolean isTickVaccinated, boolean drugConsent, String ahv, String krankenkasse, String notes) {}

    public record IntoleranceSelectionDto(Long id, IntoleranceDto intolerance, String customText, String severity) {}

    public record IntoleranceDto(Long id, String label, String definitionValue, String category) {}

}
