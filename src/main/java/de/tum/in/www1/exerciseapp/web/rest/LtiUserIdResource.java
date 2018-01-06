package de.tum.in.www1.exerciseapp.web.rest;

import com.codahale.metrics.annotation.Timed;
import de.tum.in.www1.exerciseapp.domain.LtiUserId;
import de.tum.in.www1.exerciseapp.repository.LtiUserIdRepository;
import de.tum.in.www1.exerciseapp.web.rest.util.HeaderUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * REST controller for managing LtiUserId.
 */
@RestController
@RequestMapping("/api")
public class LtiUserIdResource {

    private final Logger log = LoggerFactory.getLogger(LtiUserIdResource.class);

    private static final String ENTITY_NAME = "ltiUserId";

    private final LtiUserIdRepository ltiUserIdRepository;

    public LtiUserIdResource(LtiUserIdRepository ltiUserIdRepository) {
        this.ltiUserIdRepository = ltiUserIdRepository;
    }

    /**
     * POST  /lti-user-ids : Create a new ltiUserId.
     *
     * @param ltiUserId the ltiUserId to create
     * @return the ResponseEntity with status 201 (Created) and with body the new ltiUserId, or with status 400 (Bad Request) if the ltiUserId has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/lti-user-ids")
    @Timed
    public ResponseEntity<LtiUserId> createLtiUserId(@RequestBody LtiUserId ltiUserId) throws URISyntaxException {
        log.debug("REST request to save LtiUserId : {}", ltiUserId);
        if (ltiUserId.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(ENTITY_NAME, "idexists", "A new ltiUserId cannot already have an ID")).body(null);
        }
        LtiUserId result = ltiUserIdRepository.save(ltiUserId);
        return ResponseEntity.created(new URI("/api/lti-user-ids/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /lti-user-ids : Updates an existing ltiUserId.
     *
     * @param ltiUserId the ltiUserId to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated ltiUserId,
     * or with status 400 (Bad Request) if the ltiUserId is not valid,
     * or with status 500 (Internal Server Error) if the ltiUserId couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/lti-user-ids")
    @Timed
    public ResponseEntity<LtiUserId> updateLtiUserId(@RequestBody LtiUserId ltiUserId) throws URISyntaxException {
        log.debug("REST request to update LtiUserId : {}", ltiUserId);
        if (ltiUserId.getId() == null) {
            return createLtiUserId(ltiUserId);
        }
        LtiUserId result = ltiUserIdRepository.save(ltiUserId);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, ltiUserId.getId().toString()))
            .body(result);
    }

    //Deactivated because it would load all lti user ids and overload the server
    //TODO: activate this call again using the infinite scroll page mechanism
//    /**
//     * GET  /lti-user-ids : get all the ltiUserIds.
//     *
//     * @return the ResponseEntity with status 200 (OK) and the list of ltiUserIds in body
//     */
//    @GetMapping("/lti-user-ids")
//    @Timed
//    public List<LtiUserId> getAllLtiUserIds() {
//        log.debug("REST request to get all LtiUserIds");
//        return ltiUserIdRepository.findAll();
//    }

    /**
     * GET  /lti-user-ids/:id : get the "id" ltiUserId.
     *
     * @param id the id of the ltiUserId to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the ltiUserId, or with status 404 (Not Found)
     */
    @GetMapping("/lti-user-ids/{id}")
    @Timed
    public ResponseEntity<LtiUserId> getLtiUserId(@PathVariable Long id) {
        log.debug("REST request to get LtiUserId : {}", id);
        LtiUserId ltiUserId = ltiUserIdRepository.findOne(id);
        return ResponseUtil.wrapOrNotFound(Optional.ofNullable(ltiUserId));
    }

    /**
     * DELETE  /lti-user-ids/:id : delete the "id" ltiUserId.
     *
     * @param id the id of the ltiUserId to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/lti-user-ids/{id}")
    @Timed
    public ResponseEntity<Void> deleteLtiUserId(@PathVariable Long id) {
        log.debug("REST request to delete LtiUserId : {}", id);
        ltiUserIdRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
