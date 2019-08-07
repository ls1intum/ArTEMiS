package de.tum.in.www1.artemis.web.rest.repository;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.service.*;
import de.tum.in.www1.artemis.service.connectors.ContinuousIntegrationService;
import de.tum.in.www1.artemis.service.connectors.GitService;
import de.tum.in.www1.artemis.service.connectors.VersionControlService;
import de.tum.in.www1.artemis.web.rest.FileMove;
import de.tum.in.www1.artemis.web.rest.dto.RepositoryStatusDTO;

/**
 * Executes requested actions on the test repository of a programming exercise. Only available to TAs, Instructors and Admins.
 */
@RestController
@RequestMapping({ "/api", "/api_basic" })
@PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
public class TestRepositoryResource extends RepositoryResource {

    private final ExerciseService exerciseService;

    private final Optional<VersionControlService> versionControlService;

    public TestRepositoryResource(UserService userService, AuthorizationCheckService authCheckService, Optional<GitService> gitService,
            Optional<ContinuousIntegrationService> continuousIntegrationService, RepositoryService repositoryService, ExerciseService exerciseService,
            Optional<VersionControlService> versionControlService) {
        super(userService, authCheckService, gitService, continuousIntegrationService, repositoryService);
        this.exerciseService = exerciseService;
        this.versionControlService = versionControlService;
    }

    /**
     * Retrieve a test repository by providing its exerciseId. Will check if the user has permissions to access data related to the given test repository.
     *
     * @param exerciseId of the given test repository's exercise.
     * @param pullOnGet  perform a pull on retrieval of a git repository (in some cases it might make sense not to pull!)
     * @return
     * @throws IOException
     * @throws InterruptedException
     * @throws GitAPIException
     */
    @Override
    Repository getRepository(Long exerciseId, boolean pullOnGet) throws IOException, IllegalAccessException, InterruptedException, GitAPIException {
        ProgrammingExercise exercise = (ProgrammingExercise) exerciseService.findOne(exerciseId);
        String testRepoName = exercise.getTestRepositoryName();
        URL testsRepoUrl = versionControlService.get().getCloneURL(exercise.getProjectKey(), testRepoName);
        return repositoryService.checkoutRepositoryByName(exercise, testsRepoUrl, pullOnGet);
    }

    /**
     * Get the test repository url by providing a exercise id. Will not check any permissions!
     *
     * @param exerciseId
     * @return
     */
    @Override
    URL getRepositoryUrl(Long exerciseId) {
        ProgrammingExercise exercise = (ProgrammingExercise) exerciseService.findOne(exerciseId);
        String testRepoName = exercise.getProjectKey().toLowerCase() + "-tests";
        return versionControlService.get().getCloneURL(exercise.getProjectKey(), testRepoName);
    }

    /**
     * Check if a user can access the test repository of the exercise.
     *
     * @param exerciseId
     * @return
     */
    @Override
    boolean canAccessRepository(Long exerciseId) {
        ProgrammingExercise exercise = (ProgrammingExercise) exerciseService.findOne(exerciseId);
        return authCheckService.isAtLeastInstructorInCourse(exercise.getCourse(), userService.getUserWithGroupsAndAuthorities());
    }

    /**
     * GET /test-repository/{exerciseId}/files: Map of all file and folders of the repository. Each entry states if it is a file or a folder.
     *
     * @param exerciseId Exercise ID
     * @return
     * @throws IOException
     */
    @GetMapping(value = "/test-repository/{exerciseId}/files", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HashMap<String, FileType>> getFiles(@PathVariable Long exerciseId) throws IOException, InterruptedException {
        return super.getFiles(exerciseId);
    }

    /**
     * GET /test-repository/{exerciseId}/file: Get the content of a file
     *
     * @param exerciseId Exercise ID
     * @param filename
     * @return
     * @throws IOException
     */
    @GetMapping(value = "/test-repository/{exerciseId}/file", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> getFile(@PathVariable Long exerciseId, @RequestParam("file") String filename) throws IOException, InterruptedException {
        return super.getFile(exerciseId, filename);
    }

    /**
     * POST /test-repository/{exerciseId}/file: Create new file
     *
     * @param exerciseId Exercise ID
     * @param filename
     * @param request
     * @return
     * @throws IOException
     */
    @PostMapping(value = "/test-repository/{exerciseId}/file", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createFile(@PathVariable Long exerciseId, @RequestParam("file") String filename, HttpServletRequest request) {
        return super.createFile(exerciseId, filename, request);
    }

    /**
     * POST /test-repository/{exerciseId}/folder: Create new folder
     *
     * @param exerciseId Exercise ID
     * @param folderName
     * @param request
     * @return
     * @throws IOException
     */
    @PostMapping(value = "/test-repository/{exerciseId}/folder", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createFolder(@PathVariable Long exerciseId, @RequestParam("folder") String folderName, HttpServletRequest request) {
        return super.createFolder(exerciseId, folderName, request);
    }

    /**
     * Change the name of a file.
     *
     * @param exerciseId Exercise ID
     * @param fileMove   defines current and new path in git repository.
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    @PostMapping(value = "/test-repository/{exerciseId}/rename-file", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> renameFile(@PathVariable Long exerciseId, @RequestBody FileMove fileMove) {
        return super.renameFile(exerciseId, fileMove);
    }

    /**
     * DELETE /test-repository/{exerciseId}/file: Delete the file or the folder specified. If the path is a folder, all files in it will be deleted, too.
     *
     * @param exerciseId Exercise ID
     * @param filename   path of file or folder to delete.
     * @return
     * @throws IOException
     */
    @DeleteMapping(value = "/test-repository/{exerciseId}/file", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteFile(@PathVariable Long exerciseId, @RequestParam("file") String filename) {
        return super.deleteFile(exerciseId, filename);
    }

    /**
     * GET /test-repository/{exerciseId}/pull: Pull into the participation repository
     *
     * @param exerciseId Exercise ID
     * @return
     * @throws IOException
     */
    @GetMapping(value = "/test-repository/{exerciseId}/pull", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> pullChanges(@PathVariable Long exerciseId) {
        return super.pullChanges(exerciseId);
    }

    /**
     * POST /test-repository/{exerciseId}/commit: Commit into the participation repository
     *
     * @param exerciseId Exercise ID
     * @return
     * @throws IOException
     * @throws GitAPIException
     */
    @PostMapping(value = "/test-repository/{exerciseId}/commit", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> commitChanges(@PathVariable Long exerciseId) {
        return super.commitChanges(exerciseId);
    }

    /**
     * GET /test-repository/{exerciseId}: Get the "clean" status of the repository. Clean = No uncommitted changes.
     *
     * @param exerciseId Exercise ID
     * @return
     * @throws IOException
     * @throws GitAPIException
     */
    @GetMapping(value = "/test-repository/{exerciseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RepositoryStatusDTO> getStatus(@PathVariable Long exerciseId) throws IOException, GitAPIException, InterruptedException {
        return super.getStatus(exerciseId);
    }
}
