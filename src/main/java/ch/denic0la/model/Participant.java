package ch.denic0la.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity(name = "participants")
public class Participant extends PanacheEntity {

    @Column(name = "firstname")
    public String firstname;
    @Column(name = "lastname")
    public String lastname;
    @Column(name = "date_of_birth")
    public LocalDate dateOfBirth;
    @Column(name = "last_updated_at")
    public LocalDateTime lastUpdatedAt;

    @ManyToOne
    @JoinColumn(name = "app_user_id")
    public AppUser user;
}
