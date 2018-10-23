package de.tum.in.www1.artemis.repository;

import de.tum.in.www1.artemis.domain.Participation;
import de.tum.in.www1.artemis.domain.enumeration.InitializationState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the Participation entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    List<Participation> findByExerciseId(@Param("exerciseId") Long exerciseId);

    @Query("select p from Participation p where p.exercise.course.id = :courseId")
    List<Participation> findByCourseId(@Param("courseId") Long courseId);

    Participation findOneByExerciseIdAndStudentLogin(Long exerciseId, String username);

    Participation findOneByExerciseIdAndStudentLoginAndInitializationState(Long exerciseId, String username, InitializationState state);

    List<Participation> findByBuildPlanIdAndInitializationState(String buildPlanId, InitializationState state);

    @Query("select participation from Participation participation where participation.student.login = ?#{principal.username}")
    List<Participation> findByStudentIsCurrentUser();

    @Query("select distinct participation from Participation participation left join fetch participation.results where participation.student.login = :#{#username}")
    List<Participation> findByStudentUsernameWithEagerResults(@Param("username") String username);

    @Query("select distinct participation from Participation participation left join fetch participation.results where participation.exercise.id = :#{#exerciseId}")
    List<Participation> findByExerciseIdWithEagerResults(@Param("exerciseId") Long exerciseId);

    @Query("select distinct participation from Participation participation left join fetch participation.results where participation.id = :#{#participationId}")
    Participation findByIdWithEagerResults(@Param("participationId") Long participationId);
}
