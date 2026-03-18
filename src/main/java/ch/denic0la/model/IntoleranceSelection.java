package ch.denic0la.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class IntoleranceSelection extends PanacheEntity {
    public enum Severity {
        AFFECTED,
        STRONG,
        LIFE_THREATENING
    }

    @ManyToOne
    @JoinColumn(name = "participant_id")
    public Participant participant;

    @ManyToOne
    @JoinColumn(name = "intolerance_id")
    public GlobalDefinitions intolerance;

    @Column(name = "custom_text")
    public String customText;

    @Column(name = "severity")
    public Severity severity;
}
