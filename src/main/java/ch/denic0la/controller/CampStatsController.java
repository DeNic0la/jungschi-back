package ch.denic0la.controller;

import ch.denic0la.CurrentUserProvisioningService;
import ch.denic0la.model.AppUser;
import ch.denic0la.model.CampStats;
import ch.denic0la.model.Participant;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/participants/{participantId}/camp-stats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class CampStatsController {

    @Inject
    CurrentUserProvisioningService provisioningService;

    @GET
    public CampStatsDto get(@PathParam("participantId") Long participantId) {
        AppUser user = provisioningService.getCurrentUser();
        Participant p = Participant.findById(participantId);
        if (!provisioningService.canReadParticipant(p, user)) {
            throw new NotFoundException("Participant not found");
        }

        CampStats stats = CampStats.find("participant", p).firstResult();
        if (stats == null) {
            return null;
        }
        return toDto(stats);
    }

    @PUT
    @Transactional
    public CampStatsDto update(@PathParam("participantId") Long participantId, CampStatsDto dto) {
        AppUser user = provisioningService.getCurrentUser();
        Participant p = Participant.findById(participantId);
        if (!provisioningService.canWriteParticipant(p, user)) {
            throw new NotFoundException("Participant not found");
        }

        CampStats stats = CampStats.find("participant", p).firstResult();
        if (stats == null) {
            stats = new CampStats();
            stats.participant = p;
        }
        stats.isTickVaccinated = dto.isTickVaccinated();
        stats.drugConsent = dto.drugConsent();
        stats.ahv = dto.ahv();
        stats.krankenkasse = dto.krankenkasse();
        stats.krankenkassenNr = dto.krankenkassenNr();
        stats.medication = dto.medication();
        stats.familyDoctor = dto.familyDoctor();
        stats.nationality = dto.nationality();
        stats.nativeLanguage = dto.nativeLanguage();
        stats.foodPreferences = dto.foodPreferences();
        stats.notes = dto.notes();
        stats.persist();
        return toDto(stats);
    }

    private CampStatsDto toDto(CampStats s) {
        return new CampStatsDto(
                s.isTickVaccinated,
                s.drugConsent,
                s.ahv,
                s.krankenkasse,
                s.krankenkassenNr,
                s.medication,
                s.familyDoctor,
                s.nationality,
                s.nativeLanguage,
                s.foodPreferences,
                s.notes);
    }

    public record CampStatsDto(
            boolean isTickVaccinated,
            boolean drugConsent,
            String ahv,
            String krankenkasse,
            String krankenkassenNr,
            String medication,
            String familyDoctor,
            String nationality,
            String nativeLanguage,
            String foodPreferences,
            String notes) {}
}
