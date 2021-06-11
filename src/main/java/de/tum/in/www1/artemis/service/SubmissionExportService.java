package de.tum.in.www1.artemis.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.domain.Course;
import de.tum.in.www1.artemis.domain.DomainObject;
import de.tum.in.www1.artemis.domain.Exercise;
import de.tum.in.www1.artemis.domain.Submission;
import de.tum.in.www1.artemis.domain.participation.StudentParticipation;
import de.tum.in.www1.artemis.repository.ExerciseRepository;
import de.tum.in.www1.artemis.service.archival.ArchivalReportEntry;
import de.tum.in.www1.artemis.web.rest.dto.SubmissionExportOptionsDTO;

@Service
public abstract class SubmissionExportService {

    private final Logger log = LoggerFactory.getLogger(SubmissionExportService.class);

    private final ExerciseRepository exerciseRepository;

    private final ZipFileService zipFileService;

    private final FileService fileService;

    public SubmissionExportService(ExerciseRepository exerciseRepository, ZipFileService zipFileService, FileService fileService) {
        this.exerciseRepository = exerciseRepository;
        this.zipFileService = zipFileService;
        this.fileService = fileService;
    }

    /**
     * Exports student submissions to a zip file for an exercise.
     *
     * The outputDir is used to store the zip file and temporary files used for zipping so make
     * sure to delete it if it's no longer used.
     *
     * @param exerciseId the id of the exercise to be exported
     * @param submissionExportOptions the options for the export
     * @param outputDir directory to store the temporary files in
     * @param exportErrors a list of errors for submissions that couldn't be exported and are not included in the file
     * @param reportData   a list of all exercises and their statistics
     * @return a reference to the zipped file
     * @throws IOException if an error occurred while zipping
     */
    public Optional<File> exportStudentSubmissions(Long exerciseId, SubmissionExportOptionsDTO submissionExportOptions, Path outputDir, List<String> exportErrors,
            List<ArchivalReportEntry> reportData) throws IOException {

        Optional<Exercise> exerciseOpt = exerciseRepository.findWithEagerStudentParticipationsStudentAndSubmissionsById(exerciseId);

        if (exerciseOpt.isEmpty()) {
            return Optional.empty();
        }

        Exercise exercise = exerciseOpt.get();

        // Select the participations that should be exported
        List<StudentParticipation> exportedStudentParticipations;

        if (submissionExportOptions.isExportAllParticipants()) {
            exportedStudentParticipations = new ArrayList<>(exercise.getStudentParticipations());
        }
        else {
            List<String> participantIds = Arrays.stream(submissionExportOptions.getParticipantIdentifierList().split(",")).map(String::trim).collect(Collectors.toList());

            exportedStudentParticipations = exercise.getStudentParticipations().stream().filter(participation -> participantIds.contains(participation.getParticipantIdentifier()))
                    .collect(Collectors.toList());
        }

        if (exportedStudentParticipations.isEmpty()) {
            return Optional.empty();
        }

        ZonedDateTime filterLateSubmissionsDate = null;
        if (submissionExportOptions.isFilterLateSubmissions()) {
            if (submissionExportOptions.getFilterLateSubmissionsDate() == null) {
                filterLateSubmissionsDate = exercise.getDueDate();
            }
            else {
                filterLateSubmissionsDate = submissionExportOptions.getFilterLateSubmissionsDate();
            }
        }

        // Sort the student participations by id
        exportedStudentParticipations.sort(Comparator.comparing(DomainObject::getId));

        return this.createZipFileFromParticipations(exercise, exportedStudentParticipations, filterLateSubmissionsDate, outputDir, exportErrors, reportData);
    }

    /**
     * Creates a zip file from a list of participations for an exercise.
     *
     * The outputDir is used to store the zip file and temporary files used for zipping so make
     * sure to delete it if it's no longer used.
     *
     * @param exercise the exercise in question
     * @param participations a list of participations to include
     * @param lateSubmissionFilter an optional date filter for submissions
     * @param outputDir directory to store the temporary files in
     * @param  exportErrors a list of errors for submissions that couldn't be exported and are not included in the file
     * @param reportData   a list of all exercises and their statistics
     * @return the zipped file
     * @throws IOException if an error occurred while zipping
     */
    private Optional<File> createZipFileFromParticipations(Exercise exercise, List<StudentParticipation> participations, @Nullable ZonedDateTime lateSubmissionFilter,
            Path outputDir, List<String> exportErrors, List<ArchivalReportEntry> reportData) throws IOException {

        Course course = exercise.getCourseViaExerciseGroupOrCourseMember();

        // Create unique name for directory
        String zipGroupName = course.getShortName() + "-" + exercise.getTitle() + "-" + exercise.getId();
        String cleanZipGroupName = fileService.removeIllegalCharacters(zipGroupName);
        String zipFileName = cleanZipGroupName + "-" + ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-Hmss")) + ".zip";

        // Create directory
        Path submissionsFolderPath = Paths.get(outputDir.toString(), "zippedSubmissions", zipGroupName);
        Path zipFilePath = Paths.get(outputDir.toString(), "zippedSubmissions", zipFileName);

        File submissionFolder = submissionsFolderPath.toFile();
        if (!submissionFolder.exists() && !submissionFolder.mkdirs()) {
            log.error("Couldn't create dir: {}", submissionFolder);
            exportErrors.add("Cannot create directory: " + submissionFolder.toPath());
            return Optional.empty();
        }

        // Save all Submissions
        List<Path> submissionFilePaths = participations.stream().map((participation) -> {

            Set<Submission> submissions = participation.getSubmissions();
            Submission latestSubmission = null;

            for (Submission submission : submissions) {
                if (submission.getSubmissionDate() == null) {
                    // ignore unsubmitted submissions
                    continue;
                }
                // filter late submissions
                if (lateSubmissionFilter == null || submission.getSubmissionDate().isBefore(lateSubmissionFilter)) {
                    if (latestSubmission == null || submission.getSubmissionDate().isAfter(latestSubmission.getSubmissionDate())) {
                        latestSubmission = submission;
                    }
                }
            }

            if (latestSubmission == null) {
                return Optional.<Path>empty();
            }

            // create file path
            String submissionFileName = exercise.getTitle() + "-" + participation.getParticipantIdentifier() + "-" + latestSubmission.getId()
                    + this.getFileEndingForSubmission(latestSubmission);
            Path submissionFilePath = Paths.get(submissionsFolderPath.toString(), submissionFileName);

            // store file
            try {
                this.saveSubmissionToFile(exercise, latestSubmission, submissionFilePath.toFile());
                return Optional.of(submissionFilePath);
            }
            catch (Exception ex) {
                String message = "Could not create file " + submissionFilePath + "  for exporting: " + ex.getMessage();
                log.error(message);
                exportErrors.add(message);
                return Optional.<Path>empty();
            }

        }).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());

        // Add report entry
        reportData.add(new ArchivalReportEntry(exercise.getId(), fileService.removeIllegalCharacters(exercise.getTitle()), participations.size(), submissionFilePaths.size()));

        if (submissionFilePaths.isEmpty()) {
            return Optional.empty();
        }

        // zip stores submissions
        try {
            zipFileService.createZipFile(zipFilePath, submissionFilePaths, submissionsFolderPath);
        }
        finally {
            deleteTempFiles(submissionFilePaths);
        }

        return Optional.of(zipFilePath.toFile());
    }

    protected abstract void saveSubmissionToFile(Exercise exercise, Submission submission, File file) throws IOException;

    protected abstract String getFileEndingForSubmission(Submission submission);

    /**
     * Delete all temporary files created during export
     *
     * @param pathsToTempFiles A list of all paths to temporary files, that should be deleted
     */
    private void deleteTempFiles(List<Path> pathsToTempFiles) {
        log.debug("Delete all temporary files");
        // delete the temporary zipped repo files
        for (Path tempFile : pathsToTempFiles) {
            try {
                Files.delete(tempFile);
            }
            catch (Exception ex) {
                log.warn("Could not delete file {}. Error message: {}", tempFile, ex.getMessage());
            }
        }
    }
}
