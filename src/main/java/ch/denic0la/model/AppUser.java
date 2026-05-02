package ch.denic0la.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name="users")
public class AppUser extends PanacheEntityBase {

    @Id
    @Column(name = "oidc_subject", nullable = false, updatable = false)
    public String oidcSubject;

    @Column(name = "username")
    public String username;

    @Column(name = "email")
    public String email;

    @Column(name = "first_name")
    public String firstName;

    @Column(name = "phonenumber")
    public String phoneNumber;

    @Column(name = "last_name")
    public String lastName;

    @Column(name = "address")
    public String address;

    @Column(name = "picture_url")
    public String pictureUrl;

    @Column(name = "roles")
    public String roles;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    public Instant lastSeenAt;

    @OneToMany(mappedBy = "primaryContact")
    public List<Household> households = new ArrayList<>();

    @OneToMany(mappedBy = "secondaryContact")
    public List<Household> secondaryHouseholds = new ArrayList<>();

    @ManyToMany(mappedBy = "leaders")
    public List<Room> leadRooms = new ArrayList<>();

    public boolean hasRole(String role) {
        if (roles == null || roles.isBlank() || role == null || role.isBlank()) {
            return false;
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .anyMatch(role::equals);
    }
}
