package biocode.fims.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.net.URI;

/**
 * @author rjewing
 */
@Entity
@Table(name = "entity_identifiers")
public class EntityIdentifier {

    private int id;
    private String conceptAlias;
    private URI identifier;

    EntityIdentifier() {
    }

    public EntityIdentifier(String conceptAlias, URI identifier) {
        this.conceptAlias = conceptAlias;
        this.identifier = identifier;
    }

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int getId() {
        return id;
    }

    void setId(int id) {
        this.id = id;
    }

    @Column(name = "concept_alias", updatable = false, nullable = false)
    public String getConceptAlias() {
        return conceptAlias;
    }

    private void setConceptAlias(String conceptAlias) {
        this.conceptAlias = conceptAlias;
    }

    @Column(name = "identifier", updatable = false, nullable = false)
    public URI getIdentifier() {
        return identifier;
    }

    private void setIdentifier(URI identifier) {
        this.identifier = identifier;
    }
}
