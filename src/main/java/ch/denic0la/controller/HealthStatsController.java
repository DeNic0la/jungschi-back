package ch.denic0la.controller;

import ch.denic0la.CurrentUserProvisioningService;
import ch.denic0la.model.AppUser;
import ch.denic0la.model.HealthStats;
import ch.denic0la.model.Participant;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/participants/{participantId}/health-stats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HealthStatsController {

    @Inject
    CurrentUserProvisioningService provisioningService;

    @GET
    public HealthStatsDto get(@PathParam("participantId") Long participantId) {
        AppUser user = provisioningService.ensureCurrentUser();
        Participant p = Participant.findById(participantId);
        if (p == null || !p.user.oidcSubject.equals(user.oidcSubject)) {
            throw new NotFoundException("Participant not found");
        }

        HealthStats stats = HealthStats.find("participant", p).firstResult();
        if (stats == null) {
            return null;
        }
        return toDto(stats);
    }

    @PUT
    @Transactional
    public HealthStatsDto update(@PathParam("participantId") Long participantId, HealthStatsDto dto) {
        AppUser user = provisioningService.ensureCurrentUser();
        Participant p = Participant.findById(participantId);
        if (p == null || !p.user.oidcSubject.equals(user.oidcSubject)) {
            throw new NotFoundException("Participant not found");
        }

        HealthStats stats = HealthStats.find("participant", p).firstResult();
        if (stats == null) {
            stats = new HealthStats();
            stats.participant = p;
        }
        stats.isHealthy = dto.isHealthy();
        stats.healthyReason = dto.healthyReason();
        stats.excludedActivities = dto.excludedActivities();
        stats.persist();
        return toDto(stats);
    }

    private HealthStatsDto toDto(HealthStats s) {
        return new HealthStatsDto(s.id, s.isHealthy, s.healthyReason, s.excludedActivities);
    }

    public record HealthStatsDto(Long id, boolean isHealthy, String healthyReason, String excludedActivities) {}
}
