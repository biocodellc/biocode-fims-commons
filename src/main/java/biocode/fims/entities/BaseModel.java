package biocode.fims.entities;

import biocode.fims.serializers.Views;
import com.fasterxml.jackson.annotation.JsonView;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author rjewing
 */
@MappedSuperclass
abstract public class BaseModel implements Serializable {
    protected int id;

    @Id
    @JsonView(Views.Summary.class)
    @GeneratedValue
    public int getId() {
        return id;
    }

    private void setId(int id) {
        this.id = id;
    }
}
