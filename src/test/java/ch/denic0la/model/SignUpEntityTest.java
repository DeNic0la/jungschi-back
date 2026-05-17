package ch.denic0la.model;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
public class SignUpEntityTest {

    @Inject
    EntityManager entityManager;

    @BeforeEach
    @Transactional
    public void setup() {
        entityManager.createNativeQuery("DELETE FROM room_leader_assignment").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM camp_participant_medication").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM household_guardian").executeUpdate();
        CampParticipant.deleteAll();
        SignUp.deleteAll();
        IntoleranceSelection.deleteAll();
        GlobalIntoleranceDefinitions.deleteAll();
        HealthStats.deleteAll();
        Participant.deleteAll();
        Room.deleteAll();
        Camp.deleteAll();
        Household.deleteAll();
        AppUser.deleteAll();
    }

    @Test
    @Transactional
    public void testSignUpLifecyclePersistence() {
        AppUser primaryContact = new AppUser();
        primaryContact.username = "signupuser";
        primaryContact.email = "signup@example.com";
        primaryContact.createdAt = Instant.now();
        primaryContact.lastSeenAt = Instant.now();
        primaryContact.persist();

        Household household = new Household();
        household.primaryContact = primaryContact;
        household.streetAndNumber = "Lagerweg 12";
        household.plz = "3000";
        household.place = "Bern";
        household.persist();

        Participant participant = new Participant();
        participant.firstname = "Max";
        participant.lastname = "Muster";
        participant.dateOfBirth = LocalDate.of(2011, 5, 4);
        participant.gender = Gender.MALE;
        participant.household = household;
        participant.persist();

        HealthStats healthStats = new HealthStats();
        healthStats.participant = participant;
        healthStats.isHealthy = false;
        healthStats.healthyReason = "Asthma";
        healthStats.persist();

        GlobalIntoleranceDefinitions intoleranceDefinition = new GlobalIntoleranceDefinitions();
        intoleranceDefinition.label = "pollen";
        intoleranceDefinition.definitionValue = "Pollenallergie";
        intoleranceDefinition.category = GlobalIntoleranceDefinitions.Category.AllergyDefinition;
        intoleranceDefinition.persist();

        IntoleranceSelection intoleranceSelection = new IntoleranceSelection();
        intoleranceSelection.participant = participant;
        intoleranceSelection.intolerance = intoleranceDefinition;
        intoleranceSelection.customText = "Birke";
        intoleranceSelection.severity = IntoleranceSelection.Severity.STRONG;
        intoleranceSelection.persist();

        Camp camp = new Camp();
        camp.id = "sommerlager-2027";
        camp.title = "Sommerlager 2027";
        camp.persist();

        SignUp signUp = new SignUp();
        signUp.household = household;
        signUp.camp = camp;
        signUp.photoConsent = true;
        signUp.infoEmail = true;
        signUp.additionalContactOptionsDuringCamp = "Please call after 20:00.";
        signUp.persist();

        Room room = new Room();
        room.camp = camp;
        room.name = "Zelt Murmeli";
        room.maxCapacity = 6;
        room.gender = Gender.MALE;
        room.persist();

        CampParticipant campParticipant = new CampParticipant();
        campParticipant.participant = participant;
        campParticipant.signUp = signUp;
        campParticipant.camp = camp;
        campParticipant.room = null;
        campParticipant.schoolClass = "5a";
        campParticipant.infosZimmerleitung = "Braucht nachts ein Nachtlicht.";
        campParticipant.bemerkungen = "Schwimmt gerne.";
        campParticipant.persist();

        CampParticipantMedication medication = new CampParticipantMedication();
        medication.campParticipant = campParticipant;
        medication.medicationName = "Cetirizin";
        medication.dose = "10mg";
        medication.frequency = "daily";
        medication.purpose = "Allergy";
        medication.needsHelp = false;
        medication.confidential = true;
        medication.persist();

        SignUp persistedSignUp = SignUp.findById(signUp.id);
        assertNotNull(persistedSignUp);
        assertEquals(SignUp.State.IN_PROGRESS, persistedSignUp.state);
        assertEquals("Please call after 20:00.", persistedSignUp.additionalContactOptionsDuringCamp);

        CampParticipant persistedCampParticipant = CampParticipant.findById(campParticipant.id);
        assertNotNull(persistedCampParticipant);
        assertEquals(participant.id, persistedCampParticipant.participant.id);
        assertEquals(signUp.id, persistedCampParticipant.signUp.id);
        assertEquals("sommerlager-2027", persistedCampParticipant.camp.id);
        assertNull(persistedCampParticipant.room);
        assertEquals("5a", persistedCampParticipant.schoolClass);

        CampParticipantMedication persistedMedication = CampParticipantMedication.findById(medication.id);
        assertNotNull(persistedMedication);
        assertEquals(campParticipant.id, persistedMedication.campParticipant.id);
        assertEquals("Cetirizin", persistedMedication.medicationName);
        assertEquals("10mg", persistedMedication.dose);
        assertEquals("daily", persistedMedication.frequency);
        assertEquals("Allergy", persistedMedication.purpose);
        assertEquals(false, persistedMedication.needsHelp);
        assertEquals(true, persistedMedication.confidential);

        persistedCampParticipant.room = room;
        persistedSignUp.feedback = "Bitte vegetarisches Essen einplanen.";
        persistedSignUp.complete();
        persistedSignUp.approve();

        assertEquals(SignUp.State.APPROVED, persistedSignUp.state);
        assertEquals(room.id, persistedCampParticipant.room.id);
        assertEquals("Bitte vegetarisches Essen einplanen.", persistedSignUp.feedback);

        Object[] emergencyRow = (Object[]) entityManager.createNativeQuery("""
                SELECT participant_full_name,
                       camp_medication,
                       intolerances,
                       health,
                       primary_contact_email,
                       household_street_and_number
                FROM reporting.EMERGENCY_DATA
                WHERE camp_participant_id = ?1
                """)
                .setParameter(1, campParticipant.id)
                .getSingleResult();

        assertEquals("Max Muster", emergencyRow[0]);
        assertEquals("Cetirizin / 10mg", emergencyRow[1]);
        assertEquals("Pollenallergie Birke STRONG", emergencyRow[2]);
        assertEquals("Asthma", emergencyRow[3]);
        assertEquals("signup@example.com", emergencyRow[4]);
        assertEquals("Lagerweg 12", emergencyRow[5]);
    }
}
