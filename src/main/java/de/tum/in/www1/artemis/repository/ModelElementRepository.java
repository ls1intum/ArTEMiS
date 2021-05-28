package de.tum.in.www1.artemis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.tum.in.www1.artemis.domain.modeling.ModelElement;

/**
 * Spring Data JPA repository for the ModelElement entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ModelElementRepository extends JpaRepository<ModelElement, Long> {

}
