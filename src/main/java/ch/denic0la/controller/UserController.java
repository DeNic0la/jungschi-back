package ch.denic0la.controller;

import ch.denic0la.CurrentUserProvisioningService;
import ch.denic0la.model.AppUser;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.NoCache;

import java.util.Comparator;
import java.util.List;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class UserController {

    @Inject
    CurrentUserProvisioningService provisioningService;

    @GET
    @Path("/me")
    @NoCache
    @Transactional
    public MeDto me() {
        AppUser user = provisioningService.ensureCurrentUser();
        return new MeDto(user.email, user.username, user.email, user.phoneNumber, user.firstName, user.lastName, user.address);
    }

    @PUT
    @Path("/me")
    @Transactional
    public MeDto updateMe(UpdateUserDto update) {
        AppUser user = provisioningService.ensureCurrentUser();
        if (update.phoneNumber() != null && !update.phoneNumber().isBlank()) {
            user.phoneNumber = update.phoneNumber();
        }
        if (update.firstName() != null) {
            user.firstName = update.firstName();
        }
        if (update.lastName() != null) {
            user.lastName = update.lastName();
        }
        if (update.address() != null) {
            user.address = update.address();
        }
        user.persist();
        return new MeDto(user.email, user.username, user.email, user.phoneNumber, user.firstName, user.lastName, user.address);
    }

    @GET
    @Path("/guardians")
    @NoCache
    @Transactional
    public List<GuardianUserDto> visibleGuardians() {
        AppUser currentUser = provisioningService.ensureCurrentUser();
        return AppUser.<AppUser>listAll().stream()
                .filter(candidate -> !candidate.email.equals(currentUser.email))
                .filter(candidate -> provisioningService.canViewGuardian(currentUser, candidate))
                .sorted(Comparator
                        .comparing((AppUser user) -> firstNonBlank(user.firstName, user.username))
                        .thenComparing(user -> firstNonBlank(user.lastName, user.email)))
                .map(candidate -> {
                    Long householdId = null;
                    boolean primaryContact = false;
                    boolean secondaryContact = false;
                    var household = provisioningService.findHouseholdForContact(candidate);
                    if (household != null) {
                        householdId = household.id;
                        primaryContact = household.primaryContact != null
                                && household.primaryContact.email.equals(candidate.email);
                        secondaryContact = household.secondaryContact != null
                                && household.secondaryContact.email.equals(candidate.email);
                    }
                    return new GuardianUserDto(
                            candidate.email,
                            candidate.username,
                            candidate.email,
                            candidate.firstName,
                            candidate.lastName,
                            candidate.pictureUrl,
                            householdId,
                            primaryContact,
                            secondaryContact);
                })
                .toList();
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback != null ? fallback : "";
    }

    public record MeDto(String id, String username, String email, String phoneNumber, String firstName, String lastName, String address) {}

    public record UpdateUserDto(String phoneNumber, String firstName, String lastName, String address) {}

    public record GuardianUserDto(
            String id,
            String username,
            String email,
            String firstName,
            String lastName,
            String pictureUrl,
            Long householdId,
            boolean primaryContact,
            boolean secondaryContact) {}
}
