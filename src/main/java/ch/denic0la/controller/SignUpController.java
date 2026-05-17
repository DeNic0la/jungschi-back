package ch.denic0la.controller;

import ch.denic0la.CurrentUserProvisioningService;
import ch.denic0la.model.AppUser;
import ch.denic0la.model.Camp;
import ch.denic0la.model.CampParticipant;
import ch.denic0la.model.CampParticipantMedication;
import ch.denic0la.model.Household;
import ch.denic0la.model.Participant;
import ch.denic0la.model.SignUp;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Path("/api/signups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class SignUpController {

    @Inject
    CurrentUserProvisioningService provisioningService;

    @GET
    @Path("/camp/{campId}")
    @Transactional
    public SignUpDto getForCamp(@PathParam("campId") String campId) {
        Household household = householdForCurrentUser();
        Camp camp = findCamp(campId);
        SignUp signUp = SignUp.find("household = ?1 and camp = ?2", household, camp).firstResult();
        return signUp == null ? null : toDto(signUp);
    }

    @POST
    @Transactional
    public SignUpDto save(SignUpInput dto) {
        if (dto == null || dto.campId() == null || dto.campId().isBlank()) {
            throw new BadRequestException("campId is required");
        }
        if (dto.campParticipants() == null || dto.campParticipants().isEmpty()) {
            throw new BadRequestException("At least one camp participant is required");
        }

        Household household = householdForCurrentUser();
        Camp camp = findCamp(dto.campId());
        if (camp.startDate != null && !camp.startDate.isAfter(LocalDate.now())) {
            throw new BadRequestException("Camp has already started");
        }

        SignUp signUp = SignUp.find("household = ?1 and camp = ?2", household, camp).firstResult();
        if (signUp == null) {
            signUp = new SignUp();
            signUp.household = household;
            signUp.camp = camp;
            signUp.state = SignUp.State.IN_PROGRESS;
            signUp.persist();
        } else if (signUp.state == SignUp.State.COMPLETED) {
            signUp.state = SignUp.State.IN_PROGRESS;
        }

        signUp.photoConsent = dto.photoConsent();
        signUp.infoEmail = dto.infoEmail();
        signUp.additionalContactOptionsDuringCamp = blankToNull(dto.additionalContactOptionsDuringCamp());

        replaceCampParticipants(signUp, camp, household, dto.campParticipants());

        return toDto(signUp);
    }

    @PUT
    @Path("/{id}/complete")
    @Transactional
    public SignUpDto complete(@PathParam("id") Long id) {
        Household household = householdForCurrentUser();
        SignUp signUp = SignUp.findById(id);
        if (signUp == null || signUp.household == null || !Objects.equals(signUp.household.id, household.id)) {
            throw new NotFoundException("Signup not found");
        }
        if (signUp.campParticipants == null || signUp.campParticipants.isEmpty()) {
            throw new BadRequestException("At least one camp participant is required");
        }
        signUp.complete();
        return toDto(signUp);
    }

    private Household householdForCurrentUser() {
        AppUser user = provisioningService.ensureCurrentUser();
        Household household = provisioningService.findHouseholdForContact(user);
        if (household == null) {
            household = new Household();
            household.primaryContact = user;
            household.persist();
        }
        return household;
    }

    private Camp findCamp(String campId) {
        Camp camp = Camp.findById(campId);
        if (camp == null) {
            throw new NotFoundException("Camp not found");
        }
        return camp;
    }

    private void replaceCampParticipants(
            SignUp signUp,
            Camp camp,
            Household household,
            List<CampParticipantInput> inputs) {
        List<CampParticipant> existing = CampParticipant.list("signUp", signUp);
        for (CampParticipant campParticipant : existing) {
            CampParticipantMedication.delete("campParticipant", campParticipant);
        }
        CampParticipant.delete("signUp", signUp);
        signUp.campParticipants.clear();

        for (CampParticipantInput input : inputs) {
            Participant participant = Participant.findById(input.participantId());
            if (participant == null
                    || participant.household == null
                    || !Objects.equals(participant.household.id, household.id)) {
                throw new BadRequestException("Participant does not belong to the current household");
            }

            CampParticipant campParticipant = new CampParticipant();
            campParticipant.participant = participant;
            campParticipant.signUp = signUp;
            campParticipant.camp = camp;
            campParticipant.schoolClass = blankToNull(input.schoolClass());
            campParticipant.infosZimmerleitung = blankToNull(input.infosZimmerleitung());
            campParticipant.bemerkungen = blankToNull(input.bemerkungen());
            campParticipant.drugConsent = input.drugConsent();
            campParticipant.persist();
            signUp.campParticipants.add(campParticipant);

            if (input.medications() != null) {
                for (MedicationInput medicationInput : input.medications()) {
                    if (medicationInput.medicationName() == null
                            || medicationInput.medicationName().isBlank()) {
                        continue;
                    }
                    CampParticipantMedication medication = new CampParticipantMedication();
                    medication.campParticipant = campParticipant;
                    medication.medicationName = medicationInput.medicationName().trim();
                    medication.dose = blankToNull(medicationInput.dose());
                    medication.frequency = blankToNull(medicationInput.frequency());
                    medication.purpose = blankToNull(medicationInput.purpose());
                    medication.needsHelp = medicationInput.needsHelp();
                    medication.confidential = medicationInput.confidential();
                    medication.persist();
                    campParticipant.medications.add(medication);
                }
            }
        }
    }

    private SignUpDto toDto(SignUp signUp) {
        List<CampParticipantDto> campParticipants = new ArrayList<>(signUp.campParticipants).stream()
                .sorted(Comparator.comparing(campParticipant -> campParticipant.id))
                .map(this::toDto)
                .toList();
        return new SignUpDto(
                signUp.id,
                signUp.camp != null ? signUp.camp.id : null,
                signUp.state,
                signUp.feedback,
                signUp.photoConsent,
                signUp.infoEmail,
                signUp.additionalContactOptionsDuringCamp,
                campParticipants);
    }

    private CampParticipantDto toDto(CampParticipant campParticipant) {
        List<MedicationDto> medications = new ArrayList<>(campParticipant.medications).stream()
                .sorted(Comparator.comparing(medication -> medication.id))
                .map(medication -> new MedicationDto(
                        medication.medicationName,
                        medication.dose,
                        medication.frequency,
                        medication.purpose,
                        medication.needsHelp,
                        medication.confidential))
                .toList();
        return new CampParticipantDto(
                campParticipant.participant != null ? campParticipant.participant.id : null,
                campParticipant.schoolClass,
                campParticipant.infosZimmerleitung,
                campParticipant.bemerkungen,
                campParticipant.drugConsent,
                medications);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record SignUpInput(
            String campId,
            boolean photoConsent,
            boolean infoEmail,
            String additionalContactOptionsDuringCamp,
            List<CampParticipantInput> campParticipants) {}

    public record CampParticipantInput(
            Long participantId,
            String schoolClass,
            String infosZimmerleitung,
            String bemerkungen,
            boolean drugConsent,
            List<MedicationInput> medications) {}

    public record MedicationInput(
            String medicationName,
            String dose,
            String frequency,
            String purpose,
            boolean needsHelp,
            boolean confidential) {}

    public record SignUpDto(
            Long id,
            String campId,
            SignUp.State state,
            String feedback,
            boolean photoConsent,
            boolean infoEmail,
            String additionalContactOptionsDuringCamp,
            List<CampParticipantDto> campParticipants) {}

    public record CampParticipantDto(
            Long participantId,
            String schoolClass,
            String infosZimmerleitung,
            String bemerkungen,
            boolean drugConsent,
            List<MedicationDto> medications) {}

    public record MedicationDto(
            String medicationName,
            String dose,
            String frequency,
            String purpose,
            boolean needsHelp,
            boolean confidential) {}
}
