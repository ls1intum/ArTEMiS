package de.tum.in.www1.artemis.domain.exam;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.*;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import de.tum.in.www1.artemis.domain.Course;
import de.tum.in.www1.artemis.domain.DomainObject;
import de.tum.in.www1.artemis.domain.User;

@Entity
@Table(name = "exam")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Exam extends DomainObject {

    @Column(name = "title", nullable = false)
    private String title;

    /**
     * student can see the exam in the UI from {@link #visibleDate} date onwards
     */
    @Column(name = "visible_date")
    private ZonedDateTime visibleDate;

    /**
     * student can start working on exam from {@link #startDate}
     */
    @Column(name = "start_date")
    private ZonedDateTime startDate;

    /**
     * student can work on exam until {@link #endDate}
     */
    @Column(name = "end_date")
    private ZonedDateTime endDate;

    @Column(name = "publish_results_date")
    private ZonedDateTime publishResultsDate;

    @Column(name = "exam_student_review_start")
    private ZonedDateTime examStudentReviewStart;

    @Column(name = "exam_student_review_end")
    private ZonedDateTime examStudentReviewEnd;

    /**
     * The duration in which the students can do final submissions before the exam ends in seconds
     */
    @Column(name = "grace_period", columnDefinition = "integer default 180")
    private Integer gracePeriod = 180;

    @Column(name = "start_text")
    @Lob
    private String startText;

    @Column(name = "end_text")
    @Lob
    private String endText;

    @Column(name = "confirmation_start_text")
    @Lob
    private String confirmationStartText;

    @Column(name = "confirmation_end_text")
    @Lob
    private String confirmationEndText;

    @Column(name = "max_points")
    private Integer maxPoints;

    @Column(name = "randomize_exercise_order")
    private Boolean randomizeExerciseOrder;

    /**
     * From all exercise groups connected to the exam, {@link #numberOfExercisesInExam} are randomly
     * chosen when generating the specific exam for the {@link #registeredUsers}
     */
    @Column(name = "number_of_exercises_in_exam")
    private Integer numberOfExercisesInExam;

    @Column(name = "number_of_correction_rounds", columnDefinition = "integer default 1")
    private Integer numberOfCorrectionRoundsInExam;

    @Column(name = "examiner")
    private String examiner;

    @Column(name = "module_number")
    private String moduleNumber;

    @Column(name = "course_name")
    private String courseName;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderColumn(name = "exercise_group_order")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @JsonIgnoreProperties(value = "exam", allowSetters = true)
    private List<ExerciseGroup> exerciseGroups = new ArrayList<>();

    @OneToMany(mappedBy = "exam", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @JsonIgnoreProperties("exam")
    private Set<StudentExam> studentExams = new HashSet<>();

    // Unidirectional
    @ManyToMany
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @JoinTable(name = "exam_user", joinColumns = @JoinColumn(name = "exam_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "student_id", referencedColumnName = "id"))
    private Set<User> registeredUsers = new HashSet<>();

    @Transient
    private Long numberOfRegisteredUsersTransient;

    @Transient
    private Long numberOfGeneratedStudentExamsTransient;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ZonedDateTime getVisibleDate() {
        return visibleDate;
    }

    public void setVisibleDate(ZonedDateTime visibleDate) {
        this.visibleDate = visibleDate;
    }

    public ZonedDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(ZonedDateTime startDate) {
        this.startDate = startDate;
    }

    public ZonedDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(ZonedDateTime endDate) {
        this.endDate = endDate;
    }

    public ZonedDateTime getPublishResultsDate() {
        return publishResultsDate;
    }

    public void setPublishResultsDate(ZonedDateTime publishResultsDate) {
        this.publishResultsDate = publishResultsDate;
    }

    public ZonedDateTime getExamStudentReviewStart() {
        return examStudentReviewStart;
    }

    public void setExamStudentReviewStart(ZonedDateTime examStudentReviewStart) {
        this.examStudentReviewStart = examStudentReviewStart;
    }

    public ZonedDateTime getExamStudentReviewEnd() {
        return examStudentReviewEnd;
    }

    public void setExamStudentReviewEnd(ZonedDateTime examStudentReviewEnd) {
        this.examStudentReviewEnd = examStudentReviewEnd;
    }

    public Integer getGracePeriod() {
        return gracePeriod;
    }

    public void setGracePeriod(Integer gracePeriod) {
        this.gracePeriod = gracePeriod;
    }

    public String getStartText() {
        return startText;
    }

    public void setStartText(String startText) {
        this.startText = startText;
    }

    public String getEndText() {
        return endText;
    }

    public void setEndText(String endText) {
        this.endText = endText;
    }

    public String getConfirmationStartText() {
        return confirmationStartText;
    }

    public void setConfirmationStartText(String confirmationStartText) {
        this.confirmationStartText = confirmationStartText;
    }

    public String getConfirmationEndText() {
        return confirmationEndText;
    }

    public void setConfirmationEndText(String confirmationEndText) {
        this.confirmationEndText = confirmationEndText;
    }

    public Integer getMaxPoints() {
        return this.maxPoints;
    }

    public void setMaxPoints(Integer maxPoints) {
        this.maxPoints = maxPoints;
    }

    public Integer getNumberOfExercisesInExam() {
        return numberOfExercisesInExam;
    }

    public void setNumberOfExercisesInExam(Integer numberOfExercisesInExam) {
        this.numberOfExercisesInExam = numberOfExercisesInExam;
    }

    public Integer getNumberOfCorrectionRoundsInExam() {
        return this.numberOfCorrectionRoundsInExam != null ? this.numberOfCorrectionRoundsInExam : 1;
    }

    public void setNumberOfCorrectionRoundsInExam(Integer numberOfCorrectionRoundsInExam) {
        this.numberOfCorrectionRoundsInExam = numberOfCorrectionRoundsInExam;
    }

    public Boolean getRandomizeExerciseOrder() {
        return randomizeExerciseOrder;
    }

    public void setRandomizeExerciseOrder(Boolean randomizeExerciseOrder) {
        this.randomizeExerciseOrder = randomizeExerciseOrder;
    }

    public String getExaminer() {
        return examiner;
    }

    public void setExaminer(String examiner) {
        this.examiner = examiner;
    }

    public String getModuleNumber() {
        return moduleNumber;
    }

    public void setModuleNumber(String moduleNumber) {
        this.moduleNumber = moduleNumber;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public List<ExerciseGroup> getExerciseGroups() {
        return exerciseGroups;
    }

    public void setExerciseGroups(List<ExerciseGroup> exerciseGroups) {
        this.exerciseGroups = exerciseGroups;
    }

    public Exam addExerciseGroup(ExerciseGroup exerciseGroup) {
        this.exerciseGroups.add(exerciseGroup);
        exerciseGroup.setExam(this);
        return this;
    }

    public Exam removeExerciseGroup(ExerciseGroup exerciseGroup) {
        this.exerciseGroups.remove(exerciseGroup);
        exerciseGroup.setExam(null);
        return this;
    }

    public Set<StudentExam> getStudentExams() {
        return studentExams;
    }

    public void setStudentExams(Set<StudentExam> studentExams) {
        this.studentExams = studentExams;
    }

    public Exam addStudentExam(StudentExam studentExam) {
        this.studentExams.add(studentExam);
        studentExam.setExam(this);
        return this;
    }

    public Exam removeStudentExam(StudentExam studentExam) {
        this.studentExams.remove(studentExam);
        studentExam.setExam(null);
        return this;
    }

    public Set<User> getRegisteredUsers() {
        return registeredUsers;
    }

    public void setRegisteredUsers(Set<User> registeredUsers) {
        this.registeredUsers = registeredUsers;
    }

    public Exam addRegisteredUser(User user) {
        this.registeredUsers.add(user);
        return this;
    }

    public Exam removeRegisteredUser(User user) {
        this.registeredUsers.remove(user);
        return this;
    }

    public Long getNumberOfRegisteredUsers() {
        return this.numberOfRegisteredUsersTransient;
    }

    public void setNumberOfRegisteredUsers(Long numberOfRegisteredUsers) {
        this.numberOfRegisteredUsersTransient = numberOfRegisteredUsers;
    }

    public Long getNumberOfGeneratedStudentExams() {
        return this.numberOfGeneratedStudentExamsTransient;
    }

    public void setNumberOfGeneratedStudentExams(Long numberOfGeneratedStudentExams) {
        this.numberOfGeneratedStudentExamsTransient = numberOfGeneratedStudentExams;
    }

    /**
     * check if students are allowed to see this exam
     *
     * @return true, if students are allowed to see this exam, otherwise false, null if this cannot be determined
     */
    public Boolean isVisibleToStudents() {
        if (visibleDate == null) {  // no visible date means the exam is configured wrongly and should not be visible!
            return null;
        }
        return visibleDate.isBefore(ZonedDateTime.now());
    }

    /**
     * check if the exam has started
     *
     * @return true, if the exam has started, otherwise false, null if this cannot be determined
     */
    public Boolean isStarted() {
        if (startDate == null) {   // no start date means the exam is configured wrongly and we cannot answer the question!
            return null;
        }
        return startDate.isBefore(ZonedDateTime.now());
    }

    /**
     * check if results of exam are published
     *
     * @return true, if the results are published, false if not published or not set!
     */
    public Boolean resultsPublished() {
        if (publishResultsDate == null) {
            return false;
        }
        return publishResultsDate.isBefore(ZonedDateTime.now());
    }
}
