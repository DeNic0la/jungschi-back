package ch.denic0la.controller;

import ch.denic0la.CurrentUserProvisioningService;
import ch.denic0la.model.AppUser;
import ch.denic0la.model.Participant;
import ch.denic0la.model.ParticipantGeneralData;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/participants/{participantId}/camp-stats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class ParticipantGeneralDataController {

    @Inject
    CurrentUserProvisioningService provisioningService;

    @GET
    public ParticipantGeneralDataDto get(@PathParam("participantId") Long participantId) {
        AppUser user = provisioningService.getCurrentUser();
        Participant p = Participant.findById(participantId);
        if (!provisioningService.canReadParticipant(p, user)) {
            throw new NotFoundException("Participant not found");
        }

        ParticipantGeneralData stats = ParticipantGeneralData.find("participant", p).firstResult();
        if (stats == null) {
            return null;
        }
        return toDto(stats);
    }

    @PUT
    @Transactional
    public ParticipantGeneralDataDto update(@PathParam("participantId") Long participantId, ParticipantGeneralDataDto dto) {
        AppUser user = provisioningService.getCurrentUser();
        Participant p = Participant.findById(participantId);
        if (!provisioningService.canWriteParticipant(p, user)) {
            throw new NotFoundException("Participant not found");
        }

        ParticipantGeneralData stats = ParticipantGeneralData.find("participant", p).firstResult();
        if (stats == null) {
            stats = new ParticipantGeneralData();
            stats.participant = p;
        }
        stats.isTickVaccinated = dto.isTickVaccinated();
        stats.ahv = dto.ahv();
        stats.krankenkasse = dto.krankenkasse();
        stats.krankenkassenNr = dto.krankenkassenNr();
        stats.familyDoctor = dto.familyDoctor();
        stats.nationality = dto.nationality();
        stats.nativeLanguage = dto.nativeLanguage();
        stats.foodPreferences = dto.foodPreferences();
        stats.notes = dto.notes();
        stats.persist();
        return toDto(stats);
    }

    private ParticipantGeneralDataDto toDto(ParticipantGeneralData s) {
        return new ParticipantGeneralDataDto(
                s.isTickVaccinated,
                s.ahv,
                s.krankenkasse,
                s.krankenkassenNr,
                s.familyDoctor,
                s.nationality,
                s.nativeLanguage,
                s.foodPreferences,
                s.notes);
    }

    public record ParticipantGeneralDataDto(
            boolean isTickVaccinated,
            String ahv,
            String krankenkasse,
            String krankenkassenNr,
            String familyDoctor,
            String nationality,
            String nativeLanguage,
            String foodPreferences,
            String notes) {}
}
