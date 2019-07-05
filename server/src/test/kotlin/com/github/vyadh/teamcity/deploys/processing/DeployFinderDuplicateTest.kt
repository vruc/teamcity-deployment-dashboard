package com.github.vyadh.teamcity.deploys.processing

import com.github.vyadh.teamcity.deploys.buildfinder.BuildFinder
import com.github.vyadh.teamcity.deploys.buildfinder.LastBuildFinder
import com.github.vyadh.teamcity.deploys.buildfinder.SimulatedBuildHistory
import com.github.vyadh.teamcity.deploys.processing.BuildMocks.buildTypeWith
import com.github.vyadh.teamcity.deploys.processing.BuildMocks.deploymentBuildType
import com.github.vyadh.teamcity.deploys.processing.BuildMocks.lastBuild
import com.github.vyadh.teamcity.deploys.processing.BuildMocks.project
import com.github.vyadh.teamcity.deploys.processing.BuildMocks.regularBuildType
import com.nhaarman.mockitokotlin2.*
import jetbrains.buildServer.messages.Status
import jetbrains.buildServer.serverSide.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*
import kotlin.collections.HashMap

internal class DeployFinderDuplicateTest {

  private val links = BuildMocks.links("http://link")
  private val projectKey = "PROJECT"
  private val versionKey = "VERSION"
  private val envKey = "ENV"

  @Test
  fun searchSeesOnlyLastDatedBuild() {
    val buildFinished = mock<SFinishedBuild> {
      on { buildOwnParameters } doReturn properties("Vega", "1.0", "PRD")
      on { buildStatus } doReturn Status.FAILURE
      on { finishDate } doReturn Date.from(Instant.parse("2019-07-01T10:00:00.00Z"))
    }
    val buildRunning = mock<SRunningBuild> {
      on { buildOwnParameters } doReturn properties("Vega", "1.0", "PRD")
      on { buildStatus } doReturn Status.NORMAL
      on { startDate } doReturn Date.from(Instant.parse("2019-07-01T10:00:01.00Z"))
    }
    val finder = finder(buildFinder = lastBuild(buildFinished))
    val project = project(listOf(
          buildTypeWith(buildRunning),
          deploymentBuildType(),
          regularBuildType()
    ))

    val results = finder.search(project)

    assertThat(results.map { it.status }).containsOnly("RUNNING")
  }

  private fun finder(
        links: WebLinks = this.links,
        projectKey: String = this.projectKey,
        versionKey: String = this.versionKey,
        envKey: String = this.envKey,
        buildFinder: BuildFinder = LastBuildFinder(SimulatedBuildHistory.empty())
  ): DeployFinder {

    return DeployFinder(links, projectKey, versionKey, envKey, buildFinder)
  }

  private fun properties(
        project: String? = null, version: String? = null, env: String? = null
  ): Map<String, String> {

    val map = HashMap<String, String>()
    if (project != null) map[projectKey] = project
    if (version != null) map[versionKey] = version
    if (env != null) map[envKey] = env
    return map
  }

}
