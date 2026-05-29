package ch.denic0la.model;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class CampEntityTest {

    @Test
    @Transactional
    public void testCampPersistence() {
        Camp camp = new Camp();
        camp.id = "sommerlager-2026";
        camp.title = "Sommerlager 2026";
        camp.description = "Zwei Wochen Lager in den Bergen.";
        camp.startDate = LocalDate.of(2026, 7, 12);
        camp.endDate = LocalDate.of(2026, 7, 26);
        camp.signupEndDate = LocalDate.of(2026, 6, 15);
        camp.isJugendUndSport = true;
        camp.priceFirst = new BigDecimal("250.00");
        camp.priceSecond = new BigDecimal("220.00");
        camp.priceThird = new BigDecimal("190.00");
        camp.persist();

        Camp persisted = Camp.findById("sommerlager-2026");
        assertNotNull(persisted);
        assertEquals("Sommerlager 2026", persisted.title);
        assertEquals(LocalDate.of(2026, 7, 12), persisted.startDate);
        assertEquals(new BigDecimal("250.00"), persisted.priceFirst);
        assertEquals(new BigDecimal("220.00"), persisted.priceSecond);
        assertEquals(new BigDecimal("190.00"), persisted.priceThird);
    }

    @Test
    @Transactional
    public void testRoomPersistence() {
        AppUser leader = new AppUser();
        leader.username = "leader1";
        leader.email = "leader1@example.com";
        leader.createdAt = Instant.now();
        leader.lastSeenAt = Instant.now();
        leader.persist();

        Camp camp = new Camp();
        camp.id = "pfila-2026";
        camp.title = "Pfila 2026";
        camp.persist();

        Room room = new Room();
        room.camp = camp;
        room.name = "Zelt Adler";
        room.maxCapacity = 8;
        room.gender = Gender.MALE;
        room.leaders.add(leader);
        room.persist();

        Room persisted = Room.findById(room.id);
        assertNotNull(persisted);
        assertEquals("Zelt Adler", persisted.name);
        assertEquals(8, persisted.maxCapacity);
        assertEquals(Gender.MALE, persisted.gender);
        assertEquals("pfila-2026", persisted.camp.id);
        assertEquals(1, persisted.leaders.size());
        assertEquals("leader1@example.com", persisted.leaders.get(0).email);
    }
}
