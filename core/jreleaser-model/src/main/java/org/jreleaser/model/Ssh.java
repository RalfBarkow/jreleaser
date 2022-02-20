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
package org.jreleaser.model;

/**
 * @author Andres Almiray
 * @since 1.1.0
 */
public interface Ssh {
    String getUsername();

    void setUsername(String username);

    String getPassword();

    void setPassword(String password);

    String getHost();

    void setHost(String host);

    int getPort();

    void setPort(int port);

    String getKnownHostsFile();

    void setKnownHostsFile(String knownHostsFile);

    String getPublicKey();

    void setPublicKey(String publicKey);

    String getPrivateKey();

    void setPrivateKey(String privateKey);

    String getPassphrase();

    void setPassphrase(String passphrase);

    String getFingerprint();

    void setFingerprint(String fingerprint);

    String getResolvedUsername();

    String getResolvedPassword();

    String getResolvedHost();

    Integer getResolvedPort();

    String getResolvedPublicKey();

    String getResolvedPrivateKey();

    String getResolvedPassphrase();

    String getResolvedFingerprint();
}