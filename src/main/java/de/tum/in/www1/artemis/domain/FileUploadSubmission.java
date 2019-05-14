package de.tum.in.www1.artemis.domain;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * A FileUploadSubmission.
 */
@Entity
@DiscriminatorValue(value = "F")
public class FileUploadSubmission extends Submission implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "file_path")
    private String filePath;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove

    public String getFilePath() {
        return filePath;
    }

    public FileUploadSubmission filePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here, do not remove

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileUploadSubmission fileUploadSubmission = (FileUploadSubmission) o;
        if (fileUploadSubmission.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), fileUploadSubmission.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "FileUploadSubmission{" + "id=" + getId() + ", filePath='" + getFilePath() + "'" + "}";
    }
}
