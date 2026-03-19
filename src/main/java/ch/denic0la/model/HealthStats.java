package ch.denic0la.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

@Entity(name = "health_stats")
public class HealthStats extends PanacheEntity {
    @OneToOne
    @JoinColumn(name = "participant_id")
    public Participant participant;

    @Column(name = "isHealthy")
    public boolean isHealthy;
    @Column(name = "healthy_reason")
    public String healthyReason;
    @Column(name = "excluded_activities")
    public String excludedActivities;

}
