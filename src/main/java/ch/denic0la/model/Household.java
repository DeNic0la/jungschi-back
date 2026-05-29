package ch.denic0la.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

@Entity(name = "household")
public class Household extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "primary_contact_id")
    public AppUser primaryContact;

    @ManyToOne
    @JoinColumn(name = "secondary_contact_id")
    public AppUser secondaryContact;

    @Column(name = "secondary_contact_email")
    public String secondaryContactEmail;

    @Column(name = "street_and_number")
    public String streetAndNumber;

    @Column(name = "plz")
    public String plz;

    @Column(name = "place")
    public String place;

    @OneToMany(mappedBy = "household")
    public List<Participant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "household")
    public List<SignUp> signUps = new ArrayList<>();

    @OneToMany(mappedBy = "household")
    public List<HouseholdGuardian> guardians = new ArrayList<>();
}
