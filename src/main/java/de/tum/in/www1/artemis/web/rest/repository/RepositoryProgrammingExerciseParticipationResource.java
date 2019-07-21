package de.tum.in.www1.artemis.web.rest.repository;

import static de.tum.in.www1.artemis.web.rest.util.ResponseUtil.*;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.service.*;
import de.tum.in.www1.artemis.service.connectors.ContinuousIntegrationService;
import de.tum.in.www1.artemis.service.connectors.GitService;
import de.tum.in.www1.artemis.web.rest.FileMove;
import de.tum.in.www1.artemis.web.rest.dto.RepositoryStatusDTO;
import de.tum.in.www1.artemis.web.rest.errors.EntityNotFoundException;

/**
 * Executes repository actions on repositories related to the participation id transmitted. Available to the owner of the participation, TAs/Instructors of the exercise and Admins.
 */
@RestController
@RequestMapping({ "/api", "/api_basic" })
@PreAuthorize("hasAnyRole('USER', 'TA', 'INSTRUCTOR', 'ADMIN')")
public class RepositoryProgrammingExerciseParticipationResource extends RepositoryResource {

    private final ProgrammingExerciseParticipationService participationService;

    public RepositoryProgrammingExerciseParticipationResource(UserService userService, AuthorizationCheckService authCheckService, Optional<GitService> gitService,
            Optional<ContinuousIntegrationService> continuousIntegrationService, RepositoryService repositoryService,
            ProgrammingExerciseParticipationService participationService) {
        super(userService, authCheckService, gitService, continuousIntegrationService, repositoryService);
        this.participationService = participationService;
    }

    /**
     * Retrieve a repository by providing a participation id. Will check if the user has permissions to access data related to the given participation.
     *
     * @param participationId of the given participation.
     * @param pullOnGet       perform a pull on retrieval of a git repository (in some cases it might make sense not to pull!)
     * @return
     * @throws IOException
     * @throws InterruptedException
     * @throws GitAPIException
     */
    @Override
    Repository getRepository(Long participationId, boolean pullOnGet) throws IOException, InterruptedException, GitAPIException {
        Participation participation = participationService.findParticipation(participationId);
        if (!(participation instanceof ProgrammingExerciseParticipation))
            throw new IllegalArgumentException();
        boolean hasPermissions = participationService.canAccessParticipation((ProgrammingExerciseParticipation) participation);
        if (!hasPermissions) {
            throw new IllegalAccessError();
        }
        URL repositoryUrl = ((ProgrammingExerciseParticipation) participation).getRepositoryUrlAsUrl();
        return gitService.get().getOrCheckoutRepository(repositoryUrl, pullOnGet);
    }

    /**
     * Get the repository url by providing a participation id. Will not check any permissions!
     *
     * @param participationId
     * @return
     */
    @Override
    URL getRepositoryUrl(Long participationId) throws IllegalArgumentException {
        Participation participation = participationService.findParticipation(participationId);
        if (!(participation instanceof ProgrammingExerciseParticipation))
            throw new IllegalArgumentException();
        return ((ProgrammingExerciseParticipation) participation).getRepositoryUrlAsUrl();
    }

    /**
     * Check if a user can access the participation's repository.
     *
     * @param participationId
     * @return
     */
    @Override
    boolean canAccessRepository(Long participationId) throws IllegalArgumentException {
        Participation participation = participationService.findParticipation(participationId);
        if (!(participation instanceof ProgrammingExerciseParticipation))
            throw new IllegalArgumentException();
        return participationService.canAccessParticipation((ProgrammingExerciseParticipation) participation);
    }

    /**
     * GET /repository/{participationId}/files: Map of all file and folders of the repository. Each entry states if it is a file or a folder.
     *
     * @param participationId Participation ID
     * @return
     * @throws IOException
     */
    @GetMapping(value = "/repository/{participationId}/files", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HashMap<String, FileType>> getFiles(@PathVariable Long participationId) throws IOException, InterruptedException {
        return super.getFiles(participationId);
    }

    /**
     * GET /repository/{participationId}/file: Get the content of a file
     *
     * @param participationId Participation ID
     * @param filename
     * @return
     * @throws IOException
     */
    @GetMapping(value = "/repository/{participationId}/file", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> getFile(@PathVariable Long participationId, @RequestParam("file") String filename) throws IOException, InterruptedException {
        return super.getFile(participationId, filename);
    }

    /**
     * POST /repository/{participationId}/file: Create new file
     *
     * @param participationId Participation ID
     * @param filename
     * @param request
     * @return
     * @throws IOException
     */
    @PostMapping(value = "/repository/{participationId}/file", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createFile(@PathVariable Long participationId, @RequestParam("file") String filename, HttpServletRequest request) {
        return super.createFile(participationId, filename, request);
    }

    /**
     * POST /repository/{participationId}/folder: Create new folder
     *
     * @param participationId Participation ID
     * @param folderName
     * @param request
     * @return
     * @throws IOException
     */
    @PostMapping(value = "/repository/{participationId}/folder", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createFolder(@PathVariable Long participationId, @RequestParam("folder") String folderName, HttpServletRequest request) {
        return super.createFolder(participationId, folderName, request);
    }

    /**
     * Change the name of a file.
     *
     * @param participationId id of the participation the git repository belongs to.
     * @param fileMove        defines current and new path in git repository.
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    @PostMapping(value = "/repository/{participationId}/rename-file", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> renameFile(@PathVariable Long participationId, @RequestBody FileMove fileMove) {
        return super.renameFile(participationId, fileMove);
    }

    /**
     * DELETE /repository/{participationId}/file: Delete the file or the folder specified. If the path is a folder, all files in it will be deleted, too.
     *
     * @param participationId Participation ID
     * @param filename        path of file or folder to delete.
     * @return
     * @throws IOException
     */
    @DeleteMapping(value = "/repository/{participationId}/file", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteFile(@PathVariable Long participationId, @RequestParam("file") String filename) {
        return super.deleteFile(participationId, filename);
    }

    /**
     * GET /repository/{participationId}/pull: Pull into the participation repository
     *
     * @param participationId Participation ID
     * @return
     * @throws IOException
     */
    @GetMapping(value = "/repository/{participationId}/pull", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> pullChanges(@PathVariable Long participationId) {
        return super.pullChanges(participationId);
    }

    /**
     * POST /repository/{participationId}/commit: Commit into the participation repository
     *
     * @param participationId Participation ID
     * @return
     * @throws IOException
     * @throws GitAPIException
     */
    @PostMapping(value = "/repository/{participationId}/commit", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> commitChanges(@PathVariable Long participationId) {
        return super.commitChanges(participationId);
    }

    @PostMapping(value = "/repository/{participationId}/reset", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> resetToLastCommit(@PathVariable Long participationId) {
        return super.resetToLastCommit(participationId);
    }

    /**
     * GET /repository/{participationId}: Get the "clean" status of the repository. Clean = No uncommitted changes.
     *
     * @param participationId Participation ID
     * @return
     * @throws IOException
     * @throws GitAPIException
     */
    @GetMapping(value = "/repository/{participationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RepositoryStatusDTO> getStatus(@PathVariable Long participationId) throws IOException, GitAPIException, InterruptedException {
        return super.getStatus(participationId);
    }

    /**
     * GET /repository/:participationId/buildlogs : get the build log from Bamboo for the "participationId" repository.
     *
     * @param participationId the participationId of the result to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the result, or with status 404 (Not Found)
     */
    @GetMapping(value = "/repository/{participationId}/buildlogs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getResultDetails(@PathVariable Long participationId) {
        log.debug("REST request to get build log : {}", participationId);

        Participation participation;
        try {
            participation = participationService.findParticipation(participationId);
        }
        catch (EntityNotFoundException ex) {
            return notFound();
        }
        if (!(participation instanceof ProgrammingExerciseParticipation))
            return badRequest();

        if (!participationService.canAccessParticipation((ProgrammingExerciseParticipation) participation))
            return forbidden();

        List<BuildLogEntry> logs = continuousIntegrationService.get().getLatestBuildLogs(((ProgrammingExerciseParticipation) participation).getBuildPlanId());

        return new ResponseEntity<>(logs, HttpStatus.OK);
    }
}
