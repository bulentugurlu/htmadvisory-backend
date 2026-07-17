package org.htmadvisory.platform.documents;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thin wrapper around the GCS client, scoped to the one private bucket.
 *
 * <p>Auth is Application Default Credentials — on Cloud Run this resolves
 * automatically to the attached service account, which was granted
 * {@code roles/storage.objectViewer} on this bucket specifically (not the
 * public one, and not project-wide). No key file involved, nothing to
 * rotate. Local dev needs {@code gcloud auth application-default login}
 * for this to work at all; that's expected — most local dev on this
 * endpoint won't need to hit real GCS.
 */
@Service
public class PrivateDocumentStorageService {

    private final Storage storage;
    private final String bucketName;

    public PrivateDocumentStorageService(@Value("${htm.documents.private-bucket}") String bucketName) {
        this.bucketName = bucketName;
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    /**
     * Fetches the blob for a known object name. Callers are expected to
     * have already resolved that name via {@link PrivateDocumentCatalog} —
     * this class has no opinion on what's a legitimate object to fetch,
     * that validation happens one layer up.
     */
    public Blob fetch(String objectName) {
        Blob blob = storage.get(BlobId.of(bucketName, objectName));
        if (blob == null || !blob.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Document not found in storage — catalog entry may be stale");
        }
        return blob;
    }
}
