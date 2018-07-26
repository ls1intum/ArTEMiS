package de.tum.in.www1.artemis.repository;

import de.tum.in.www1.artemis.domain.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


/**
 * Spring Data JPA repository for the Result entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {

    List<Result> findByParticipationIdOrderByCompletionDateDesc(Long participationId);

    List<Result> findByParticipationIdAndRatedOrderByCompletionDateDesc(Long participationId, boolean rated);

    List<Result> findByParticipationExerciseIdOrderByCompletionDateAsc(Long exerciseId);

    @Query("select r from Result r where r.completionDate = (select max(rr.completionDate) from Result rr where rr.participation.exercise.id = :exerciseId and rr.participation.student.id = r.participation.student.id) and r.participation.exercise.id = :exerciseId order by r.completionDate asc")
    List<Result> findLatestResultsForExercise(@Param("exerciseId") Long exerciseId);

//    @Query("select r from Result r where r.completionDate = (select min(rr.completionDate) from Result rr where rr.participation.exercise.id = :exerciseId and rr.participation.student.id = r.participation.student.id and rr.successful = true) and r.participation.exercise.id = :exerciseId and r.successful = true order by r.completionDate asc")
//    List<Result> findEarliestSuccessfulResultsForExercise(@Param("exerciseId") Long exerciseId);

    @Query("select r from Result r where r.completionDate = (select min(rr.completionDate) from Result rr where rr.participation.exercise.id = r.participation.exercise.id and rr.participation.student.id = r.participation.student.id and rr.successful = true) and r.participation.exercise.course.id = :courseId and r.successful = true order by r.completionDate asc")
    List<Result> findEarliestSuccessfulResultsForCourse(@Param("courseId") Long courseId);


    /**
     * Custom query that counts the number of results for each participation of a particular exercise
     * @param exerciseId id of exercise for which the number of results in participations is aggregated
     * @return list of object arrays, where each object array contains two Long values, participation id (index 0) and
     * number of results for this participation (index 1)
     */
    @Query("select participation.id, count(id) from Result where participation.exercise.id = :exerciseId group by participation.id")
    List<Object[]> findSubmissionCountsForStudents(@Param("exerciseId") Long exerciseId);

    List<Result> findByParticipationExerciseIdAndSuccessfulOrderByCompletionDateAsc(Long exerciseId, boolean successful);

    Optional<Result> findFirstByParticipationIdOrderByCompletionDateDesc(Long participationId);

    Optional<Result> findFirstByParticipationIdAndRatedOrderByCompletionDateDesc(Long participationId, boolean rated);

    Optional<Result> findDistinctBySubmissionId(Long submissionId);

    /**
     * This SQL query is used for inserting results if only one unrated result should exist per participation.
     * Specifically this prevents multiple concurrent inserts with the same participation_id and rated = 0.
     *
     * @param participationId   the participation id for which the result should be inserted
     */
    @Modifying
    @Query(value = "insert into Result (participation_id, rated) select :participationId, 0 from Result where (participation_id = :participationId and rated = 0) having count(*) = 0", nativeQuery = true)
    void insertIfNonExisting(@Param("participationId") Long participationId);
}
