package de.tum.in.www1.artemis.repository;

import static org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.LOAD;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.tum.in.www1.artemis.domain.exam.StudentExam;

/**
 * Spring Data JPA repository for the StudentExam entity.
 */
@Repository
public interface StudentExamRepository extends JpaRepository<StudentExam, Long> {

    @EntityGraph(type = LOAD, attributePaths = { "exercises" })
    Optional<StudentExam> findWithExercisesById(Long id);

    @EntityGraph(type = LOAD, attributePaths = { "exercises", "exercises.studentParticipations", "exercises.studentParticipations.submissions" })
    Optional<StudentExam> findWithExercisesAndStudentParticipationsAndSubmissionsById(Long id);

    List<StudentExam> findByExamId(Long examId);

}
