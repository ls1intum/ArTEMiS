package de.tum.in.www1.exerciseapp.repository;

import de.tum.in.www1.exerciseapp.domain.DragAndDropQuestionStatistic;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.*;


/**
 * Spring Data JPA repository for the DragAndDropQuestionStatistic entity.
 */
@SuppressWarnings("unused")
@Repository
public interface DragAndDropQuestionStatisticRepository extends JpaRepository<DragAndDropQuestionStatistic,Long> {
    
}
