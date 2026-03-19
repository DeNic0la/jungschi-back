package ch.denic0la.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

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
    public GlobalIntoleranceDefinitions intolerance;

    @Column(name = "custom_text")
    public String customText;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "severity")
    public Severity severity;
}
