package de.tum.in.www1.artemis.repository;

import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * Spring Data JPA repository for the ProgrammingExercise entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ProgrammingExerciseRepository extends JpaRepository<ProgrammingExercise, Long> {

    @Query("select e from ProgrammingExercise e where e.course.id = :#{#courseId}")
    List<ProgrammingExercise> findByCourseId(@Param("courseId") Long courseId);

    @Query("select distinct p from ProgrammingExercise as p left join fetch p.participations")
    List<ProgrammingExercise> findAllWithEagerParticipations();

    ProgrammingExercise findOneByTemplateParticipationId(Long templateParticipationId);

    ProgrammingExercise findOneBySolutionParticipationId(Long solutionParticipationId);
}
