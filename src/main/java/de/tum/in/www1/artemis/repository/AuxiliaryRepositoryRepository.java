package de.tum.in.www1.artemis.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import de.tum.in.www1.artemis.domain.AuxiliaryRepository;

/**
 * Spring Data repository for the AuxiliaryRepository entity.
 */
@Repository
public interface AuxiliaryRepositoryRepository extends JpaRepository<AuxiliaryRepository, Long> {

    List<AuxiliaryRepository> findByExerciseId(Long exerciseId);

}
