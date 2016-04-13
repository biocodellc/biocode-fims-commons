package biocode.fims.entities;

import java.sql.Timestamp;

/**
 * Expedition entity object
 */
public class Expedition {
    public static final String EXPEDITION_RESOURCE_TYPE = "http://purl.org/dc/dcmitype/Collection";

    private Integer expeditionId;
    private String expeditionCode;
    private String expeditionTitle;
    private int userId;
    private Timestamp ts;
    private Integer projectId;
    private boolean isPublic;

    private Bcid bcid;

    public static class ExpeditionBuilder {

        // Required
        private String expeditionCode;
        private int userId;
        private int projectId;
        // Optional
        private String expeditionTitle;
        private boolean isPublic = true;

        public ExpeditionBuilder(String expeditionCode, int userId, int projectId) {
            this.expeditionCode = expeditionCode;
            this.userId = userId;
            this.projectId = projectId;
        }

        public ExpeditionBuilder expeditionTitle(String expeditionTitle) {
            this.expeditionTitle = expeditionTitle;
            return this;
        }

        public ExpeditionBuilder isPublic(boolean isPublic) {
            this.isPublic = isPublic;
            return this;
        }

        public Expedition build() {
            if (expeditionTitle == null)
                expeditionTitle = expeditionCode + " dataset";
            return new Expedition(this);
        }

    }
    private Expedition(ExpeditionBuilder builder) {
        expeditionCode = builder.expeditionCode;
        expeditionTitle = builder.expeditionTitle;
        userId = builder.userId;
        projectId = builder.projectId;
        isPublic = builder.isPublic;
    }

    public boolean isNew() {
        return this.expeditionId == null;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public Integer getExpeditionId() {
        return expeditionId;
    }

    /**
     * This will only set the expeditionId if the current expeditionId is null. This method is only to be used when
     * fetching an Expedition from the db
     * @param expeditionId
     */
    public void setExpeditionId(Integer expeditionId) {
        if (this.expeditionId == null)
            this.expeditionId = expeditionId;
    }

    public String getExpeditionCode() {
        return expeditionCode;
    }

    public String getExpeditionTitle() {
        return expeditionTitle;
    }

    public void setExpeditionTitle(String expeditionTitle) {
        this.expeditionTitle = expeditionTitle;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Timestamp getTs() {
        return ts;
    }

    public void setTs(Timestamp ts) {
        this.ts = ts;
    }

    public int getProjectId() {
        return projectId;
    }

    /**
     * This will only set the projectId if the current projectId is null. This method is only to be used when
     * fetching an Expedition from the db
     * @param projectId
     */
    public void setProjectId(int projectId) {
        if (this.projectId == null)
            this.projectId = projectId;
    }

    public Bcid getBcid() {
        return bcid;
    }

    /**
     * This will only set the bcid if the current bcid is null. This method is only to be used when
     * fetching an Expedition from the db
     * @param bcid
     */
    public void setBcid(Bcid bcid) {
        if (this.bcid == null)
            this.bcid = bcid;
    }

    @Override
    public String toString() {
        return "Expedition{" +
                "expeditionId=" + expeditionId +
                ", expeditionCode='" + expeditionCode + '\'' +
                ", expeditionTitle='" + expeditionTitle + '\'' +
                ", userId=" + userId +
                ", ts=" + ts +
                ", projectId=" + projectId +
                ", isPublic=" + isPublic +
                ", bcid=" + bcid +
                '}';
    }
}