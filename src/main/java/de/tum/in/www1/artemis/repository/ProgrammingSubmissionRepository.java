package de.tum.in.www1.artemis.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import de.tum.in.www1.artemis.domain.ProgrammingSubmission;

/**
 * Spring Data JPA repository for the ProgrammingSubmission entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ProgrammingSubmissionRepository extends JpaRepository<ProgrammingSubmission, Long> {

    @EntityGraph(attributePaths = { "result.feedbacks" })
    ProgrammingSubmission findFirstByParticipationIdAndCommitHash(Long participationId, String commitHash);

    @EntityGraph(attributePaths = "result")
    Optional<ProgrammingSubmission> findFirstByParticipationIdOrderBySubmissionDateDesc(Long participationId);

    @EntityGraph(attributePaths = "result")
    @Query("select s from Submission s left join s.participation p left join p.exercise e where p.id = :#{#participationId} and (s.type = 'INSTRUCTOR' or s.type = 'TEST' or (e.dueDate is null or s.submissionDate <= e.dueDate)) order by s.submissionDate desc")
    List<ProgrammingSubmission> findGradedByParticipationAIdOrderBySubmissionDateDesc(long participationId, Pageable pageable);

    @EntityGraph(attributePaths = { "result.feedbacks" })
    List<ProgrammingSubmission> findByParticipationIdAndResultIsNullOrderBySubmissionDateDesc(Long participationId);

    @EntityGraph(attributePaths = "result")
    @Query("select distinct s from Submission s where s.id = :#{#submissionId}")
    ProgrammingSubmission findByIdWithEagerResult(@Param("submissionId") Long submissionId);

    Optional<ProgrammingSubmission> findByResultId(long resultId);
}
