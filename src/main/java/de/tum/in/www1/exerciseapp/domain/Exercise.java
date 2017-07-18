package de.tum.in.www1.exerciseapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.tum.in.www1.exerciseapp.domain.enumeration.ExerciseType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

/**
 * A Exercise.
 */
@Entity
@Table(name = "exercise")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Exercise implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "base_repository_url")
    private String baseRepositoryUrl;

    @Column(name = "base_build_plan_id")
    private String baseBuildPlanId;

    @Column(name = "publish_build_plan_url")
    private Boolean publishBuildPlanUrl;

    @Column(name = "release_date")
    private ZonedDateTime releaseDate;

    @Column(name = "due_date")
    private ZonedDateTime dueDate;

    @Column(name = "allow_online_editor")
    private Boolean allowOnlineEditor;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_type")
    private ExerciseType exerciseType;

    @ManyToOne
    private Course course;

    @OneToMany(mappedBy = "exercise")
    @JsonIgnore
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Participation> participations = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBaseRepositoryUrl() {
        return baseRepositoryUrl;
    }

    public void setBaseRepositoryUrl(String baseRepositoryUrl) {
        this.baseRepositoryUrl = baseRepositoryUrl;
    }

    public String getBaseBuildPlanId() {
        return baseBuildPlanId;
    }

    public void setBaseBuildPlanId(String baseBuildPlanId) {
        this.baseBuildPlanId = baseBuildPlanId;
    }

    public Boolean isPublishBuildPlanUrl() {
        return publishBuildPlanUrl;
    }

    public void setPublishBuildPlanUrl(Boolean publishBuildPlanUrl) {
        this.publishBuildPlanUrl = publishBuildPlanUrl;
    }

    public ZonedDateTime getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(ZonedDateTime releaseDate) {
        this.releaseDate = releaseDate;
    }

    public ZonedDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(ZonedDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public Boolean isAllowOnlineEditor() {
        return allowOnlineEditor;
    }

    public void setAllowOnlineEditor(Boolean allowOnlineEditor) {
        this.allowOnlineEditor = allowOnlineEditor;
    }

    public ExerciseType getExerciseType() {
        return exerciseType;
    }

    public void setExerciseType(ExerciseType exerciseType) {
        this.exerciseType = exerciseType;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Set<Participation> getParticipations() {
        return participations;
    }

    public void setParticipations(Set<Participation> participations) {
        this.participations = participations;
    }

    public URL getBaseRepositoryUrlAsUrl() {
        try {
            return new URL(baseRepositoryUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Exercise exercise = (Exercise) o;
        if(exercise.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, exercise.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Exercise{" +
            "id=" + id +
            ", title='" + title + "'" +
            ", baseRepositoryUrl='" + baseRepositoryUrl + "'" +
            ", baseBuildPlanId='" + baseBuildPlanId + "'" +
            ", publishBuildPlanUrl='" + publishBuildPlanUrl + "'" +
            ", releaseDate='" + releaseDate + "'" +
            ", dueDate='" + dueDate + "'" +
            ", allowOnlineEditor='" + allowOnlineEditor + "'" +
            '}';
    }
}
