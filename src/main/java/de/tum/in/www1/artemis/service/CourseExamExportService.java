package de.tum.in.www1.artemis.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.exam.Exam;
import de.tum.in.www1.artemis.domain.modeling.ModelingExercise;
import de.tum.in.www1.artemis.domain.quiz.QuizExercise;
import de.tum.in.www1.artemis.repository.ExamRepository;
import de.tum.in.www1.artemis.service.programming.ProgrammingExerciseExportService;
import de.tum.in.www1.artemis.web.rest.dto.SubmissionExportOptionsDTO;

/**
 * Service Implementation for exporting courses and exams.
 */
@Service
public class CourseExamExportService {

    private final Logger log = LoggerFactory.getLogger(CourseExamExportService.class);

    private final ProgrammingExerciseExportService programmingExerciseExportService;

    private final ZipFileService zipFileService;

    private final FileService fileService;

    private final FileUploadSubmissionExportService fileUploadSubmissionExportService;

    private final TextSubmissionExportService textSubmissionExportService;

    private final ModelingSubmissionExportService modelingSubmissionExportService;

    private final ExamRepository examRepository;

    private final WebsocketMessagingService websocketMessagingService;

    public CourseExamExportService(ProgrammingExerciseExportService programmingExerciseExportService, ZipFileService zipFileService, FileService fileService,
            FileUploadSubmissionExportService fileUploadSubmissionExportService, TextSubmissionExportService textSubmissionExportService,
            ModelingSubmissionExportService modelingSubmissionExportService, WebsocketMessagingService websocketMessagingService, ExamRepository examRepository) {
        this.programmingExerciseExportService = programmingExerciseExportService;
        this.zipFileService = zipFileService;
        this.fileService = fileService;
        this.fileUploadSubmissionExportService = fileUploadSubmissionExportService;
        this.textSubmissionExportService = textSubmissionExportService;
        this.modelingSubmissionExportService = modelingSubmissionExportService;
        this.websocketMessagingService = websocketMessagingService;
        this.examRepository = examRepository;
    }

    /**
     * Exports the entire course into a single zip file that is saved in the directory specified
     * by outputDir.
     *
     * @param course        The course to export
     * @param outputDir     The directory where the exported course is saved
     * @param exportErrors  List of failures that occurred during the export
     * @return              Path to the zip file
     */
    public Optional<Path> exportCourse(Course course, String outputDir, List<String> exportErrors) {
        // Used for sending export progress notifications to instructors
        var notificationTopic = "/topic/courses/" + course.getId() + "/export-course";
        notifyUserAboutExerciseExportState(notificationTopic, CourseExamExportState.RUNNING, List.of("Creating temporary directories..."));

        var timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-Hmss"));
        var courseDirName = course.getShortName() + "-" + course.getTitle() + "-" + timestamp;
        String cleanCourseDirName = fileService.removeIllegalCharacters(courseDirName);

        // Create a temporary directory that will contain the files that will be zipped
        Path tmpCourseDir = Path.of("./exports", cleanCourseDirName);
        try {
            Files.createDirectories(tmpCourseDir);
        }
        catch (IOException e) {
            logMessageAndAppendToList("Failed to export course " + course.getId() + " because the temporary directory: " + tmpCourseDir + " cannot be created.", exportErrors);
            return Optional.empty();
        }

        // Export course exercises and exams
        List<Path> exportedFiles = exportCourseAndExamExercises(notificationTopic, course, tmpCourseDir.toString(), exportErrors);

        // Zip all exported exercises into a single zip file.
        notifyUserAboutExerciseExportState(notificationTopic, CourseExamExportState.RUNNING, List.of("Done exporting exercises. Creating course zip..."));
        Path courseZip = Path.of(outputDir, tmpCourseDir.getFileName() + ".zip");
        var exportedCourse = createCourseZipFile(courseZip, exportedFiles, tmpCourseDir, exportErrors);

        // Delete temporary directory used for zipping
        fileService.scheduleForDirectoryDeletion(tmpCourseDir, 1);

        var exportState = exportErrors.isEmpty() ? CourseExamExportState.COMPLETED : CourseExamExportState.COMPLETED_WITH_WARNINGS;
        notifyUserAboutExerciseExportState(notificationTopic, exportState, exportErrors);

        log.info("Successfully exported course {}. The zip file is located at: {}", course.getId(), exportedCourse);
        return exportedCourse;
    }

    /**
     * Exports an exam into a single zip file that is saved in the directory specified
     * by outputDir.
     *
     * @param exam          The exam to export
     * @param outputDir     The directory where the exported exam is saved
     * @param exportErrors  List of failures that occurred during the export
     * @return              Path to the zip file
     */
    public Optional<Path> exportExam(Exam exam, String outputDir, List<String> exportErrors) {
        // Used for sending export progress notifications to instructors
        var notificationTopic = "/topic/exams/" + exam.getId() + "/export";
        notifyUserAboutExerciseExportState(notificationTopic, CourseExamExportState.RUNNING, List.of("Creating temporary directories..."));

        var timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-Hmss"));
        var examDirName = exam.getId() + "-" + exam.getTitle() + "-" + timestamp;
        var cleanExamDirName = fileService.removeIllegalCharacters(examDirName);

        // Create a temporary directory that will contain the files that will be zipped
        Path tempExamsDir = Path.of("./exports", cleanExamDirName);
        try {
            Files.createDirectories(tempExamsDir);
        }
        catch (IOException e) {
            logMessageAndAppendToList("Failed to export exam " + exam.getId() + " because the temporary directory: " + tempExamsDir + " cannot be created.", exportErrors);
            return Optional.empty();
        }

        // Export exam exercises
        notifyUserAboutExerciseExportState(notificationTopic, CourseExamExportState.RUNNING, List.of("Preparing to export exam exercises..."));
        var exercises = examRepository.findAllExercisesByExamId(exam.getId());
        List<Path> exportedExercises = exportExercises(notificationTopic, exercises, tempExamsDir.toString(), 0, exercises.size(), exportErrors);

        // Zip all exported exercises into a single zip file.
        notifyUserAboutExerciseExportState(notificationTopic, CourseExamExportState.RUNNING, List.of("Done exporting exercises. Creating course zip..."));
        Path examZip = Path.of(outputDir, tempExamsDir.getFileName() + ".zip");
        var exportedExamPath = createCourseZipFile(examZip, exportedExercises, tempExamsDir, exportErrors);

        // Delete temporary directory used for zipping
        fileService.scheduleForDirectoryDeletion(tempExamsDir, 1);

        var exportState = exportErrors.isEmpty() ? CourseExamExportState.COMPLETED : CourseExamExportState.COMPLETED_WITH_WARNINGS;
        notifyUserAboutExerciseExportState(notificationTopic, exportState, exportErrors);

        log.info("Successfully exported exam {}. The zip file is located at: {}", exam.getId(), exportedExamPath);
        return exportedExamPath;
    }

    /**
     * Exports the exercises of the course and of each exam belonging to the course.
     *
     * @param notificationTopic     the topic used to notify the client
     * @param course                The course to export
     * @param outputDir             the directory that will be used to store the exported data
     * @param exportErrors          any errors that occurred during the export
     * @return                      list of zip files
     */
    private List<Path> exportCourseAndExamExercises(String notificationTopic, Course course, String outputDir, List<String> exportErrors) {
        notifyUserAboutExerciseExportState(notificationTopic, CourseExamExportState.RUNNING, List.of("Preparing to export course exercises and exams..."));

        // Get every course and exam exercise
        Set<Exercise> courseExercises = course.getExercises();

        // Retrieve exams of the course and get exercises for each exam
        List<Exam> courseExams = examRepository.findByCourseId(course.getId());

        // Calculate the amount of exercises for all exams
        var examExercises = courseExams.stream().map(exam -> examRepository.findAllExercisesByExamId(exam.getId())).flatMap(Collection::stream).collect(Collectors.toSet());

        int totalExercises = courseExercises.size() + examExercises.size();
        int progress = 0;

        // Export exercises of the course
        var exportedFiles = exportCourseExercises(notificationTopic, course, outputDir, progress, totalExercises, exportErrors);
        progress += courseExercises.size();

        // Export the exercises of the course exams
        List<Path> exportedExercises = exportExams(notificationTopic, courseExams, outputDir, progress, totalExercises, exportErrors);
        exportedFiles.addAll(new ArrayList<>(exportedExercises));

        return exportedFiles;
    }

    /**
     * Exports all exercises of the course and adds them into the directory
     * outputDir/exercises/
     *
     * @param notificationTopic     the topic used to notify the client
     * @param course                The course where the exercises are located
     * @param outputDir             The directory that will be used to store the exercises subdirectory
     * @param progress              The previous export progress that should be used as a base
     * @param totalExerciseCount    The total amount of exercises that will be exported
     * @param exportErrors          List of failures that occurred during the export
     * @return                      List of paths for the exported exercises
     */
    private List<Path> exportCourseExercises(String notificationTopic, Course course, String outputDir, int progress, int totalExerciseCount, List<String> exportErrors) {
        log.info("Exporting course exercises for course {} and title {}", course.getId(), course.getTitle());

        Path exercisesDir = Path.of(outputDir, "course-exercises");
        try {
            Files.createDirectory(exercisesDir);
            return exportExercises(notificationTopic, course.getExercises(), exercisesDir.toString(), progress, totalExerciseCount, exportErrors);
        }
        catch (IOException e) {
            logMessageAndAppendToList("Failed to create course exercise directory" + exercisesDir + ".", exportErrors);
            return List.of();
        }
    }

    /**
     * Exports all exams of the course by zipping each one separately and add them into the directory
     * outputDir/exams/
     *
     * @param notificationTopic     the topic used to notify the client
     * @param exams                 The exams that will be exported
     * @param outputDir             The directory that will be used to store the exams
     * @param progress              The previous export progress that should be used as a base
     * @param totalExerciseCount    The total amount of exercises that will be exported
     * @param exportErrors          List of failures that occurred during the export
     * @return                      List of paths
     */
    private List<Path> exportExams(String notificationTopic, List<Exam> exams, String outputDir, int progress, int totalExerciseCount, List<String> exportErrors) {
        Optional<Exam> firstExam = exams.stream().findFirst();
        if (firstExam.isEmpty()) {
            log.warn("Skipping exam export since the course does not have any exams");
            return List.of();
        }

        Course course = firstExam.get().getCourse();
        log.info("Export course exams for course {} and title {}", course.getId(), course.getTitle());

        int currentProgress = progress;
        Path examsDir = null;
        try {
            // Create the exams directory that will contain the exported exams
            examsDir = Path.of(outputDir, "exams");
            Files.createDirectory(examsDir);

            // Export each exams. We first fetch its exercises and then export them.
            var exportedExams = new ArrayList<Path>();
            for (var exam : exams) {
                var examExercises = examRepository.findAllExercisesByExamId(exam.getId());
                var exportedExam = exportExam(notificationTopic, exam, examExercises, examsDir.toString(), currentProgress, totalExerciseCount, exportErrors);
                exportedExams.addAll(exportedExam);
                currentProgress += examExercises.size();
            }
            return exportedExams;
        }
        catch (IOException e) {
            logMessageAndAppendToList("Failed to create course exams directory " + examsDir + ".", exportErrors);
            return List.of();
        }
    }

    /**
     * Exports an exam and adds the zip file into the directory specified by outputDir.
     *
     * @param notificationTopic     the topic used to notify the client
     * @param exam                  The exam to export
     * @param examExercises         the exercises that belong to the exam
     * @param outputDir             The directory that will be used to store the exam
     * @param progress              The previous export progress that should be used as a base
     * @param totalExerciseCount    The total amount of exercises that will be exported
     * @param exportErrors          List of failures that occurred during the export
     * @return                      List of paths
     */
    private List<Path> exportExam(String notificationTopic, Exam exam, Set<Exercise> examExercises, String outputDir, int progress, int totalExerciseCount,
            List<String> exportErrors) {
        log.info("Export course exam {}", exam.getId());

        Path examDir = null;
        try {
            // Create exam directory.
            String cleanExamTitle = fileService.removeIllegalCharacters(exam.getId() + "-" + exam.getTitle());
            examDir = Path.of(outputDir, cleanExamTitle);
            Files.createDirectory(examDir);

            // We retrieve every exercise from each exercise group and flatten the list.
            return exportExercises(notificationTopic, examExercises, examDir.toString(), progress, totalExerciseCount, exportErrors);
        }
        catch (IOException e) {
            logMessageAndAppendToList("Failed to create exam directory " + examDir + ".", exportErrors);
            return List.of();
        }
    }

    /**
     * Exports the specified exercises by creating a zip file for each exercise.
     * The zip files are created in the directory specified by outputDir. The function returns
     * a list of exercise ids that couldn't be exported.
     *
     * @param notificationTopic     the topic used to notify the client
     * @param exercises             The exercises to export
     * @param outputDir             The path to a directory that will be used to store the zipped files.
     * @param progress              The previous export progress that should be used as a base
     * @param totalExerciseCount    The total amount of exercises that will be exported
     * @param exportErrors          List of failures that occurred during the export
     * @return                      List of paths of the exported exercises
     */
    private List<Path> exportExercises(String notificationTopic, Set<Exercise> exercises, String outputDir, int progress, int totalExerciseCount, List<String> exportErrors) {
        List<Path> exportedExercises = new ArrayList<>();
        int currentProgress = progress;
        for (var exercise : exercises) {
            log.info("Exporting exercise {} with id {} ", exercise.getTitle(), exercise.getId());

            // Notify the user after the progress
            currentProgress++;
            notifyUserAboutExerciseExportState(notificationTopic, CourseExamExportState.RUNNING, List.of(currentProgress + "/" + totalExerciseCount + " done"));

            // Export programming exercise
            if (exercise instanceof ProgrammingExercise programmingExercise) {
                // Download the repositories template, solution, tests and students' repositories
                exportedExercises.add(programmingExerciseExportService.exportProgrammingExerciseRepositories(programmingExercise, true, Path.of(outputDir), exportErrors));
                continue;
            }

            // Export the other exercises types

            // Export options
            var submissionsExportOptions = new SubmissionExportOptionsDTO();
            submissionsExportOptions.setExportAllParticipants(true);

            // The zip file containing student submissions for the other exercise types
            Optional<File> exportedSubmissionsFileOrEmpty = Optional.empty();

            try {
                if (exercise instanceof FileUploadExercise) {
                    exportedSubmissionsFileOrEmpty = fileUploadSubmissionExportService.exportStudentSubmissions(exercise.getId(), submissionsExportOptions);

                }
                else if (exercise instanceof TextExercise) {
                    exportedSubmissionsFileOrEmpty = textSubmissionExportService.exportStudentSubmissions(exercise.getId(), submissionsExportOptions);

                }
                else if (exercise instanceof ModelingExercise) {
                    exportedSubmissionsFileOrEmpty = modelingSubmissionExportService.exportStudentSubmissions(exercise.getId(), submissionsExportOptions);
                }
                else if (exercise instanceof QuizExercise) {
                    // TODO: Quiz submissions aren't supported yet
                    continue;
                }
                else {
                    // Exercise is not supported so skip
                    continue;
                }
            }
            catch (IOException e) {
                logMessageAndAppendToList("Failed to export exercise '" + exercise.getTitle() + "' (id: " + exercise.getId() + "): " + e.getMessage(), exportErrors);
            }

            // Exported submissions are stored somewhere else so we move the generated zip file into the
            // outputDir (directory where the files needed for the course archive are stored).
            if (exportedSubmissionsFileOrEmpty.isPresent()) {
                var exportedSubmissionsFile = exportedSubmissionsFileOrEmpty.get();
                try {
                    Path newExportedSubmissionsFilePath = Path.of(outputDir, exportedSubmissionsFile.getName());
                    Files.move(exportedSubmissionsFile.toPath(), newExportedSubmissionsFilePath);
                    // Delete the directory where the zip was located before it was moved
                    FileUtils.deleteDirectory(Path.of(exportedSubmissionsFile.getParent()).toFile());

                    exportedExercises.add(newExportedSubmissionsFilePath);
                }
                catch (IOException e) {
                    logMessageAndAppendToList("Failed to move file " + exportedSubmissionsFile.toPath() + " to " + outputDir + ".", exportErrors);
                }
            }
        }
        return exportedExercises.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Creates a zip file out of all the files and directories inside courseDirPath and saves it to
     * the directory specified by outputDirPath
     *
     * @param outputZipFile     The path to the zip file that will be created
     * @param filesToZip        the files to zip together
     * @param relativeZipPath   the path of the zip files will be relative to this path
     * @return                  The path to the zip file
     */
    private Optional<Path> createCourseZipFile(Path outputZipFile, List<Path> filesToZip, Path relativeZipPath, List<String> exportErrors) {
        try {
            zipFileService.createZipFile(outputZipFile, filesToZip, relativeZipPath);
            log.info("Successfully created zip file: {}", outputZipFile);
            return Optional.of(outputZipFile);
        }
        catch (IOException e) {
            logMessageAndAppendToList("Failed to create zip file" + outputZipFile + ".", exportErrors);
            return Optional.empty();
        }
    }

    private void logMessageAndAppendToList(String message, List<String> messageList) {
        log.error(message);
        messageList.add(message);
    }

    /***
     * Sends a message notifying the user about the current state of the exercise export
     *
     * @param topic         The topic to send the notification to
     * @param exportState   The export state
     * @param messages      optional messages to send
     */
    private void notifyUserAboutExerciseExportState(String topic, CourseExamExportState exportState, List<String> messages) {
        Map<String, String> payload = new HashMap<>();
        payload.put("exportState", exportState.toString());
        payload.put("message", String.join("\n", messages));

        var mapper = new ObjectMapper();
        try {
            websocketMessagingService.sendMessage(topic, mapper.writeValueAsString(payload));
        }
        catch (IOException e) {
            log.info("Couldn't notify the user about the exercise export state for topic {}: {}", topic, e.getMessage());
        }
    }
}
