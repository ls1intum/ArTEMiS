package de.tum.in.www1.artemis.domain;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.io.Serializable;
import java.util.Objects;

/**
 * A ProgrammingSubmission.
 */
@Entity
@DiscriminatorValue(value="P")
public class ProgrammingSubmission extends Submission implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "commit_hash")
    private String commitHash;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove

    public String getCommitHash() {
        return commitHash;
    }

    public ProgrammingSubmission commitHash(String commitHash) {
        this.commitHash = commitHash;
        return this;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
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
        ProgrammingSubmission programmingSubmission = (ProgrammingSubmission) o;
        if (programmingSubmission.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), programmingSubmission.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "ProgrammingSubmission{" +
            "id=" + getId() +
            ", commitHash='" + getCommitHash() + "'" +
            "}";
    }
}
