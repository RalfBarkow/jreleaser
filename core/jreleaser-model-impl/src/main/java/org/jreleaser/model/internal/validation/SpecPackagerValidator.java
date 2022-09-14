/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2022 The JReleaser authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jreleaser.model.internal.validation;

import org.jreleaser.bundle.RB;
import org.jreleaser.model.Active;
import org.jreleaser.model.internal.JReleaserContext;
import org.jreleaser.model.internal.JReleaserModel;
import org.jreleaser.model.internal.common.Artifact;
import org.jreleaser.model.internal.distributions.Distribution;
import org.jreleaser.model.internal.packagers.SpecPackager;
import org.jreleaser.model.internal.release.Releaser;
import org.jreleaser.util.Errors;

import java.util.Collections;
import java.util.List;

import static org.jreleaser.model.internal.validation.DistributionsValidator.validateArtifactPlatforms;
import static org.jreleaser.model.internal.validation.ExtraPropertiesValidator.mergeExtraProperties;
import static org.jreleaser.model.internal.validation.TemplateValidator.validateTemplate;
import static org.jreleaser.util.StringUtils.isBlank;

/**
 * @author Andres Almiray
 * @since 0.9.1
 */
public abstract class SpecPackagerValidator extends Validator {
    public static void validateSpec(JReleaserContext context, Distribution distribution, SpecPackager packager, Errors errors) {
        context.getLogger().debug("distribution.{}.spec", distribution.getName());
        JReleaserModel model = context.getModel();
        SpecPackager parentPackager = model.getPackagers().getSpec();

        if (!packager.isActiveSet() && parentPackager.isActiveSet()) {
            packager.setActive(parentPackager.getActive());
        }
        if (!packager.resolveEnabled(context.getModel().getProject(), distribution)) {
            context.getLogger().debug(RB.$("validation.disabled"));
            packager.disable();
            return;
        }
        Releaser service = model.getRelease().getReleaser();
        if (!service.isReleaseSupported()) {
            context.getLogger().debug(RB.$("validation.disabled.release"));
            packager.disable();
            return;
        }

        List<Artifact> candidateArtifacts = packager.resolveCandidateArtifacts(context, distribution);
        if (candidateArtifacts.size() == 0) {
            packager.setActive(Active.NEVER);
            context.getLogger().debug(RB.$("validation.disabled.no.artifacts"));
            packager.disable();
            return;
        } else if (candidateArtifacts.size() > 1) {
            errors.configuration(RB.$("validation_packager_multiple_artifacts", "distribution." + distribution.getName() + ".spec"));
            context.getLogger().debug(RB.$("validation.disabled.multiple.artifacts"));
            packager.disable();
            return;
        }

        if (isBlank(packager.getRelease())) {
            packager.setRelease(parentPackager.getRelease());
        }
        if (isBlank(packager.getRelease())) {
            packager.setRelease("1");
        }

        try {
            Integer.parseInt(packager.getRelease());
            packager.setRelease(packager.getRelease() + "%{?dist}");
        } catch (NumberFormatException ignored) {
            // ok?
        }

        if (packager.getRequires().isEmpty()) {
            packager.setRequires(parentPackager.getRequires());
        }
        if (packager.getRequires().isEmpty()) {
            packager.setRequires(Collections.singletonList("java"));
        }

        validateCommitAuthor(packager, parentPackager);
        SpecPackager.SpecRepository repository = packager.getRepository();
        repository.resolveEnabled(model.getProject());
        validateTap(context, distribution, repository, parentPackager.getRepository(), "spec.repository");
        validateTemplate(context, distribution, packager, parentPackager, errors);
        mergeExtraProperties(packager, parentPackager);
        validateContinueOnError(packager, parentPackager);
        if (isBlank(packager.getDownloadUrl())) {
            packager.setDownloadUrl(parentPackager.getDownloadUrl());
        }
        validateArtifactPlatforms(context, distribution, packager, candidateArtifacts, errors);

        if (isBlank(packager.getPackageName())) {
            packager.setPackageName(parentPackager.getPackageName());
            if (isBlank(packager.getPackageName())) {
                packager.setPackageName(distribution.getName());
            }
        }
    }
}