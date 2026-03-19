package ch.denic0la.controller;

import ch.denic0la.CurrentUserProvisioningService;
import ch.denic0la.model.AppUser;
import ch.denic0la.model.Participant;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Path("/api/participants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ParticipantController {

    @Inject
    CurrentUserProvisioningService provisioningService;

    @GET
    @Transactional
    public List<ParticipantDto> getAll() {
        AppUser user = provisioningService.ensureCurrentUser();
        return Participant.list("user.oidcSubject", user.oidcSubject).stream()
                .map(p -> (Participant) p)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    @Transactional
    public ParticipantDto getById(@PathParam("id") Long id) {
        AppUser user = provisioningService.getCurrentUser();
        Participant p = Participant.findById(id);
        if (p == null || !p.user.oidcSubject.equals(user.oidcSubject)) {
            throw new NotFoundException("Participant not found");
        }
        return toDto(p);
    }

    @POST
    @Transactional
    public ParticipantDto create(ParticipantDto dto) {
        AppUser user = provisioningService.ensureCurrentUser();
        Participant p = new Participant();
        p.firstname = dto.firstname();
        p.lastname = dto.lastname();
        p.dateOfBirth = dto.dateOfBirth();
        p.lastUpdatedAt = LocalDateTime.now();
        p.user = user;
        p.persist();
        return toDto(p);
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public ParticipantDto update(@PathParam("id") Long id, ParticipantDto dto) {
        AppUser user = provisioningService.ensureCurrentUser();
        Participant p = Participant.findById(id);
        if (p == null || !p.user.oidcSubject.equals(user.oidcSubject)) {
            throw new NotFoundException("Participant not found");
        }
        p.firstname = dto.firstname();
        p.lastname = dto.lastname();
        p.dateOfBirth = dto.dateOfBirth();
        p.lastUpdatedAt = LocalDateTime.now();
        return toDto(p);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public void delete(@PathParam("id") Long id) {
        AppUser user = provisioningService.getCurrentUser();
        Participant p = Participant.findById(id);
        if (p == null || !p.user.oidcSubject.equals(user.oidcSubject)) {
            throw new NotFoundException("Participant not found");
        }
        p.delete();
    }

    private ParticipantDto toDto(Participant p) {
        return new ParticipantDto(p.id, p.firstname, p.lastname, p.dateOfBirth, p.lastUpdatedAt);
    }

    public record ParticipantDto(Long id, String firstname, String lastname, LocalDate dateOfBirth, LocalDateTime lastUpdatedAt) {}
}
