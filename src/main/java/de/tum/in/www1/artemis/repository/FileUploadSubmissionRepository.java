package de.tum.in.www1.artemis.repository;

import de.tum.in.www1.artemis.domain.FileUploadSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * Spring Data JPA repository for the FileUploadSubmission entity.
 */
@SuppressWarnings("unused")
@Repository
public interface FileUploadSubmissionRepository extends JpaRepository<FileUploadSubmission, Long> {

}
