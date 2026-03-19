package ch.denic0la.controller;

import ch.denic0la.model.GlobalIntoleranceDefinitions;
import io.quarkus.panache.common.Sort;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/global-definitions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GlobalDefinitionsController {

    @GET
    @Path("/food-intolerances")
    @Transactional
    public List<GlobalDefinitionDto> getFoodIntolerances() {
        List<GlobalIntoleranceDefinitions> definitions = GlobalIntoleranceDefinitions.find("category", Sort.by("label"), GlobalIntoleranceDefinitions.Category.FoodIntolerance).list();
        return definitions.stream()
                .map(this::toDto)
                .toList();
    }

    @GET
    @Path("/allergies")
    @Transactional
    public List<GlobalDefinitionDto> getAllergies() {
        List<GlobalIntoleranceDefinitions> definitions = GlobalIntoleranceDefinitions.find("category", Sort.by("label"), GlobalIntoleranceDefinitions.Category.AllergyDefinition).list();
        return definitions.stream()
                .map(this::toDto)
                .toList();
    }

    private GlobalDefinitionDto toDto(GlobalIntoleranceDefinitions d) {
        return new GlobalDefinitionDto(d.id, d.label, d.definitionValue);
    }

    public record GlobalDefinitionDto(Long id, String label, String definitionValue) {}
}
