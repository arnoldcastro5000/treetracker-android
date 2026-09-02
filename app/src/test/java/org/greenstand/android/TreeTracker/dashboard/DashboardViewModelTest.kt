/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.dashboard

import android.content.Context
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.background.NotificationConstants
import org.greenstand.android.TreeTracker.background.TreeSyncWorker
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DashboardViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    private lateinit var dao: TreeTrackerDAO

    @MockK(relaxed = true)
    private lateinit var workManager: WorkManager

    @MockK(relaxed = true)
    private lateinit var analytics: Analytics

    @MockK(relaxed = true)
    private lateinit var treesToSyncHelper: TreesToSyncHelper

    @MockK(relaxed = true)
    private lateinit var orgRepo: OrgRepo

    @MockK(relaxed = true)
    private lateinit var messagesRepo: MessagesRepo

    @MockK(relaxed = true)
    private lateinit var checkForInternetUseCase: CheckForInternetUseCase

    @MockK(relaxed = true)
    private lateinit var locationDataCapturer: LocationDataCapturer
    private lateinit var testSubject: DashboardViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        coEvery { analytics.syncButtonTapped(any(), any(), any()) } just Runs
        coEvery { dao.getUploadedLegacyTreeImageCount() } returns 3
        coEvery { dao.getUploadedTreeImageCount() } returns 5
        coEvery { dao.getNonUploadedLegacyTreeCaptureImageCount() } returns 2
        coEvery { dao.getNonUploadedTreeImageCount() } returns 4
        coEvery { checkForInternetUseCase.execute(Unit) } returns true
        coEvery { messagesRepo.syncMessages() } just Runs
        coEvery { treesToSyncHelper.getTreeCountToSync() } returns 6
        coEvery { orgRepo.getOrgs() } returns FakeFileGenerator.fakeOrganizationList
        coEvery { messagesRepo.checkForUnreadMessages() } returns false
        every { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) } returns mockk()
        testSubject =
            DashboardViewModel(
                dao = dao,
                workManager = workManager,
                analytics = analytics,
                treesToSyncHelper = treesToSyncHelper,
                orgRepo = orgRepo,
                messagesRepo = messagesRepo,
                checkForInternetUseCase = checkForInternetUseCase,
                locationDataCapturer = locationDataCapturer,
            )
    }

    @Test
    fun `syncMessages should call syncMessages on messagesRepo if there is internet connection`() =
        runTest {
            coEvery { checkForInternetUseCase.execute(Unit) } returns true
            coEvery { messagesRepo.syncMessages() } just Runs
            testSubject.handleAction(DashboardAction.SyncMessages)
            coVerify { messagesRepo.syncMessages() }
        }

    @Test
    fun `syncMessages should not call syncMessages on messagesRepo if there is no internet connection`() =
        runTest {
            coEvery { checkForInternetUseCase.execute(Unit) } returns false
            testSubject.handleAction(DashboardAction.SyncMessages)
            coVerify(exactly = 0) { messagesRepo.syncMessages() }
        }

    @Test
    fun `updateData should update the state with correct values querying totalTreesToSync`() =
        runTest {
            // updateData() runs on Dispatchers.IO, so wait for state to be populated
            testSubject.state.first { it.totalTreesToSync != 0 }
            assertEquals(6, testSubject.state.value.totalTreesToSync)
        }

    @Test
    fun `sync should start sync if not syncing and there are trees to sync`() =
        runTest {
            coEvery { checkForInternetUseCase.execute(Unit) } returns true
            testSubject.handleAction(DashboardAction.Sync)
            coVerify(exactly = 1) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
            coVerify { analytics.syncButtonTapped(any(), any(), any()) }
        }

    // The tests above assert the enqueue against a MOCKED WorkManager (a method call with any() args).
    // The tests below assert it against a REAL in-memory WorkManager (WorkManagerTestInitHelper), so
    // they verify the actual request shape: the correct unique-work name and the TreeSyncWorker class.
    // A no-op WorkerFactory replaces TreeSyncWorker so its real Koin upload never runs (the true upload
    // is covered cross-process by the Appium full-stack E2E). See MONO/.scratch/volunteer-e2e-ci/issues/45.

    @Test
    fun `sync enqueues unique TreeSyncWorker work when there are trees to sync`() =
        runTest {
            val realWorkManager = realTestWorkManager()
            val viewModel = viewModelWith(realWorkManager)

            viewModel.handleAction(DashboardAction.Sync)

            val infos = realWorkManager.getWorkInfosForUniqueWork(NotificationConstants.UNIQUE_WORK_ID).get()
            assertEquals(1, infos.size)
            assertTrue(
                "the enqueued unique work must be a TreeSyncWorker",
                infos.first().tags.contains(TreeSyncWorker::class.java.name),
            )
        }

    @Test
    fun `sync does not enqueue work when there are no trees to sync`() =
        runTest {
            coEvery { treesToSyncHelper.getTreeCountToSync() } returns 0
            val realWorkManager = realTestWorkManager()
            val viewModel = viewModelWith(realWorkManager)

            viewModel.handleAction(DashboardAction.Sync)

            val infos = realWorkManager.getWorkInfosForUniqueWork(NotificationConstants.UNIQUE_WORK_ID).get()
            assertTrue("no sync work must be enqueued when there is nothing to sync", infos.isEmpty())
        }

    private fun viewModelWith(workManager: WorkManager): DashboardViewModel =
        DashboardViewModel(
            dao = dao,
            workManager = workManager,
            analytics = analytics,
            treesToSyncHelper = treesToSyncHelper,
            orgRepo = orgRepo,
            messagesRepo = messagesRepo,
            checkForInternetUseCase = checkForInternetUseCase,
            locationDataCapturer = locationDataCapturer,
        )

    private fun realTestWorkManager(): WorkManager {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config =
            Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .setExecutor(SynchronousExecutor())
                .setWorkerFactory(NoOpWorkerFactory())
                .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        return WorkManager.getInstance(context)
    }

    /** Returns a trivial worker for any class, so the real TreeSyncWorker (Koin upload) never runs. */
    private class NoOpWorkerFactory : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
        ): ListenableWorker =
            object : Worker(appContext, workerParameters) {
                override fun doWork(): Result = Result.success()
            }
    }
}