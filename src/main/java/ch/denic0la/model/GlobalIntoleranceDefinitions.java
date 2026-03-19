package ch.denic0la.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity(name = "global_definitions")
public class GlobalIntoleranceDefinitions extends PanacheEntity {
    public enum Category {
        FoodIntolerance,
        AllergyDefinition
    }
    @Column(name = "label")
    public String label;
    @Column(name = "definition_value")
    public String definitionValue;
    @Column(name = "category")
    public Category category;

}
