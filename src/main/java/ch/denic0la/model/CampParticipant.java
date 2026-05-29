package ch.denic0la.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "camp_participant")
public class CampParticipant extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "participant_id")
    public Participant participant;

    @ManyToOne
    @JoinColumn(name = "signup_id")
    public SignUp signUp;

    @ManyToOne
    @JoinColumn(name = "camp_id")
    public Camp camp;

    @ManyToOne
    @JoinColumn(name = "room_id")
    public Room room;

    @Column(name = "school_class")
    public String schoolClass;

    @Column(name = "infos_zimmerleitung")
    public String infosZimmerleitung;

    @Column(name = "bemerkungen")
    public String bemerkungen;

    @Column(name = "drug_consent")
    public boolean drugConsent;

    @OneToMany(mappedBy = "campParticipant")
    public List<CampParticipantMedication> medications = new ArrayList<>();
}
