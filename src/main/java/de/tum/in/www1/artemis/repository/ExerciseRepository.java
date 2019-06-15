package de.tum.in.www1.artemis.repository;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import de.tum.in.www1.artemis.domain.Exercise;

/**
 * Spring Data JPA repository for the Exercise entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    List<Exercise> findByCourseId(@Param("courseId") Long courseId);

    /**
     * Select Exercise for Course ID WHERE there does exist an LtiOutcomeUrl for the current user (-> user has started exercise once using LTI)
     */
    @Query("select e from Exercise e where e.course.id = :#{#courseId} and exists (select l from LtiOutcomeUrl l where e = l.exercise and l.user.login = :#{#principal.name})")
    List<Exercise> findByCourseIdWhereLtiOutcomeUrlExists(@Param("courseId") Long courseId, @Param("principal") Principal principal);

    @Query("select e from Exercise e where e.course.id =  :#{#courseId}")
    List<Exercise> findAllByCourseId(@Param("courseId") Long courseId);

    @Query("select distinct exercise from Exercise exercise left join fetch exercise.participations where exercise.id = :#{#exerciseId}")
    Optional<Exercise> findByIdWithEagerParticipations(@Param("exerciseId") Long exerciseId);
}
