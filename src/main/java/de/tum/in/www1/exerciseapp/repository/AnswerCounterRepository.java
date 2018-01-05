package de.tum.in.www1.exerciseapp.repository;

import de.tum.in.www1.exerciseapp.domain.AnswerCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * Spring Data JPA repository for the AnswerCounter entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AnswerCounterRepository extends JpaRepository<AnswerCounter,Long> {

}
