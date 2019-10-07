package de.tum.in.www1.artemis.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import de.tum.in.www1.artemis.domain.TutorGroup;

/**
 * Spring Data repository for the TutorGroup entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TutorGroupRepository extends JpaRepository<TutorGroup, Long> {

    @Query(value = "select distinct tutor_group from TutorGroup tutor_group left join fetch tutor_group.students", countQuery = "select count(distinct tutor_group) from TutorGroup tutor_group")
    Page<TutorGroup> findAllWithEagerRelationships(Pageable pageable);

    @Query(value = "select distinct tutor_group from TutorGroup tutor_group left join fetch tutor_group.students")
    List<TutorGroup> findAllWithEagerRelationships();

    @Query("select tutor_group from TutorGroup tutor_group left join fetch tutor_group.students where tutor_group.id =:id")
    Optional<TutorGroup> findOneWithEagerRelationships(@Param("id") Long id);

}
