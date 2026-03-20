package ch.denic0la.controller;

import ch.denic0la.CurrentUserProvisioningService;
import ch.denic0la.model.AppUser;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.NoCache;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserController {

    @Inject
    CurrentUserProvisioningService provisioningService;

    @GET
    @Path("/me")
    @NoCache
    @Transactional
    public MeDto me() {
        AppUser user = provisioningService.ensureCurrentUser();
        return new MeDto(user.oidcSubject, user.oidcSubject, user.username, user.email, user.phoneNumber, user.firstName, user.lastName);
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
        user.persist();
        return new MeDto(user.oidcSubject, user.oidcSubject, user.username, user.email, user.phoneNumber, user.firstName, user.lastName);
    }

    public record MeDto(String id, String oidcSubject, String username, String email, String phoneNumber, String firstName, String lastName) {}

    public record UpdateUserDto(String phoneNumber, String firstName, String lastName) {}
}