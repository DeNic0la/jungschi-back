package ch.denic0la.controller;

import ch.denic0la.CurrentUserProvisioningService;
import ch.denic0la.model.AppUser;
import ch.denic0la.model.Household;
import ch.denic0la.model.HouseholdGuardian;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
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
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Path("/api/household")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("guardian")
public class HouseholdController {

    @Inject
    CurrentUserProvisioningService provisioningService;

    @GET
    @Path("/me")
    @Transactional
    public HouseholdDto me() {
        return toDto(ensureCurrentHousehold(), provisioningService.getCurrentUser());
    }

    @PUT
    @Path("/me")
    @Transactional
    public HouseholdDto update(UpdateHouseholdDto update) {
        Household household = ensureCurrentHousehold();
        household.streetAndNumber = blankToNull(update.streetAndNumber());
        household.plz = blankToNull(update.plz());
        household.place = blankToNull(update.place());
        return toDto(household, provisioningService.getCurrentUser());
    }

    @POST
    @Path("/me/guardians")
    @Transactional
    public HouseholdDto addGuardian(AddGuardianDto addGuardian) {
        if (addGuardian == null || addGuardian.email() == null || addGuardian.email().isBlank()) {
            throw new BadRequestException("email is required");
        }
        Household household = ensureCurrentHousehold();
        String email = provisioningService.normalizeEmail(addGuardian.email());

        HouseholdGuardian existingMembership = HouseholdGuardian.find("email", email).firstResult();
        if (existingMembership != null && !Objects.equals(existingMembership.household.id, household.id)) {
            throw new WebApplicationException("Guardian already belongs to another household", Response.Status.CONFLICT);
        }
        if (existingMembership != null) {
            return toDto(household, provisioningService.getCurrentUser());
        }

        AppUser existingUser = AppUser.findById(email);
        Household existingUserHousehold = provisioningService.findHouseholdForContact(existingUser);
        if (existingUserHousehold != null && !Objects.equals(existingUserHousehold.id, household.id)) {
            throw new WebApplicationException("Guardian already belongs to another household", Response.Status.CONFLICT);
        }
        if (existingUser != null && !existingUser.hasRole("guardian")) {
            throw new BadRequestException("User is not a guardian");
        }

        HouseholdGuardian membership = new HouseholdGuardian();
        membership.household = household;
        membership.email = email;
        membership.user = existingUser;
        if (existingUser != null && household.secondaryContact == null) {
            membership.contactType = HouseholdGuardian.ContactType.SECONDARY;
            household.secondaryContact = existingUser;
        } else {
            membership.contactType = existingUser != null
                    ? HouseholdGuardian.ContactType.ADDITIONAL
                    : HouseholdGuardian.ContactType.PENDING;
        }
        membership.persist();

        return toDto(household, provisioningService.getCurrentUser());
    }

    @DELETE
    @Path("/me/guardians/{email}")
    @Transactional
    public HouseholdDto removeGuardian(@PathParam("email") String rawEmail) {
        Household household = ensureCurrentHousehold();
        AppUser currentUser = provisioningService.getCurrentUser();
        String email = provisioningService.normalizeEmail(rawEmail);
        HouseholdGuardian membership = HouseholdGuardian.find("household = ?1 and email = ?2", household, email).firstResult();
        if (membership == null) {
            throw new NotFoundException("Guardian not found");
        }
        if (currentUser.email.equals(email) || membership.contactType == HouseholdGuardian.ContactType.PRIMARY) {
            throw new BadRequestException("This guardian cannot be removed");
        }
        long count = HouseholdGuardian.count("household", household);
        if (count <= 1) {
            throw new BadRequestException("The last household guardian cannot be removed");
        }
        if (membership.contactType == HouseholdGuardian.ContactType.SECONDARY) {
            household.secondaryContact = null;
        }
        membership.delete();
        return toDto(household, currentUser);
    }

    @PUT
    @Path("/me/guardians/{email}/contact-type")
    @Transactional
    public HouseholdDto updateGuardianContactType(
            @PathParam("email") String rawEmail,
            UpdateGuardianContactTypeDto update) {
        if (update == null
                || update.contactType() == null
                || update.contactType() == HouseholdGuardian.ContactType.PENDING
                || update.contactType() == HouseholdGuardian.ContactType.ADDITIONAL) {
            throw new BadRequestException("PRIMARY or SECONDARY contactType is required");
        }

        Household household = ensureCurrentHousehold();
        String email = provisioningService.normalizeEmail(rawEmail);
        HouseholdGuardian membership = HouseholdGuardian.find("household = ?1 and email = ?2", household, email).firstResult();
        if (membership == null) {
            throw new NotFoundException("Guardian not found");
        }
        if (membership.user == null) {
            throw new BadRequestException("Pending guardians cannot be primary or secondary contacts");
        }

        if (update.contactType() == HouseholdGuardian.ContactType.PRIMARY) {
            setPrimaryContact(household, membership);
        } else {
            setSecondaryContact(household, membership);
        }

        return toDto(household, provisioningService.getCurrentUser());
    }

    private Household ensureCurrentHousehold() {
        AppUser user = provisioningService.ensureCurrentUser();
        Household household = provisioningService.findHouseholdForContact(user);
        if (household == null) {
            household = new Household();
            household.primaryContact = user;
            household.persist();
            provisioningService.ensureGuardianMembership(household, user, HouseholdGuardian.ContactType.PRIMARY);
        } else {
            HouseholdGuardian membership = HouseholdGuardian
                    .find("household = ?1 and email = ?2", household, provisioningService.normalizeEmail(user.email))
                    .firstResult();
            if (membership == null) {
                HouseholdGuardian.ContactType contactType = HouseholdGuardian.ContactType.ADDITIONAL;
                if (household.primaryContact != null && household.primaryContact.email.equals(user.email)) {
                    contactType = HouseholdGuardian.ContactType.PRIMARY;
                } else if (household.secondaryContact != null && household.secondaryContact.email.equals(user.email)) {
                    contactType = HouseholdGuardian.ContactType.SECONDARY;
                }
                provisioningService.ensureGuardianMembership(household, user, contactType);
            }
        }
        return household;
    }

    private void setPrimaryContact(Household household, HouseholdGuardian newPrimary) {
        List<HouseholdGuardian> memberships = HouseholdGuardian.list("household", household);
        for (HouseholdGuardian membership : memberships) {
            if (membership.contactType == HouseholdGuardian.ContactType.PRIMARY) {
                membership.contactType = HouseholdGuardian.ContactType.ADDITIONAL;
            }
        }
        newPrimary.contactType = HouseholdGuardian.ContactType.PRIMARY;
        household.primaryContact = newPrimary.user;
        if (household.secondaryContact != null && household.secondaryContact.email.equals(newPrimary.email)) {
            household.secondaryContact = null;
        }
    }

    private void setSecondaryContact(Household household, HouseholdGuardian newSecondary) {
        if (household.primaryContact != null && household.primaryContact.email.equals(newSecondary.email)) {
            throw new BadRequestException("Primary contact cannot also be secondary contact");
        }
        List<HouseholdGuardian> memberships = HouseholdGuardian.list("household", household);
        for (HouseholdGuardian membership : memberships) {
            if (membership.contactType == HouseholdGuardian.ContactType.SECONDARY) {
                membership.contactType = HouseholdGuardian.ContactType.ADDITIONAL;
            }
        }
        newSecondary.contactType = HouseholdGuardian.ContactType.SECONDARY;
        household.secondaryContact = newSecondary.user;
    }

    private HouseholdDto toDto(Household household, AppUser currentUser) {
        List<HouseholdGuardianDto> guardians = HouseholdGuardian.<HouseholdGuardian>list("household", household).stream()
                .sorted(Comparator
                        .comparing((HouseholdGuardian guardian) -> guardian.contactType == HouseholdGuardian.ContactType.PENDING)
                        .thenComparing(guardian -> guardian.email))
                .map(guardian -> toDto(guardian, currentUser))
                .toList();
        return new HouseholdDto(
                household.id,
                household.streetAndNumber,
                household.plz,
                household.place,
                guardians);
    }

    private HouseholdGuardianDto toDto(HouseholdGuardian guardian, AppUser currentUser) {
        AppUser user = guardian.user;
        return new HouseholdGuardianDto(
                guardian.email,
                user != null ? user.username : null,
                user != null ? user.firstName : null,
                user != null ? user.lastName : null,
                user != null ? user.pictureUrl : null,
                guardian.contactType,
                user == null,
                currentUser != null && currentUser.email.equals(guardian.email));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record HouseholdDto(
            Long id,
            String streetAndNumber,
            String plz,
            String place,
            List<HouseholdGuardianDto> guardians) {}

    public record HouseholdGuardianDto(
            String email,
            String username,
            String firstName,
            String lastName,
            String pictureUrl,
            HouseholdGuardian.ContactType contactType,
            boolean pending,
            boolean currentUser) {}

    public record UpdateHouseholdDto(String streetAndNumber, String plz, String place) {}

    public record AddGuardianDto(String email) {}

    public record UpdateGuardianContactTypeDto(HouseholdGuardian.ContactType contactType) {}
}
