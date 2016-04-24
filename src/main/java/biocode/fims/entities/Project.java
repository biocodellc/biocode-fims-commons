package biocode.fims.entities;

import java.sql.Timestamp;

/**
 * Expedition entity object
 */
public class Project {

    private Integer projectId;
    private String projectCode;
    private String projectTitle;
    private String projectAbstract;
    private int userId;
    private String validationXml;
    private Timestamp ts;
    private boolean isPublic;

    public static class ProjectBuilder {

        // Required
        private String projectCode;
        private String projectTitle;
        private int userId;
        private String validationXml;

        // Optional
        private String projectAbstract;
        private boolean isPublic = true;

        public ProjectBuilder(String projectCode, String projectTitle,  int userId, String validationXml) {
            this.projectCode = projectCode;
            this.projectTitle = projectTitle;
            this.userId = userId;
            this.validationXml = validationXml;
        }

        public ProjectBuilder isPublic(boolean isPublic) {
            this.isPublic = isPublic;
            return this;
        }

        public ProjectBuilder projectAbstract(String projectAbstract) {
            this.projectAbstract = projectAbstract;
            return this;
        }

        public Project build() {
            return new Project(this);
        }

    }
    private Project(ProjectBuilder builder) {
        projectCode = builder.projectCode;
        projectTitle = builder.projectTitle;
        userId = builder.userId;
        validationXml = builder.validationXml;
        projectAbstract = builder.projectAbstract;
        isPublic = builder.isPublic;
    }

    public boolean isNew() {
        return this.projectId == null;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public Integer getProjectId() {
        return projectId;
    }

    /**
     * This will only set the projectId if the current projectId is null. This method is only to be used when
     * fetching a Project from the db
     * @param projectId
     */
    public void setProjectId(int projectId) {
        if (this.projectId == null)
            this.projectId = projectId;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getProjectAbstract() {
        return projectAbstract;
    }

    public void setProjectAbstract(String projectAbstract) {
        this.projectAbstract = projectAbstract;
    }

    public String getValidationXml() {
        return validationXml;
    }

    public void setValidationXml(String validationXml) {
        this.validationXml = validationXml;
    }

    public Timestamp getTs() {
        return ts;
    }

    public void setTs(Timestamp ts) {
        this.ts = ts;
    }

    @Override
    public String toString() {
        return "Project{" +
                "projectId=" + projectId +
                ", projectCode='" + projectCode + '\'' +
                ", projectTitle='" + projectTitle + '\'' +
                ", projectAbstract='" + projectAbstract + '\'' +
                ", userId=" + userId +
                ", validationXml='" + validationXml + '\'' +
                ", ts=" + ts +
                ", isPublic=" + isPublic +
                '}';
    }
}