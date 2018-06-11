package biocode.fims.models;

import biocode.fims.models.dataTypes.converters.UriPersistenceConverter;
import biocode.fims.serializers.JsonViewOverride;
import biocode.fims.serializers.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

import javax.persistence.*;
import java.net.URI;

/**
 * @author rjewing
 */
@Entity
@Table(name = "entity_identifiers")
public class EntityIdentifier {

    private int id;
    private Expedition expedition;
    private String conceptAlias;
    private URI identifier;

    EntityIdentifier() {
    }

    public EntityIdentifier(Expedition expedition, String conceptAlias, URI identifier) {
        this.expedition = expedition;
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

    void setConceptAlias(String conceptAlias) {
        this.conceptAlias = conceptAlias;
    }

    @Convert(converter = UriPersistenceConverter.class)
    @Column(name = "identifier", updatable = false, nullable = false)
    public URI getIdentifier() {
        return identifier;
    }

    void setIdentifier(URI identifier) {
        this.identifier = identifier;
    }

    @JsonView(Views.Detailed.class)
    @JsonViewOverride(Views.Summary.class)
    @ManyToOne()
    @JoinColumn(name = "expedition_id",
            referencedColumnName = "id",
            insertable = false, nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "entity_identifiers_expedition_id_fkey")
    )
    public Expedition getExpedition() {
        return expedition;
    }

    // needed for hibernate
    private void setExpedition(Expedition expedition) { this.expedition = expedition; }
}
