/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */

package org.elasticsearch.gradle.internal;

/**
 * This class models the different Docker base images that are used to build Docker distributions of Elasticsearch.
 */
public enum DockerBase {
    DEFAULT("ubuntu:20.04", "", "apt-get"),

    // "latest" here is intentional, since the image name specifies "8"
    UBI("docker.elastic.co/ubi8/ubi-minimal:latest", "-ubi8", "microdnf"),

    // The Iron Bank base image is UBI (albeit hardened), but we are required to parameterize the Docker build
    IRON_BANK("${BASE_REGISTRY}/${BASE_IMAGE}:${BASE_TAG}", "-ironbank", "yum"),

    // Based on CLOUD above, with more extras. We don't set a base image because
    // we programmatically extend from the Cloud image.
    CLOUD_ESS(null, "-cloud-ess", "apt-get"),

    // Chainguard based wolfi image with latest jdk
    // This is usually updated via renovatebot
    // spotless:off
    WOLFI("docker.elastic.co/wolfi/chainguard-base:latest@sha256:dd66beec64a7f9b19c6c35a1195153b2b630a55e16ec71949ed5187c5947eea1",
        "-wolfi",
        "apk"
    );
    // spotless:on
    private final String image;
    private final String suffix;
    private final String packageManager;

    DockerBase(String image, String suffix) {
        this(image, suffix, "apt-get");
    }

    DockerBase(String image, String suffix, String packageManager) {
        this.image = image;
        this.suffix = suffix;
        this.packageManager = packageManager;
    }

    public String getImage() {
        return image;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getPackageManager() {
        return packageManager;
    }
}
