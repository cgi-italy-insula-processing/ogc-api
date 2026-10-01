package com.cgi.eoss.ogcapi.processes.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

/**
 * Utility class for manipulating and rewriting URLs used in the application.
 */
@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UrlRewriteUtils {

    private static final String SCHEME_SEPARATOR = "://";

    /**
     * Rewrites {@code originalHref} to use {@code targetBaseUri} as scheme/host (and optional base path),
     * while preserving path/query/fragment of the original href.
     */
    public static String rewriteBaseUrl(String originalHref, URI targetBaseUri) {
        if (targetBaseUri == null) {
            return originalHref;
        }
        if (originalHref == null) {
            return null;
        }

        return computeSuffix(originalHref)
                .map(suffix -> joinBaseAndSuffix(targetBaseUri.toString(), suffix))
                .orElse(originalHref);
    }

    private static Optional<String> computeSuffix(String originalHref) {
        return computeSuffixParsed(originalHref)
                .or(() -> computeSuffixByScan(originalHref));
    }

    private static Optional<String> computeSuffixParsed(String originalHref) {
        final UriComponents uriComponents;
        try {
            uriComponents = UriComponentsBuilder.fromUriString(originalHref).build(true);
        } catch (IllegalArgumentException exception) {
            LOG.debug("Unable to parse href '{}' via UriComponentsBuilder.", originalHref, exception);
            return Optional.empty();
        }

        if (uriComponents.getScheme() == null || uriComponents.getHost() == null) {
            return Optional.empty();
        }

        StringBuilder suffix = new StringBuilder();
        if (uriComponents.getPath() != null) {
            suffix.append(uriComponents.getPath());
        }
        if (!uriComponents.getQueryParams().isEmpty()) {
            suffix.append('?').append(uriComponents.getQuery());
        }
        if (uriComponents.getFragment() != null) {
            suffix.append('#').append(uriComponents.getFragment());
        }
        return Optional.of(suffix.toString());
    }

    private static Optional<String> computeSuffixByScan(String originalHref) {
        int schemeIdx = originalHref.indexOf(SCHEME_SEPARATOR);
        if (schemeIdx == -1) {
            return Optional.empty();
        }

        int authorityStartIdx = schemeIdx + SCHEME_SEPARATOR.length();

        int slashIdx = originalHref.indexOf('/', authorityStartIdx);
        int queryIdx = originalHref.indexOf('?', authorityStartIdx);
        int fragIdx  = originalHref.indexOf('#', authorityStartIdx);

        int suffixStartIdx = firstDelimiterIndex(slashIdx, queryIdx, fragIdx);
        String suffix = suffixStartIdx != -1 ? originalHref.substring(suffixStartIdx) : "";
        return Optional.of(suffix);
    }

    private static String joinBaseAndSuffix(String base, String suffix) {
        if (base.endsWith("/") && suffix.startsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + suffix;
    }

    private static int firstDelimiterIndex(int... candidateIndexes) {
        int firstIdx = Integer.MAX_VALUE;
        for (int candidateIdx : candidateIndexes) {
            if (candidateIdx >= 0 && candidateIdx < firstIdx) {
                firstIdx = candidateIdx;
            }
        }
        return firstIdx == Integer.MAX_VALUE ? -1 : firstIdx;
    }
}
