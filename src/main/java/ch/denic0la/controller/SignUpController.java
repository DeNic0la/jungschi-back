package ch.denic0la.controller;

import ch.denic0la.CurrentUserProvisioningService;
import ch.denic0la.model.AppUser;
import ch.denic0la.model.Camp;
import ch.denic0la.model.CampParticipant;
import ch.denic0la.model.CampParticipantMedication;
import ch.denic0la.model.Household;
import ch.denic0la.model.Participant;
import ch.denic0la.model.Room;
import ch.denic0la.model.SignUp;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.annotation.security.RolesAllowed;
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
    @RolesAllowed("guardian")
    public SignUpDto getForCamp(@PathParam("campId") String campId) {
        Household household = householdForCurrentUser();
        Camp camp = findCamp(campId);
        SignUp signUp = SignUp.find("household = ?1 and camp = ?2", household, camp).firstResult();
        return signUp == null ? null : toGuardianDto(signUp);
    }

    @GET
    @Path("/camp/{campId}/review")
    @Transactional
    @RolesAllowed({"Jungschiteam", "ADMIN"})
    public List<TeamSignUpDto> listForReview(@PathParam("campId") String campId) {
        AppUser user = provisioningService.ensureCurrentUser();
        Camp camp = findCamp(campId);
        return SignUp.<SignUp>list("camp", camp).stream()
                .sorted(Comparator
                        .comparingInt((SignUp signUp) -> reviewStateOrder(signUp.state))
                        .thenComparing(signUp -> signUp.household != null ? signUp.household.id : 0))
                .map(signUp -> toTeamDto(signUp, user))
                .toList();
    }

    @POST
    @Transactional
    @RolesAllowed("guardian")
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
        } else if (signUp.state != SignUp.State.IN_PROGRESS) {
            throw new BadRequestException("Signup must be reopened before it can be edited");
        }

        signUp.photoConsent = dto.photoConsent();
        signUp.infoEmail = dto.infoEmail();
        signUp.additionalContactOptionsDuringCamp = blankToNull(dto.additionalContactOptionsDuringCamp());

        replaceCampParticipants(signUp, camp, household, dto.campParticipants());

        return toGuardianDto(signUp);
    }

    @PUT
    @Path("/{id}/complete")
    @Transactional
    @RolesAllowed("guardian")
    public SignUpDto complete(@PathParam("id") Long id) {
        Household household = householdForCurrentUser();
        SignUp signUp = SignUp.findById(id);
        if (signUp == null || signUp.household == null || !Objects.equals(signUp.household.id, household.id)) {
            throw new NotFoundException("Signup not found");
        }
        if (signUp.state != SignUp.State.IN_PROGRESS) {
            throw new BadRequestException("Only in-progress signups can be completed");
        }
        if (signUp.campParticipants == null || signUp.campParticipants.isEmpty()) {
            throw new BadRequestException("At least one camp participant is required");
        }
        signUp.complete();
        return toGuardianDto(signUp);
    }

    @PUT
    @Path("/{id}/reopen")
    @Transactional
    @RolesAllowed("guardian")
    public SignUpDto reopen(@PathParam("id") Long id) {
        Household household = householdForCurrentUser();
        SignUp signUp = SignUp.findById(id);
        if (signUp == null || signUp.household == null || !Objects.equals(signUp.household.id, household.id)) {
            throw new NotFoundException("Signup not found");
        }
        if (signUp.state == SignUp.State.IN_PROGRESS) {
            return toGuardianDto(signUp);
        }
        if (signUp.state == SignUp.State.APPROVED) {
            signUp.feedback = null;
        }
        signUp.reopen();
        return toGuardianDto(signUp);
    }

    @PUT
    @Path("/{id}/feedback")
    @Transactional
    @RolesAllowed({"Jungschiteam", "ADMIN"})
    public TeamSignUpDto updateFeedback(@PathParam("id") Long id, FeedbackInput input) {
        AppUser user = provisioningService.ensureCurrentUser();
        SignUp signUp = findSignUp(id);
        signUp.feedback = blankToNull(input == null ? null : input.feedback());
        return toTeamDto(signUp, user);
    }

    @PUT
    @Path("/{id}/reject")
    @Transactional
    @RolesAllowed({"Jungschiteam", "ADMIN"})
    public TeamSignUpDto reject(@PathParam("id") Long id, FeedbackInput input) {
        AppUser user = provisioningService.ensureCurrentUser();
        SignUp signUp = findSignUp(id);
        String feedback = blankToNull(input == null ? null : input.feedback());
        if (feedback == null) {
            throw new BadRequestException("Feedback is required");
        }
        signUp.feedback = feedback;
        signUp.reopen();
        return toTeamDto(signUp, user);
    }

    @PUT
    @Path("/{id}/approve")
    @Transactional
    @RolesAllowed({"Jungschiteam", "ADMIN"})
    public TeamSignUpDto approve(@PathParam("id") Long id) {
        AppUser user = provisioningService.ensureCurrentUser();
        SignUp signUp = findSignUp(id);
        if (signUp.state != SignUp.State.COMPLETED) {
            throw new BadRequestException("Only completed signups can be approved");
        }
        signUp.approve();
        return toTeamDto(signUp, user);
    }

    @GET
    @Path("/camp-participants/{campParticipantId}")
    @Transactional
    @RolesAllowed({"guardian", "Jungschiteam", "ADMIN", "Sanitaet"})
    public CampParticipantDetailDto getCampParticipant(@PathParam("campParticipantId") Long campParticipantId) {
        AppUser user = provisioningService.ensureCurrentUser();
        CampParticipant campParticipant = CampParticipant.findById(campParticipantId);
        if (campParticipant == null
                || (!provisioningService.canReadCampParticipant(campParticipant, user)
                && !provisioningService.canViewCampParticipantTeamOperationalData(campParticipant))) {
            throw new NotFoundException("Camp participant not found");
        }
        return toDetailDto(campParticipant, user);
    }

    @PUT
    @Path("/camp-participants/{campParticipantId}/room")
    @Transactional
    @RolesAllowed({"Jungschiteam", "ADMIN"})
    public TeamSignUpDto assignRoom(
            @PathParam("campParticipantId") Long campParticipantId,
            AssignRoomInput input) {
        AppUser user = provisioningService.ensureCurrentUser();
        CampParticipant campParticipant = CampParticipant.findById(campParticipantId);
        if (campParticipant == null || campParticipant.signUp == null) {
            throw new NotFoundException("Camp participant not found");
        }
        if (input == null || input.roomId() == null) {
            campParticipant.room = null;
        } else {
            Room room = Room.findById(input.roomId());
            if (room == null
                    || room.camp == null
                    || campParticipant.camp == null
                    || !Objects.equals(room.camp.id, campParticipant.camp.id)) {
                throw new BadRequestException("Room must belong to the same camp");
            }
            validateRoomAssignment(campParticipant, room);
            campParticipant.room = room;
        }
        return toTeamDto(campParticipant.signUp, user);
    }

    private Household householdForCurrentUser() {
        return provisioningService.ensureCurrentUserHousehold();
    }

    private Camp findCamp(String campId) {
        Camp camp = Camp.findById(campId);
        if (camp == null) {
            throw new NotFoundException("Camp not found");
        }
        return camp;
    }

    private SignUp findSignUp(Long id) {
        SignUp signUp = SignUp.findById(id);
        if (signUp == null) {
            throw new NotFoundException("Signup not found");
        }
        return signUp;
    }

    private int reviewStateOrder(SignUp.State state) {
        if (state == SignUp.State.COMPLETED) {
            return 0;
        }
        if (state == SignUp.State.IN_PROGRESS) {
            return 1;
        }
        return 2;
    }

    private void validateRoomAssignment(CampParticipant campParticipant, Room room) {
        if (campParticipant.participant == null) {
            throw new BadRequestException("Camp participant has no participant data");
        }
        boolean currentlyAssigned = campParticipant.room != null
                && Objects.equals(campParticipant.room.id, room.id);
        if (room.gender != null && !room.gender.equals(campParticipant.participant.gender)) {
            throw new BadRequestException("Room gender does not match participant gender");
        }
        if (!currentlyAssigned && room.maxCapacity != null && CampParticipant.count("room", room) >= room.maxCapacity) {
            throw new BadRequestException("Room is full");
        }
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

    private SignUpDto toGuardianDto(SignUp signUp) {
        List<CampParticipantDto> campParticipants = new ArrayList<>(signUp.campParticipants).stream()
                .sorted(Comparator.comparing(campParticipant -> campParticipant.id))
                .map(this::toGuardianDto)
                .toList();
        return new SignUpDto(
                signUp.id,
                signUp.camp != null ? signUp.camp.id : null,
                signUp.state,
                signUp.state == SignUp.State.IN_PROGRESS ? signUp.feedback : null,
                signUp.photoConsent,
                signUp.infoEmail,
                signUp.additionalContactOptionsDuringCamp,
                campParticipants);
    }

    private CampParticipantDto toGuardianDto(CampParticipant campParticipant) {
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

    private TeamSignUpDto toTeamDto(SignUp signUp, AppUser user) {
        List<TeamCampParticipantDto> campParticipants = new ArrayList<>(signUp.campParticipants).stream()
                .sorted(Comparator.comparing(campParticipant -> campParticipant.id))
                .map(campParticipant -> toTeamDto(campParticipant, user))
                .toList();
        return new TeamSignUpDto(
                signUp.id,
                signUp.camp != null ? signUp.camp.id : null,
                signUp.household != null ? signUp.household.id : null,
                signUp.state,
                signUp.feedback,
                signUp.photoConsent,
                signUp.infoEmail,
                signUp.additionalContactOptionsDuringCamp,
                campParticipants);
    }

    private TeamCampParticipantDto toTeamDto(CampParticipant campParticipant, AppUser user) {
        Participant participant = campParticipant.participant;
        Room room = campParticipant.room;
        boolean fullAccess = provisioningService.canReadCampParticipant(campParticipant, user);
        boolean canViewRoomLeaderInfo = provisioningService.canViewRoomLeaderInfo(campParticipant, user);
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
        return new TeamCampParticipantDto(
                campParticipant.id,
                participant != null ? participant.id : null,
                participant != null ? participant.firstname : null,
                participant != null ? participant.lastname : null,
                participant != null ? participant.gender : null,
                campParticipant.schoolClass,
                canViewRoomLeaderInfo ? campParticipant.infosZimmerleitung : null,
                fullAccess ? campParticipant.bemerkungen : null,
                fullAccess ? campParticipant.drugConsent : null,
                room != null ? room.id : null,
                room != null ? room.name : null,
                fullAccess ? medications : List.of(),
                fullAccess,
                canViewRoomLeaderInfo);
    }

    private CampParticipantDetailDto toDetailDto(CampParticipant campParticipant, AppUser user) {
        Participant participant = campParticipant.participant;
        Room room = campParticipant.room;
        boolean fullAccess = provisioningService.canReadCampParticipant(campParticipant, user);
        boolean canViewRoomLeaderInfo = provisioningService.canViewRoomLeaderInfo(campParticipant, user);
        List<MedicationDto> medications = fullAccess
                ? new ArrayList<>(campParticipant.medications).stream()
                .sorted(Comparator.comparing(medication -> medication.id))
                .map(medication -> new MedicationDto(
                        medication.medicationName,
                        medication.dose,
                        medication.frequency,
                        medication.purpose,
                        medication.needsHelp,
                        medication.confidential))
                .toList()
                : List.of();
        return new CampParticipantDetailDto(
                campParticipant.id,
                participant != null ? participant.id : null,
                participant != null ? participant.firstname : null,
                participant != null ? participant.lastname : null,
                fullAccess && participant != null ? participant.dateOfBirth : null,
                participant != null ? participant.gender : null,
                campParticipant.schoolClass,
                canViewRoomLeaderInfo ? campParticipant.infosZimmerleitung : null,
                fullAccess ? campParticipant.bemerkungen : null,
                fullAccess ? campParticipant.drugConsent : null,
                room != null ? room.id : null,
                room != null ? room.name : null,
                medications,
                fullAccess,
                canViewRoomLeaderInfo);
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

    public record FeedbackInput(String feedback) {}

    public record AssignRoomInput(Long roomId) {}

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

    public record TeamSignUpDto(
            Long id,
            String campId,
            Long householdId,
            SignUp.State state,
            String feedback,
            boolean photoConsent,
            boolean infoEmail,
            String additionalContactOptionsDuringCamp,
            List<TeamCampParticipantDto> campParticipants) {}

    public record TeamCampParticipantDto(
            Long id,
            Long participantId,
            String firstname,
            String lastname,
            ch.denic0la.model.Gender gender,
            String schoolClass,
            String infosZimmerleitung,
            String bemerkungen,
            Boolean drugConsent,
            Long roomId,
            String roomName,
            List<MedicationDto> medications,
            boolean fullAccess,
            boolean roomLeaderInfoVisible) {}

    public record CampParticipantDetailDto(
            Long id,
            Long participantId,
            String firstname,
            String lastname,
            LocalDate dateOfBirth,
            ch.denic0la.model.Gender gender,
            String schoolClass,
            String infosZimmerleitung,
            String bemerkungen,
            Boolean drugConsent,
            Long roomId,
            String roomName,
            List<MedicationDto> medications,
            boolean fullAccess,
            boolean roomLeaderInfoVisible) {}
}
