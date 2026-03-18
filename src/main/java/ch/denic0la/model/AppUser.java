package ch.denic0la.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name="users")
public class AppUser extends PanacheEntity {

    @Column(name = "oidc_subject", nullable = false, unique = true, updatable = false)
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

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    public Instant lastSeenAt;

}
