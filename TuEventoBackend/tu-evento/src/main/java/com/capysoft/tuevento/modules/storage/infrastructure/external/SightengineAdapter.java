package com.capysoft.tuevento.modules.storage.infrastructure.external;

import com.capysoft.tuevento.modules.storage.infrastructure.config.ModerationConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Calls the Sightengine REST API to detect gore, nudity, suggestive/hentai
 * content, and weapons. Only invoked after OpenNSFW2 has already approved
 * the image (cascade pattern). Fails open on any infrastructure error.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SightengineAdapter {

    private static final String SIGHTENGINE_URL =
            "https://api.sightengine.com/1.0/check.json";

    private final RestTemplate restTemplate;
    private final ModerationConfig moderationConfig;

    public boolean isSafe(byte[] imageBytes) {
        try {
            ModerationConfig.Sightengine cfg = moderationConfig.getSightengine();

            if (cfg.getApiUser() == null || cfg.getApiUser().isBlank()
                    || cfg.getApiSecret() == null || cfg.getApiSecret().isBlank()) {
                log.warn("Sightengine credentials not configured — failing open");
                return true;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ByteArrayResource resource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() { return "image.jpg"; }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("media",      resource);
            body.add("models",     cfg.getModels());
            body.add("api_user",   cfg.getApiUser());
            body.add("api_secret", cfg.getApiSecret());

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response =
                    restTemplate.postForEntity(SIGHTENGINE_URL, request,
                            (Class<Map<String, Object>>) (Class<?>) Map.class);

            if (response.getBody() == null) {
                log.warn("Sightengine returned empty body — failing open");
                return true;
            }

            Map<String, Object> responseBody = response.getBody();
            log.debug("Sightengine full response: {}", responseBody);

            double threshold = cfg.getThreshold();
            double nudityThreshold = cfg.getNudityThreshold();

            // ── Gore ──────────────────────────────────────────────────────────
            if (isFieldUnsafe(responseBody, "gore", "prob", threshold)) {
                log.warn("Sightengine rejected: gore");
                return false;
            }

            // ── Nudity / Suggestive / Hentai / Besos con piel expuesta ───────
            @SuppressWarnings("unchecked")
            Map<String, Object> nudity = (Map<String, Object>) responseBody.get("nudity");
            if (nudity != null) {

                // raw/safe/partial: siempre presentes, tanto con el modelo
                // clásico "nudity" como con "nudity-2.0". Es la red de
                // seguridad principal — sin esto, si nudity-2.0 no está
                // activo o Sightengine no devuelve alguna subcategoría,
                // el contenido explícito pasa sin ser detectado (bug real
                // que dejó pasar una imagen con raw=0.8).
                double raw = getDouble(nudity, "raw");
                double partial = getDouble(nudity, "partial");

                // TODO(calibración ~2026-08-20): bajar este log a debug o eliminarlo una vez
                // que haya suficientes datos reales para confirmar que el threshold (0.50) tiene
                // margen cómodo. Dejarlo en INFO indefinidamente llena los logs de producción.
                // Ver: https://github.com/capysoft/tuevento — issue de seguimiento del threshold NSFW.
                log.info("Sightengine nudity scores — raw:{} partial:{} threshold:{}", raw, partial, nudityThreshold);

                if (raw >= nudityThreshold || partial >= nudityThreshold) {
                    log.warn("Sightengine rejected: nudity raw/partial ({}/{})", raw, partial);
                    return false;
                }

                // Subcategorías finas de nudity-2.0. Si el modelo activo es
                // solo "nudity" (clásico), estos campos no vienen en la
                // respuesta y getDouble() devuelve 0.0 de forma segura —
                // no rompe nada, simplemente no aportan nada hasta que
                // actives nudity-2.0 en SIGHTENGINE_MODELS.
                double sexualActivity = getDouble(nudity, "sexual_activity");
                double sexualDisplay  = getDouble(nudity, "sexual_display");
                double erotica        = getDouble(nudity, "erotica");
                double suggestive     = getDouble(nudity, "suggestive");
                double sextoy         = getDouble(nudity, "sextoy");

                log.debug("Sightengine nudity — sexual_activity:{} sexual_display:{} erotica:{} suggestive:{} sextoy:{}",
                        sexualActivity, sexualDisplay, erotica, suggestive, sextoy);

                if (sexualActivity >= nudityThreshold
                        || sexualDisplay >= nudityThreshold
                        || erotica >= nudityThreshold
                        || sextoy >= nudityThreshold
                        || suggestive >= nudityThreshold) {
                    log.warn("Sightengine rejected: nudity/suggestive");
                    return false;
                }

                // Subcategorías sugestivas: lencería, minifalda, escote (normal y
                // muy revelador). Cubre mejor besos/contacto con piel expuesta y
                // contenido tipo hentai que no siempre eleva sexual_activity.
                @SuppressWarnings("unchecked")
                Map<String, Object> suggestiveClasses =
                        (Map<String, Object>) nudity.get("suggestive_classes");
                if (suggestiveClasses != null) {
                    double lingerie  = getDouble(suggestiveClasses, "lingerie");
                    double miniskirt = getDouble(suggestiveClasses, "miniskirt");
                    double bikini    = getDouble(suggestiveClasses, "bikini");
                    double cleavage  = getDouble(suggestiveClasses, "cleavage");
                    double other     = getDouble(suggestiveClasses, "other");

                    double maleChest      = getDouble(suggestiveClasses, "male_chest");
                    double maleUnderwear  = getDouble(suggestiveClasses, "male_underwear");

                    @SuppressWarnings("unchecked")
                    Map<String, Object> cleavageCategories =
                            (Map<String, Object>) suggestiveClasses.get("cleavage_categories");
                    double veryRevealingCleavage = cleavageCategories != null
                            ? getDouble(cleavageCategories, "very_revealing") : 0.0;
                    double revealingCleavage = cleavageCategories != null
                            ? getDouble(cleavageCategories, "revealing") : 0.0;

                    @SuppressWarnings("unchecked")
                    Map<String, Object> maleChestCategories =
                            (Map<String, Object>) suggestiveClasses.get("male_chest_categories");
                    double veryRevealingMaleChest = maleChestCategories != null
                            ? getDouble(maleChestCategories, "very_revealing") : 0.0;
                    double revealingMaleChest = maleChestCategories != null
                            ? getDouble(maleChestCategories, "revealing") : 0.0;
                    double slightlyRevealingMaleChest = maleChestCategories != null
                            ? getDouble(maleChestCategories, "slightly_revealing") : 0.0;

                    log.debug("Sightengine suggestive_classes — lingerie:{} miniskirt:{} bikini:{} cleavage:{} other:{} male_chest:{} male_underwear:{} veryRevealingCleavage:{} revealingCleavage:{} veryRevealingMaleChest:{} revealingMaleChest:{} slightlyRevealingMaleChest:{}",
                            lingerie, miniskirt, bikini, cleavage, other, maleChest, maleUnderwear, veryRevealingCleavage, revealingCleavage, veryRevealingMaleChest, revealingMaleChest, slightlyRevealingMaleChest);

                    if (lingerie >= nudityThreshold
                            || miniskirt >= nudityThreshold
                            || cleavage >= nudityThreshold
                            || other >= nudityThreshold
                            || veryRevealingCleavage >= nudityThreshold
                            || revealingCleavage >= nudityThreshold
                            || maleChest >= nudityThreshold
                            || maleUnderwear >= nudityThreshold
                            || veryRevealingMaleChest >= nudityThreshold
                            || revealingMaleChest >= nudityThreshold
                            || slightlyRevealingMaleChest >= nudityThreshold) {
                        log.warn("Sightengine rejected: nudity (suggestive_classes)");
                        return false;
                    }
                }
            }

            // ── Weapon ────────────────────────────────────────────────────────
            @SuppressWarnings("unchecked")
            Map<String, Object> weapon = (Map<String, Object>) responseBody.get("weapon");
            if (weapon != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> classes = (Map<String, Object>) weapon.get("classes");
                if (classes != null) {
                    for (Map.Entry<String, Object> entry : classes.entrySet()) {
                        double prob = getDouble(classes, entry.getKey());
                        if (prob >= threshold) {
                            log.warn("Sightengine rejected: weapon.{} = {}", entry.getKey(), prob);
                            return false;
                        }
                    }
                }
            }

            return true;

        } catch (Exception e) {
            log.error("Sightengine unreachable or failed — failing open: {}", e.getMessage());
            return true;
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private boolean isFieldUnsafe(Map<String, Object> body, String field, String subField, double threshold) {
        Map<String, Object> fieldMap = (Map<String, Object>) body.get(field);
        if (fieldMap == null) return false;
        double prob = getDouble(fieldMap, subField);
        return prob >= threshold;
    }

    private double getDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return 0.0;
        return ((Number) val).doubleValue();
    }
}