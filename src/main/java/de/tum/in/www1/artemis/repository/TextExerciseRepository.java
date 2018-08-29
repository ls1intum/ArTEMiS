package de.tum.in.www1.artemis.repository;

import de.tum.in.www1.artemis.domain.TextExercise;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.*;


/**
 * Spring Data JPA repository for the TextExercise entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TextExerciseRepository extends JpaRepository<TextExercise, Long> {

}
