package ch.denic0la.controller;

import ch.denic0la.CurrentUserProvisioningService;
import ch.denic0la.model.AppUser;
import ch.denic0la.model.Gender;
import ch.denic0la.model.HealthStats;
import ch.denic0la.model.Household;
import ch.denic0la.model.Participant;
import ch.denic0la.model.ParticipantGeneralData;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Path("/api/participants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class ParticipantController {

    @Inject
    CurrentUserProvisioningService provisioningService;

    @GET
    @Transactional
    public List<ParticipantDto> getAll() {
        AppUser user = provisioningService.ensureCurrentUser();
        if (provisioningService.canViewAnything()) {
            return Participant.listAll().stream()
                    .map(p -> (Participant) p)
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }
        Household household = provisioningService.findHouseholdForContact(user);
        if (household == null) {
            return Collections.emptyList();
        }
        return Participant.list("household", household).stream()
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
        if (!provisioningService.canReadParticipant(p, user)) {
            throw new NotFoundException("Participant not found");
        }
        return toDto(p);
    }

    @GET
    @Path("/{id}/info")
    @Transactional
    public ParticipantInfoDto getInfoById(@PathParam("id") Long id) {
        AppUser user = provisioningService.getCurrentUser();
        Participant p = Participant.findById(id);
        if (!provisioningService.canReadParticipant(p, user)) {
            throw new NotFoundException("Participant not found");
        }
        boolean hasHealthStats = HealthStats.count("participant", p) > 0;
        boolean hasCampStats = ParticipantGeneralData.count("participant", p) > 0;
        return toInfoDto(p, hasHealthStats, hasCampStats);
    }

    @POST
    @Transactional
    public ParticipantDto create(ParticipantDto dto) {
        Household household = provisioningService.ensureCurrentUserHousehold();
        Participant p = new Participant();
        p.firstname = dto.firstname();
        p.lastname = dto.lastname();
        p.dateOfBirth = dto.dateOfBirth();
        p.gender = dto.gender();
        p.lastUpdatedAt = LocalDateTime.now();
        p.household = household;
        p.persist();
        return toDto(p);
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public ParticipantDto update(@PathParam("id") Long id, ParticipantDto dto) {
        AppUser user = provisioningService.ensureCurrentUser();
        Participant p = Participant.findById(id);
        if (!provisioningService.canWriteParticipant(p, user)) {
            throw new NotFoundException("Participant not found");
        }
        p.firstname = dto.firstname();
        p.lastname = dto.lastname();
        p.dateOfBirth = dto.dateOfBirth();
        p.gender = dto.gender();
        p.lastUpdatedAt = LocalDateTime.now();
        return toDto(p);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public void delete(@PathParam("id") Long id) {
        AppUser user = provisioningService.getCurrentUser();
        Participant p = Participant.findById(id);
        if (!provisioningService.canWriteParticipant(p, user)) {
            throw new NotFoundException("Participant not found");
        }
        p.delete();
    }

    private ParticipantDto toDto(Participant p) {
        return new ParticipantDto(p.id, p.firstname, p.lastname, p.dateOfBirth, p.gender, p.lastUpdatedAt);
    }

    private ParticipantInfoDto toInfoDto(Participant p, boolean healthStats, boolean campStats) {
        return new ParticipantInfoDto(p.id, p.firstname, p.lastname, p.dateOfBirth, p.gender, p.lastUpdatedAt, healthStats, campStats);
    }

    public record ParticipantDto(Long id, String firstname, String lastname, LocalDate dateOfBirth, Gender gender, LocalDateTime lastUpdatedAt) {}

    public record ParticipantInfoDto(Long id, String firstname, String lastname, LocalDate dateOfBirth, Gender gender, LocalDateTime lastUpdatedAt, boolean healthStats, boolean campStats) {}
}
