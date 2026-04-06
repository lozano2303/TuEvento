package com.capysoft.tuevento.modules.storage.application.port.out;

public interface StorageClientPort {

    /**
     * Uploads a file to the storage provider.
     *
     * @return the s3Key of the uploaded object
     */
    String uploadFile(String bucket, String key, byte[] content, String contentType);

    /** Permanently deletes a file from the storage provider. */
    void deleteFile(String bucket, String key);

    /**
     * Generates a permanent public URL for a file.
     *
     * @return the public URL string
     */
    String generatePublicUrl(String bucket, String key);

    /**
     * Generates a temporary pre-signed URL for a file.
     *
     * @param expirationMinutes how long the URL remains valid
     * @return the pre-signed URL string
     */
    String generatePresignedUrl(String bucket, String key, int expirationMinutes);
}
