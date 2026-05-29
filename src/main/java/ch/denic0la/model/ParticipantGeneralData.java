package ch.denic0la.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity(name = "participant_general_data")
@Table(name = "participant_general_data")
public class ParticipantGeneralData extends PanacheEntity {

    @OneToOne
    @JoinColumn(name = "participant_id")
    public Participant participant;

    @Column(name = "isTickVaccinated")
    public boolean isTickVaccinated;

    @Column(name = "ahv")
    public String ahv;

    @Column(name = "krankenkasse")
    public String krankenkasse;

    @Column(name = "krankenkassenNr")
    public String krankenkassenNr;

    @Column(name = "familyDoctor")
    public String familyDoctor;

    @Column(name = "nationality")
    public String nationality;

    @Column(name = "native_language")
    public String nativeLanguage;

    @Column(name = "food_preferences")
    public String foodPreferences;

    @Column(name = "notes")
    public String notes;
}
