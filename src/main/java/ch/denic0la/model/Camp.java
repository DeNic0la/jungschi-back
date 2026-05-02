package ch.denic0la.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "camp")
public class Camp extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    public String id;

    @Column(name = "title")
    public String title;

    @Column(name = "description")
    public String description;

    @Column(name = "start_date")
    public LocalDate startDate;

    @Column(name = "end_date")
    public LocalDate endDate;

    @Column(name = "signup_enddate")
    public LocalDate signupEndDate;

    @Column(name = "is_jugend_und_sport")
    public boolean isJugendUndSport;

    @Column(name = "price_first")
    public BigDecimal priceFirst;

    @Column(name = "price_second")
    public BigDecimal priceSecond;

    @Column(name = "price_third")
    public BigDecimal priceThird;

    @OneToMany(mappedBy = "camp")
    public List<Room> rooms = new ArrayList<>();

    @OneToMany(mappedBy = "camp")
    public List<SignUp> signUps = new ArrayList<>();

    @OneToMany(mappedBy = "camp")
    public List<CampParticipant> campParticipants = new ArrayList<>();
}
