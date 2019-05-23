package de.tum.in.www1.artemis.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import de.tum.in.www1.artemis.domain.Course;

/**
 * Spring Data JPA repository for the Course entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("select distinct course from Course course where (course.startDate <= current_timestamp or course.startDate is null) and (course.endDate >= current_timestamp or course.endDate is null)")
    List<Course> findAllActive();

    @Query("select distinct course from Course course left join fetch course.exercises where (course.startDate <= current_timestamp or course.startDate is null) and (course.endDate >= current_timestamp or course.endDate is null)")
    List<Course> findAllActiveWithEagerExercises();

    @Query("select distinct course from Course course left join fetch course.exercises where course.id = :#{#courseId}")
    Course findOneWithEagerExercises(@Param("courseId") Long courseId);

    @Query("select distinct course from Course course where course.startDate <= current_timestamp and course.endDate >= current_timestamp and course.onlineCourse = 0 and course.registrationEnabled = 1")
    List<Course> findAllCurrentlyActiveAndNotOnlineAndEnabled();
}
