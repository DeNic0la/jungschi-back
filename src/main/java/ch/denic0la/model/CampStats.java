package ch.denic0la.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

@Entity(name = "camp_stats")
public class CampStats extends PanacheEntity {

    @OneToOne
    @JoinColumn(name = "participant_id")
    public Participant participant;

    @Column(name = "isTickVaccinated")
    public boolean isTickVaccinated;
    @Column(name = "drugConsent")
    public boolean drugConsent;
    @Column(name = "ahv")
    public String ahv;
    @Column(name = "krankenkasse")
    public String krankenkasse;
    @Column(name = "krankenkassenNr")
    public String krankenkassenNr;
    @Column(name = "medication")
    public String medication;

    @Column(name = "notes")
    public String notes;

}
