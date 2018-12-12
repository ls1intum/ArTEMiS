package de.tum.in.www1.artemis.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.tum.in.www1.artemis.domain.enumeration.ProgrammingLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

/**
 * A ProgrammingExercise.
 */
@Entity
@DiscriminatorValue(value="P")
public class ProgrammingExercise extends Exercise implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(ProgrammingExercise.class);

    private static final long serialVersionUID = 1L;

    @Column(name = "base_repository_url")
    private String baseRepositoryUrl;

    @Column(name = "solution_repository_url")
    private String solutionRepositoryUrl;

    @Column(name = "test_repository_url")
    private String testRepositoryUrl;

    @Column(name = "base_build_plan_id")
    private String baseBuildPlanId;

    @Column(name = "solution_build_plan_id")
    private String solutionBuildPlanId;

    @Column(name = "publish_build_plan_url")
    private Boolean publishBuildPlanUrl;

    @Column(name = "allow_online_editor")
    private Boolean allowOnlineEditor;

    @Enumerated(EnumType.STRING)
    @Column(name = "programming_language")
    private ProgrammingLanguage programmingLanguage;

    @Column(name = "package_name")
    private String packageName;

    // jhipster-needle-entity-add-field - Jhipster will add fields here, do not remove
    public String getBaseRepositoryUrl() {
        return baseRepositoryUrl;
    }

    public ProgrammingExercise baseRepositoryUrl(String baseRepositoryUrl) {
        this.baseRepositoryUrl = baseRepositoryUrl;
        return this;
    }

    public void setBaseRepositoryUrl(String baseRepositoryUrl) {
        this.baseRepositoryUrl = baseRepositoryUrl;
    }

    public String getSolutionRepositoryUrl() {
        return solutionRepositoryUrl;
    }

    public ProgrammingExercise solutionRepositoryUrl(String solutionRepositoryUrl) {
        this.solutionRepositoryUrl = solutionRepositoryUrl;
        return this;
    }

    public void setTestRepositoryUrl(String testRepositoryUrl) {
        this.testRepositoryUrl = testRepositoryUrl;
    }

    public String getTestRepositoryUrl() {
        return testRepositoryUrl;
    }

    public ProgrammingExercise testRepositoryUrl(String testRepositoryUrl) {
        this.testRepositoryUrl = testRepositoryUrl;
        return this;
    }

    public void setSolutionRepositoryUrl(String solutionRepositoryUrl) {
        this.solutionRepositoryUrl = solutionRepositoryUrl;
    }

    public String getBaseBuildPlanId() {
        return baseBuildPlanId;
    }

    public ProgrammingExercise baseBuildPlanId(String baseBuildPlanId) {
        this.baseBuildPlanId = baseBuildPlanId;
        return this;
    }

    public void setBaseBuildPlanId(String baseBuildPlanId) {
        this.baseBuildPlanId = baseBuildPlanId;
    }

    public String getSolutionBuildPlanId() {
        return solutionBuildPlanId;
    }

    public ProgrammingExercise solutionBuildPlanId(String solutionBuildPlanId) {
        this.solutionBuildPlanId = solutionBuildPlanId;
        return this;
    }

    public void setSolutionBuildPlanId(String solutionBuildPlanId) {
        this.solutionBuildPlanId = solutionBuildPlanId;
    }

    public Boolean isPublishBuildPlanUrl() {
        return publishBuildPlanUrl;
    }

    public ProgrammingExercise publishBuildPlanUrl(Boolean publishBuildPlanUrl) {
        this.publishBuildPlanUrl = publishBuildPlanUrl;
        return this;
    }

    public void setPublishBuildPlanUrl(Boolean publishBuildPlanUrl) {
        this.publishBuildPlanUrl = publishBuildPlanUrl;
    }

    public Boolean isAllowOnlineEditor() {
        return allowOnlineEditor;
    }

    public ProgrammingExercise allowOnlineEditor(Boolean allowOnlineEditor) {
        this.allowOnlineEditor = allowOnlineEditor;
        return this;
    }

    public void setAllowOnlineEditor(Boolean allowOnlineEditor) {
        this.allowOnlineEditor = allowOnlineEditor;
    }

    public ProgrammingLanguage getProgrammingLanguage() {
        return programmingLanguage;
    }

    public ProgrammingExercise programmingLanguage(ProgrammingLanguage programmingLanguage) {
        this.programmingLanguage = programmingLanguage;
        return this;
    }

    public void setProgrammingLanguage(ProgrammingLanguage programmingLanguage) {
        this.programmingLanguage = programmingLanguage;
    }

    public String getPackageName() {
        return packageName;
    }

    public ProgrammingExercise packageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    // jhipster-needle-entity-add-getters-setters - Jhipster will add getters and setters here, do not remove

    public URL getBaseRepositoryUrlAsUrl() {
        if (baseRepositoryUrl == null || baseRepositoryUrl.isEmpty()) {
            return null;
        }
        try {
            return new URL(baseRepositoryUrl);
        } catch (MalformedURLException e) {
            log.warn("Cannot create URL for baseRepositoryUrl: " + baseRepositoryUrl + " due to the following error: " + e.getMessage());
        }
        return null;
    }

    public URL getSolutionRepositoryUrlAsUrl() {
        if (solutionRepositoryUrl == null || solutionRepositoryUrl.isEmpty()) {
            return null;
        }
        try {
            return new URL(solutionRepositoryUrl);
        } catch (MalformedURLException e) {
            log.warn("Cannot create URL for solutionRepositoryUrl: " + solutionRepositoryUrl + " due to the following error: " + e.getMessage());
        }
        return null;
    }

    public URL getTestRepositoryUrlAsUrl() {
        if (testRepositoryUrl == null || testRepositoryUrl.isEmpty()) {
            return null;
        }
        try {
            return new URL(testRepositoryUrl);
        } catch (MalformedURLException e) {
            log.warn("Cannot create URL for testRepositoryUrl: " + testRepositoryUrl + " due to the following error: " + e.getMessage());
        }
        return null;
    }

    @JsonIgnore
    public String getProjectKey() {
        //this is the key used for Bitbucket and Bamboo
        //remove all whitespace and make sure it is upper case
        return (this.getCourse().getShortName() + this.getShortName()).toUpperCase().replaceAll("\\s+","");
    }

    @JsonIgnore
    public String getProjectName() {
        //this is the name used for Bitbucket and Bamboo
        return this.getCourse().getShortName() + " " + this.getTitle();
    }

    @JsonIgnore
    public String getPackageFolderName() {
        return getPackageName().replace(".", "/");
    }

    /**
     * set all sensitive information to null, so no info with respect to the solution gets leaked to students through json
     */
    @Override
    public void filterSensitiveInformation() {
        setBaseRepositoryUrl(null);
        setSolutionRepositoryUrl(null);
        setTestRepositoryUrl(null);
        setBaseBuildPlanId(null);
        setSolutionBuildPlanId(null);
        super.filterSensitiveInformation();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProgrammingExercise programmingExercise = (ProgrammingExercise) o;
        if (programmingExercise.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), programmingExercise.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "ProgrammingExercise{" +
            "id=" + getId() +
            ", baseRepositoryUrl='" + getBaseRepositoryUrl() + "'" +
            ", solutionRepositoryUrl='" + getSolutionRepositoryUrl() + "'" +
            ", baseBuildPlanId='" + getBaseBuildPlanId() + "'" +
            ", solutionBuildPlanId='" + getSolutionBuildPlanId() + "'" +
            ", publishBuildPlanUrl='" + isPublishBuildPlanUrl() + "'" +
            ", allowOnlineEditor='" + isAllowOnlineEditor() + "'" +
            ", programmingLanguage='" + getProgrammingLanguage() + "'" +
            ", packageName='" + getPackageName() + "'" +
            "}";
    }
}
