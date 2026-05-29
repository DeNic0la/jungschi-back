package ch.denic0la.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "room")
public class Room extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "camp_id")
    public Camp camp;

    @Column(name = "name")
    public String name;

    @Column(name = "max_capacity")
    public Integer maxCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    public Gender gender;

    @ManyToMany
    @JoinTable(
            name = "room_leader_assignment",
            joinColumns = @JoinColumn(name = "room_id"),
            inverseJoinColumns = @JoinColumn(name = "user_email")
    )
    public List<AppUser> leaders = new ArrayList<>();

    @OneToMany(mappedBy = "room")
    public List<CampParticipant> campParticipants = new ArrayList<>();
}
