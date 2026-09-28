package dev.infrai.healthupload.infrai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.moderations.ModerationCreateParams;
import com.openai.models.moderations.ModerationCreateResponse;
import com.openai.models.moderations.ModerationImageUrlInput;
import com.openai.models.moderations.ModerationMultiModalInput;
import com.openai.models.moderations.ModerationTextInput;
import dev.infrai.healthupload.config.InfraiProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class InfraiModerationGateway implements ModerationGateway {
    private final OpenAIClient client;

    public InfraiModerationGateway(InfraiProperties config) {
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(config.apiKey())
                .baseUrl(config.baseUrl())
                .maxRetries(config.retry().maxAttempts() - 1)
                .build();
    }

    @Override
    public ModerationAssessment screen(String caption, String imageUrl) {
        ModerationTextInput text = ModerationTextInput.builder().text(caption).build();
        ModerationImageUrlInput image = ModerationImageUrlInput.builder()
                .imageUrl(ModerationImageUrlInput.ImageUrl.builder().url(imageUrl).build())
                .build();
        ModerationCreateParams params = ModerationCreateParams.builder()
                .model("omni-moderation-latest")
                .inputOfModerationMultiModalArray(List.of(
                        ModerationMultiModalInput.ofText(text),
                        ModerationMultiModalInput.ofImageUrl(image)))
                .build();
        ModerationCreateResponse response = client.moderations().create(params);
        boolean flagged = response.results().stream().anyMatch(result -> result.flagged());
        return new ModerationAssessment(flagged, response.model());
    }
}
