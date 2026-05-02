package ch.denic0la.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "signup")
public class SignUp extends PanacheEntity {

    public enum State {
        IN_PROGRESS,
        REVIEWED,
        COMPLETED
    }

    @ManyToOne
    @JoinColumn(name = "household_id")
    public Household household;

    @ManyToOne
    @JoinColumn(name = "camp_id")
    public Camp camp;

    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    public State state = State.IN_PROGRESS;

    @Column(name = "feedback")
    public String feedback;

    @Column(name = "photo_consent")
    public boolean photoConsent;

    @Column(name = "info_email")
    public boolean infoEmail;

    @Column(name = "additional_contact_options_during_camp")
    public String additionalContactOptionsDuringCamp;

    @OneToMany(mappedBy = "signUp")
    public List<CampParticipant> campParticipants = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (state == null) {
            state = State.IN_PROGRESS;
        }
    }

    public void finish() {
        state = State.REVIEWED;
    }

    public void complete() {
        state = State.COMPLETED;
    }
}
