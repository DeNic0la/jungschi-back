package ch.denic0la.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "camp_participant_medication")
public class CampParticipantMedication extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "camp_participant_id")
    public CampParticipant campParticipant;

    @Column(name = "medication_name")
    public String medicationName;

    @Column(name = "dose")
    public String dose;

    @Column(name = "frequency")
    public String frequency;

    @Column(name = "purpose")
    public String purpose;

    @Column(name = "needs_help")
    public boolean needsHelp;

    @Column(name = "confidential")
    public boolean confidential;
}
