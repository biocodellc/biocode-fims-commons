package biocode.fims.repositories;

import biocode.fims.entities.DateItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * repository to populate {@link DateItem} with the current_timestamp
 */
@Transactional
public interface DateItemRepository extends Repository<DateItem, Date> {

    @Query(value = "SELECT current_timestamp AS DATE_VALUE FROM dual", nativeQuery = true)
    DateItem getCurrentDateItem();
}
