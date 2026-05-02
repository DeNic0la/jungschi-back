package ch.denic0la.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    public Gender gender;

    @ManyToOne
    @JoinColumn(name = "household_id")
    public Household household;

    @OneToOne(mappedBy = "participant", cascade = CascadeType.ALL, orphanRemoval = true)
    public HealthStats healthStats;

    @OneToOne(mappedBy = "participant", cascade = CascadeType.ALL, orphanRemoval = true)
    public CampStats campStats;

    @OneToMany(mappedBy = "participant", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<IntoleranceSelection> intoleranceSelections = new ArrayList<>();

    @OneToMany(mappedBy = "participant")
    public List<CampParticipant> campParticipants = new ArrayList<>();

}
