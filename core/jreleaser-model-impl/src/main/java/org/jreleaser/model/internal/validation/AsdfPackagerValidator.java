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
import org.jreleaser.model.internal.packagers.AsdfPackager;
import org.jreleaser.model.internal.release.Releaser;
import org.jreleaser.util.Errors;

import java.util.List;

import static org.jreleaser.model.internal.validation.DistributionsValidator.validateArtifactPlatforms;
import static org.jreleaser.model.internal.validation.ExtraPropertiesValidator.mergeExtraProperties;
import static org.jreleaser.model.internal.validation.TemplateValidator.validateTemplate;
import static org.jreleaser.util.StringUtils.isBlank;
import static org.jreleaser.util.StringUtils.isNotBlank;

/**
 * @author Andres Almiray
 * @since 1.2.0
 */
public abstract class AsdfPackagerValidator extends Validator {
    public static void validateAsdf(JReleaserContext context, Distribution distribution, AsdfPackager packager, Errors errors) {
        context.getLogger().debug("distribution.{}.asdf", distribution.getName());
        JReleaserModel model = context.getModel();
        AsdfPackager parentPackager = model.getPackagers().getAsdf();

        if (!packager.isActiveSet() && parentPackager.isActiveSet()) {
            packager.setActive(parentPackager.getActive());
        }
        if (!packager.resolveEnabled(context.getModel().getProject(), distribution)) {
            context.getLogger().debug(RB.$("validation.disabled"));
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
        } else if (candidateArtifacts.stream()
            .filter(artifact -> isBlank(artifact.getPlatform()))
            .count() > 1) {
            errors.configuration(RB.$("validation_packager_multiple_artifacts", "distribution." + distribution.getName() + ".asdf"));
            context.getLogger().debug(RB.$("validation.disabled.multiple.artifacts"));
            packager.disable();
            return;
        }

        if (isBlank(packager.getToolCheck()) && isNotBlank(parentPackager.getToolCheck())) {
            packager.setToolCheck(parentPackager.getToolCheck());
        }
        if (isBlank(packager.getToolCheck())) {
            packager.setToolCheck("{{distributionExecutable}} --version");
        }

        if (isBlank(packager.getRepository().getName())) {
            packager.getRepository().setName("asdf-" + distribution.getName());
        }
        packager.getRepository().setTapName("asdf-" + distribution.getName());

        validateCommitAuthor(packager, parentPackager);
        AsdfPackager.AsdfRepository repository = packager.getRepository();
        repository.resolveEnabled(model.getProject());
        validateTap(context, distribution, repository, parentPackager.getRepository(), "asdf.repository");
        validateTemplate(context, distribution, packager, parentPackager, errors);
        mergeExtraProperties(packager, parentPackager);
        validateContinueOnError(packager, parentPackager);
        if (isBlank(packager.getDownloadUrl())) {
            packager.setDownloadUrl(parentPackager.getDownloadUrl());
        }
        validateArtifactPlatforms(context, distribution, packager, candidateArtifacts, errors);
    }
}