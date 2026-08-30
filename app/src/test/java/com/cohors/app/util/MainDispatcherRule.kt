package com.cohors.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit rule that swaps [Dispatchers.Main] for a [StandardTestDispatcher]
 * for the duration of a test, and resets it afterwards.
 *
 * Usage:
 * ```
 * @get:Rule
 * val mainDispatcherRule = MainDispatcherRule()
 *
 * @Test
 * fun myTest() = runTest(mainDispatcherRule.testDispatcher) { ... }
 * ```
 * Running the test body on the SAME dispatcher instance that backs
 * [Dispatchers.Main] means calls like `advanceUntilIdle()` inside the test
 * also drive any `viewModelScope` coroutines under test.
 */
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
