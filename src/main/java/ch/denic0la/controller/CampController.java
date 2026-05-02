package ch.denic0la.controller;

import ch.denic0la.model.Camp;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Path("/api/camps")
@Produces(MediaType.APPLICATION_JSON)
public class CampController {

    @GET
    @Transactional
    public List<CampDto> getAll() {
        return Camp.<Camp>listAll().stream()
                .sorted(Comparator
                        .comparing((Camp camp) -> camp.startDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(camp -> camp.id))
                .map(this::toDto)
                .toList();
    }

    @GET
    @Path("/{id}")
    @Transactional
    public CampDto getById(@PathParam("id") String id) {
        Camp camp = Camp.findById(id);
        if (camp == null) {
            throw new NotFoundException("Camp not found");
        }
        return toDto(camp);
    }

    private CampDto toDto(Camp camp) {
        return new CampDto(
                camp.id,
                camp.title,
                camp.description,
                camp.startDate,
                camp.endDate,
                camp.signupEndDate,
                camp.isJugendUndSport,
                camp.priceFirst,
                camp.priceSecond,
                camp.priceThird);
    }

    public record CampDto(
            String id,
            String title,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate signupEndDate,
            boolean isJugendUndSport,
            BigDecimal priceFirst,
            BigDecimal priceSecond,
            BigDecimal priceThird) {}
}
