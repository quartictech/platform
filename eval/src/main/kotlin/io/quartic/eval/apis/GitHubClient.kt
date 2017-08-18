package io.quartic.eval.apis

import io.quartic.github.GithubInstallationClient.GitHubInstallationAccessToken
import java.util.concurrent.CompletableFuture

interface GitHubClient {
    fun getAccessTokenAsync(installationId: Long): CompletableFuture<GitHubInstallationAccessToken>
}
