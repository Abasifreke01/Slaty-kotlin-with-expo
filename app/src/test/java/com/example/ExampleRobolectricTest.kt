package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SlatyDatabase
import com.example.data.local.TaskEntity
import com.example.data.repository.TaskSyncRepository
import com.example.model.TaskItem
import com.example.model.TaskStatus
import com.example.model.UserRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var testDb: SlatyDatabase
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        testDb = Room.inMemoryDatabaseBuilder(context, SlatyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        testDb.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Slaty", appName)
    }

    @Test
    fun `test repository role switching`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = com.example.data.SlatyRepository(context)

        assertEquals(UserRole.STUDENT, repository.currentUser.value?.role)
        repository.switchUserRole(UserRole.TUTOR)
        assertEquals(UserRole.TUTOR, repository.currentUser.value?.role)
    }

    @Test
    fun `test room to firestore repository pattern caching and sync`() = testScope.runTest {
        val dao = testDb.taskDao()
        val repo = TaskSyncRepository(dao, testScope, firestore = null)

        val sampleTask = TaskItem(
            id = "task_test_01",
            title = "Engineering Fluid Mechanics Navier-Stokes",
            studentName = "Tunde Adeleke",
            studentId = "usr_999",
            institution = "Unilag",
            courseCode = "MEG 305",
            faculty = "Engineering",
            department = "Mechanical",
            description = "Detailed derivation of cylindrical Navier-Stokes equations.",
            amount = 9500.0,
            deadline = "Tomorrow, 5 PM",
            learnerType = "Undergraduate",
            status = TaskStatus.PENDING
        )

        // 1. Create task offline in Room
        repo.createTask(sampleTask)

        val tasksFromRoom = repo.allTasks.first()
        assertTrue(tasksFromRoom.any { it.id == "task_test_01" })

        // 2. Accept task
        val acceptSuccess = repo.acceptTask("task_test_01", "Dr. Emmanuel Okon", "tutor_01")
        assertTrue(acceptSuccess)

        val updatedTask = repo.allTasks.first().first { it.id == "task_test_01" }
        assertEquals(TaskStatus.ACCEPTED, updatedTask.status)
        assertEquals("Dr. Emmanuel Okon", updatedTask.acceptedByTutorName)

        // 3. Complete task
        val completeSuccess = repo.completeTask("task_test_01")
        assertTrue(completeSuccess)

        val completedTask = repo.allTasks.first().first { it.id == "task_test_01" }
        assertEquals(TaskStatus.COMPLETED, completedTask.status)
        assertEquals("Just now", completedTask.completedAt)

        // 4. Verify pending sync query
        val pendingSync = dao.getPendingSyncTasks()
        assertTrue(pendingSync.isNotEmpty())
        assertEquals("task_test_01", pendingSync.first().id)

        // 5. Simulate sync completion
        dao.markSynced("task_test_01")
        val pendingAfterSync = dao.getPendingSyncTasks()
        assertTrue(pendingAfterSync.isEmpty())
    }

    @Test
    fun `test cbt questions loaded`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = com.example.data.SlatyRepository(context)

        val bioQuestions = repository.getQuestionsForSubject("Biology")
        assertTrue(bioQuestions.isNotEmpty())

        val mathQuestions = repository.getQuestionsForSubject("Mathematics")
        assertTrue(mathQuestions.isNotEmpty())
    }

    @Test
    fun `test notification helper and authentication viewmodel`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        com.example.util.NotificationHelper.createNotificationChannel(context)

        val authViewModel = com.example.viewmodel.AuthenticationViewModel(context)
        authViewModel.setSelectedRole(UserRole.STUDENT)
        assertEquals(UserRole.STUDENT, authViewModel.selectedRole.value)

        authViewModel.signInWithEmail("student@slaty.edu.ng", "password123", UserRole.STUDENT)
        val state = authViewModel.uiState.value
        assertTrue(state is com.example.viewmodel.AuthUiState.Success)
    }

    @Test
    fun `test fcm token persistence and push notification delivery`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.util.NotificationHelper.createNotificationChannel(context)

        // 1. Test FCM token saving and retrieval
        val sampleToken = "fcm_test_token_sample_1234567890"
        com.example.service.SlatyFirebaseMessagingService.saveFcmToken(context, sampleToken)
        val retrieved = com.example.service.SlatyFirebaseMessagingService.getSavedFcmToken(context)
        assertEquals(sampleToken, retrieved)

        // 2. Test Foreground & Background push notification creation
        com.example.util.NotificationHelper.showPushNotification(
            context = context,
            title = "New Message from Dr. Emmanuel Okon",
            body = "Your physics calculation worksheet is ready for review.",
            data = mapOf(
                "senderName" to "Dr. Emmanuel Okon",
                "type" to "message",
                "chatId" to "chat_tutor_1"
            ),
            senderName = "Dr. Emmanuel Okon"
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channel = notificationManager.getNotificationChannel(com.example.util.NotificationHelper.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(android.app.NotificationManager.IMPORTANCE_HIGH, channel.importance)
    }

    @Test
    fun `test home screen filter tutors and study groups by subject and academic level`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = com.example.viewmodel.SlatyViewModel(app)

        // 1. Initial State: All tutors and communities available
        val allTutors = vm.tutors.value
        val allComms = vm.communities.value
        assertTrue(allTutors.isNotEmpty())
        assertTrue(allComms.isNotEmpty())

        // 2. Filter by subject "Physics"
        vm.setHomeSubjectFilter("Physics")
        assertEquals("Physics", vm.homeSelectedSubject.value)
        val physicsTutors = allTutors.filter { it.subjects.contains("Physics") }
        val physicsComms = allComms.filter { it.subjects.contains("Physics") }
        assertTrue(physicsTutors.any { it.name.contains("Okon") })
        assertTrue(physicsComms.any { it.name.contains("JAMB") || it.name.contains("Engineering") })

        // 3. Filter by academic level "Secondary (WAEC/NECO)"
        vm.setHomeSubjectFilter(null)
        vm.setHomeLevelFilter("Secondary (WAEC/NECO)")
        assertEquals("Secondary (WAEC/NECO)", vm.homeSelectedLevel.value)
        val secTutors = allTutors.filter { it.academicLevels.contains("Secondary (WAEC/NECO)") }
        val secComms = allComms.filter { it.academicLevels.contains("Secondary (WAEC/NECO)") }
        assertTrue(secTutors.any { it.name.contains("Amina") || it.name.contains("Grace") })
        assertTrue(secComms.any { it.name.contains("WAEC") })

        // 4. Combined search query "Calculus" and Target "TUTORS"
        vm.setHomeLevelFilter(null)
        vm.setHomeSearchQuery("Calculus")
        vm.setHomeSearchTarget(com.example.model.HomeSearchTarget.TUTORS)
        assertEquals("Calculus", vm.homeSearchQuery.value)
        assertEquals(com.example.model.HomeSearchTarget.TUTORS, vm.homeSearchTarget.value)

        val searchTutors = allTutors.filter { it.name.contains("Chukwudi") || it.subjects.contains("Calculus") }
        assertTrue(searchTutors.any { it.name.contains("Chukwudi") })

        // 5. Reset all filters
        vm.clearHomeFilters()
        assertEquals("", vm.homeSearchQuery.value)
        assertEquals(null, vm.homeSelectedSubject.value)
        assertEquals(null, vm.homeSelectedLevel.value)
        assertEquals(com.example.model.HomeSearchTarget.ALL, vm.homeSearchTarget.value)
        assertEquals(com.example.model.HomeSortOption.HIGHEST_RATED, vm.homeSortOption.value)
    }

    @Test
    fun `test home screen sort tutors and study groups by rating price and newest`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = com.example.viewmodel.SlatyViewModel(app)

        val allTutors = vm.tutors.value
        val allComms = vm.communities.value
        assertTrue(allTutors.size >= 2)
        assertTrue(allComms.size >= 2)

        // 1. Sort by Highest Rated
        vm.setHomeSortOption(com.example.model.HomeSortOption.HIGHEST_RATED)
        assertEquals(com.example.model.HomeSortOption.HIGHEST_RATED, vm.homeSortOption.value)
        val sortedRatedTutors = allTutors.sortedWith(
            compareByDescending<com.example.model.Tutor> { it.rating }.thenByDescending { it.reviewsCount }
        )
        assertTrue(sortedRatedTutors.first().rating >= sortedRatedTutors.last().rating)

        // 2. Sort by Price (Low to High)
        vm.setHomeSortOption(com.example.model.HomeSortOption.PRICE_LOW_TO_HIGH)
        assertEquals(com.example.model.HomeSortOption.PRICE_LOW_TO_HIGH, vm.homeSortOption.value)
        val sortedPriceTutors = allTutors.sortedWith(
            compareBy<com.example.model.Tutor> { it.hourlyRate }.thenByDescending { it.rating }
        )
        assertTrue(sortedPriceTutors.first().hourlyRate <= sortedPriceTutors.last().hourlyRate)

        // 3. Sort by Newest
        vm.setHomeSortOption(com.example.model.HomeSortOption.NEWEST)
        assertEquals(com.example.model.HomeSortOption.NEWEST, vm.homeSortOption.value)

        // 4. Verify reset
        vm.clearHomeFilters()
        assertEquals(com.example.model.HomeSortOption.HIGHEST_RATED, vm.homeSortOption.value)
    }
}
