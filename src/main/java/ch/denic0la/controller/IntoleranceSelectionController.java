package ch.denic0la.controller;

import ch.denic0la.CurrentUserProvisioningService;
import ch.denic0la.model.AppUser;
import ch.denic0la.model.GlobalDefinitions;
import ch.denic0la.model.IntoleranceSelection;
import ch.denic0la.model.Participant;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.stream.Collectors;

@Path("/api/participants/{participantId}/intolerance-selections")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IntoleranceSelectionController {

    @Inject
    CurrentUserProvisioningService provisioningService;

    @GET
    @Transactional
    public List<IntoleranceSelectionDto> get(@PathParam("participantId") Long participantId) {
        AppUser user = provisioningService.getCurrentUser();
        Participant p = Participant.findById(participantId);
        if (p == null || !p.user.oidcSubject.equals(user.oidcSubject)) {
            throw new NotFoundException("Participant not found");
        }

        return IntoleranceSelection.find("participant", p).stream()
                .map(s -> (IntoleranceSelection) s)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @PUT
    @Transactional
    public IntoleranceSelectionDto update(@PathParam("participantId") Long participantId, IntoleranceSelectionDto dto) {
        AppUser user = provisioningService.getCurrentUser();
        Participant p = Participant.findById(participantId);
        if (p == null || !p.user.oidcSubject.equals(user.oidcSubject)) {
            throw new NotFoundException("Participant not found");
        }

        IntoleranceSelection selection;
        if (dto.intoleranceId() != null) {
            GlobalDefinitions intolerance = GlobalDefinitions.findById(dto.intoleranceId());
            if (intolerance == null) {
                throw new NotFoundException("GlobalDefinition not found");
            }
            selection = IntoleranceSelection.find("participant = ?1 and intolerance = ?2", p, intolerance).firstResult();
            if (selection == null) {
                selection = new IntoleranceSelection();
                selection.participant = p;
                selection.intolerance = intolerance;
            }
        } else {
            // "allow one value without global definition per participant"
            selection = IntoleranceSelection.find("participant = ?1 and intolerance is null", p).firstResult();
            if (selection == null) {
                selection = new IntoleranceSelection();
                selection.participant = p;
                selection.intolerance = null;
            }
        }

        selection.customText = dto.customText();
        selection.severity = dto.severity();
        selection.persist();

        return toDto(selection);
    }

    @DELETE
    @Transactional
    public void delete(@PathParam("participantId") Long participantId, @QueryParam("intoleranceId") Long intoleranceId) {
        AppUser user = provisioningService.getCurrentUser();
        Participant p = Participant.findById(participantId);
        if (p == null || !p.user.oidcSubject.equals(user.oidcSubject)) {
            throw new NotFoundException("Participant not found");
        }

        if (intoleranceId != null) {
            GlobalDefinitions intolerance = GlobalDefinitions.findById(intoleranceId);
            if (intolerance == null) {
                throw new NotFoundException("GlobalDefinition not found");
            }
            IntoleranceSelection.delete("participant = ?1 and intolerance = ?2", p, intolerance);
        } else {
            IntoleranceSelection.delete("participant = ?1 and intolerance is null", p);
        }
    }

    private IntoleranceSelectionDto toDto(IntoleranceSelection s) {
        return new IntoleranceSelectionDto(
                s.intolerance != null ? s.intolerance.id : null,
                s.customText,
                s.severity
        );
    }

    public record IntoleranceSelectionDto(Long intoleranceId, String customText, IntoleranceSelection.Severity severity) {}
}
