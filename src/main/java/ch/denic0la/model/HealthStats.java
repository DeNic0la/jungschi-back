package ch.denic0la.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity(name = "health_stats")
public class HealthStats extends PanacheEntity {
    @ManyToOne
    @JoinColumn(name = "participant_id")
    public Participant participant;

    @Column(name = "isHealthy")
    public boolean isHealthy;
    @Column(name = "helthy_reason")
    public String helthy_reason;
    @Column(name = "excluded_activities")
    public String excluded_activities;

}
