package dev.infrai.healthupload.infrai;

public interface ImageOperations {
    UploadedImage upload(byte[] file, String filename, String idempotencyKey);
    PublishedImage resize(String image, String idempotencyKey);

    record UploadedImage(String id, String url) {}
    record PublishedImage(String id, String url) {}
}
