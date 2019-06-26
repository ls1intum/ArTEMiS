package de.tum.in.www1.artemis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import de.tum.in.www1.artemis.domain.Course;
import de.tum.in.www1.artemis.repository.CourseRepository;
import de.tum.in.www1.artemis.util.DatabaseUtilService;
import de.tum.in.www1.artemis.util.ModelFactory;
import de.tum.in.www1.artemis.util.RequestUtilService;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@ActiveProfiles("artemis")
public class CourseIntegrationTest {

    @Autowired
    DatabaseUtilService database;

    @Autowired
    RequestUtilService request;

    @Autowired
    CourseRepository courseRepo;

    @Before
    public void resetDatabase() {
        database.resetDatabase();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void createCourseWithPermission() throws Exception {
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>());
        request.post("/api/courses", course, HttpStatus.CREATED);
        List<Course> repoContent = courseRepo.findAll();
        assertThat(repoContent.size()).as("Course got stored").isEqualTo(1);

        course = ModelFactory.generateCourse(1L, null, null, new HashSet<>());
        request.post("/api/courses", course, HttpStatus.BAD_REQUEST);
        assertThat(courseRepo.findAll()).as("Course has not been stored").contains(repoContent.toArray(new Course[0]));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void createCourseWithoutPermission() throws Exception {
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>());
        request.post("/api/courses", course, HttpStatus.FORBIDDEN);
        assertThat(courseRepo.findAll().size()).as("Course got stored").isEqualTo(0);
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getCourseWithoutPermission() throws Exception {
        request.getList("/api/courses", HttpStatus.FORBIDDEN, Course.class);
    }

    public void loadInitialCourses() {
        Course course = ModelFactory.generateCourse(1L, null, null, new HashSet<>());
        assertThat(courseRepo.findAll()).as("course repo got initialized correctly").contains(course);
    }
}
