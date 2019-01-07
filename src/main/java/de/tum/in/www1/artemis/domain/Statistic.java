package de.tum.in.www1.artemis.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A Statistic.
 */
@Entity
@Table(name = "statistic")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
    name="discriminator",
    discriminatorType=DiscriminatorType.STRING
)
@DiscriminatorValue(value="S")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public abstract class Statistic implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "participants_rated")
    private Integer participantsRated = 0;

    @Column(name = "participants_unrated")
    private Integer participantsUnrated = 0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getParticipantsRated() {
        return participantsRated;
    }

    public Statistic participantsRated(Integer participantsRated) {
        this.participantsRated = participantsRated;
        return this;
    }

    public void setParticipantsRated(Integer participantsRated) {
        this.participantsRated = participantsRated;
    }

    public Integer getParticipantsUnrated() {
        return participantsUnrated;
    }

    public Statistic participantsUnrated(Integer participantsUnrated) {
        this.participantsUnrated = participantsUnrated;
        return this;
    }

    public void setParticipantsUnrated(Integer participantsUnrated) {
        this.participantsUnrated = participantsUnrated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Statistic statistic = (Statistic) o;
        if (statistic.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), statistic.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Statistic{" +
            "id=" + getId() +
            ", participantsRated='" + getParticipantsRated() + "'" +
            ", participantsUnrated='" + getParticipantsUnrated() + "'" +
            "}";
    }
}
