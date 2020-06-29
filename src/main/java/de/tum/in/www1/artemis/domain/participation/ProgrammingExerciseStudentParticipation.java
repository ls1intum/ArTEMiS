package de.tum.in.www1.artemis.domain.participation;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

import de.tum.in.www1.artemis.domain.Exercise;
import de.tum.in.www1.artemis.domain.FileType;
import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import de.tum.in.www1.artemis.domain.view.QuizView;

@Entity
@DiscriminatorValue(value = "PESP")
public class ProgrammingExerciseStudentParticipation extends StudentParticipation implements ProgrammingExerciseParticipation {

    private static final long serialVersionUID = 1L;

    @Column(name = "repository_url")
    @JsonView(QuizView.Before.class)
    private String repositoryUrl;

    @Column(name = "build_plan_id")
    @JsonView(QuizView.Before.class)
    private String buildPlanId;

    @Transient
    private List<ProgrammingExerciseRepositoryFile> repositoryFiles = new ArrayList<>();

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public Participation repositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
        return this;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getBuildPlanId() {
        return buildPlanId;
    }

    public Participation buildPlanId(String buildPlanId) {
        this.buildPlanId = buildPlanId;
        return this;
    }

    public void setBuildPlanId(String buildPlanId) {
        this.buildPlanId = buildPlanId;
    }

    /**
     * @return the repository URL as an URL Object
     */
    @JsonIgnore
    public URL getRepositoryUrlAsUrl() {
        if (repositoryUrl == null) {
            return null;
        }

        try {
            return new URL(repositoryUrl);
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    @JsonIgnore
    // TODO: this is a helper method to avoid casts in other classes that want to access the underlying exercise
    public ProgrammingExercise getProgrammingExercise() {
        Exercise exercise = getExercise();
        if (exercise instanceof ProgrammingExercise) { // this should always be the case except exercise is null
            return (ProgrammingExercise) exercise;
        }
        else {
            return null;
        }
    }

    @Override
    public void setProgrammingExercise(ProgrammingExercise programmingExercise) {
        setExercise(programmingExercise);
    }

    @Override
    public String toString() {
        return "Participation{" + "id=" + getId() + ", repositoryUrl='" + getRepositoryUrl() + "'" + ", buildPlanId='" + getBuildPlanId() + "'" + ", initializationState='"
                + getInitializationState() + "'" + ", initializationDate='" + getInitializationDate() + "'" + ", presentationScore=" + getPresentationScore() + "}";
    }

    @Override
    public Participation copyParticipationId() {
        var participation = new ProgrammingExerciseStudentParticipation();
        participation.setId(getId());
        return participation;
    }

    @JsonGetter(value = "repositoryFiles")
    public List<ProgrammingExerciseRepositoryFile> getRepositoryFiles() {
        return repositoryFiles;
    }

    public void setRepositoryFiles(List<ProgrammingExerciseRepositoryFile> repositoryFiles) {
        this.repositoryFiles = repositoryFiles;
    }

    public static class ProgrammingExerciseRepositoryFile {

        private String filename;

        private FileType fileType;

        private String fileContent;

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public FileType getFileType() {
            return fileType;
        }

        public void setFileType(FileType fileType) {
            this.fileType = fileType;
        }

        public String getFileContent() {
            return fileContent;
        }

        public void setFileContent(String fileContent) {
            this.fileContent = fileContent;
        }
    }
}
