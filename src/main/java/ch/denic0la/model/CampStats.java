package ch.denic0la.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity(name = "camp_stats")
public class CampStats extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "participant_id")
    public Participant participant;

    @Column(name = "isTickVaccinated")
    public boolean isTickVaccinated;
    @Column(name = "drugConsent")
    public boolean drugConstent;
    @Column(name = "ahv")
    public String ahv;
    @Column(name = "krankenkasse")
    public String krankenkasse;

    @Column(name = "notes")
    public String notes;

}
