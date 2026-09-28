package dev.infrai.healthupload.infrai;

public interface ModerationGateway {
    ModerationAssessment screen(String caption, String imageUrl);

    record ModerationAssessment(boolean flagged, String model) {}
}
