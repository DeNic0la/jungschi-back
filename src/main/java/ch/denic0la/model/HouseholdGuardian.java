package ch.denic0la.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "household_guardian")
public class HouseholdGuardian extends PanacheEntity {

    public enum ContactType {
        PRIMARY,
        SECONDARY,
        ADDITIONAL,
        PENDING
    }

    @ManyToOne
    @JoinColumn(name = "household_id")
    public Household household;

    @ManyToOne
    @JoinColumn(name = "user_email")
    public AppUser user;

    @Column(name = "email", nullable = false)
    public String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", nullable = false)
    public ContactType contactType = ContactType.ADDITIONAL;
}
