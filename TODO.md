
- ~~Add a stop button (same action as the notification) on the home screen, next to the gps/imu/battery indicators.~~ (done)

- ~~Investigate why taping stop on the notification do not change the gps satellite count. It should go to zero as the stop action should kill all location related features.~~ (done)

- Could the auto pause settings be something we apply to traces after the fact? Less stress for the user to make a choice that is not reversible (they make one choice, and then physically do the ride).

- ~~Could we use another icon for the home/race screen in the bottom nav? A home button feels a bit out of place in the context of this app and what the particular screen is about.~~ (done)

---

## Testing debt

The unit test suite (domain, fusion, export, util) is in good shape — plain JUnit4, fast, reliable. The instrumented layer has two serious problems.

### NavigationTest is completely stale

`NavigationTest` references UI text and content descriptions from an old version of the app:
`"START RACE"`, `"Past Sessions"`, `"Recording Settings"`, `onNodeWithContentDescription("Sessions")`, `onNodeWithContentDescription("Race")`, `"ID: test-session-123"`, `"Export CSV"` — none of these exist in the current UI. If run today every assertion fails. This file gives false confidence: CI would show it passing only because it is probably never executed.

Action: rewrite `NavigationTest` against the current nav graph, or delete it and start fresh. The current screen labels are "Ride", "Trips", "Stats", "Settings"; the home screen heading is "Ready to ride"; etc.

### No isolated Compose UI test infrastructure

There are no `createComposeRule()`-based tests anywhere. UI behaviour (conditional rendering, click callbacks, state-driven visibility) is entirely untested. This made it impossible to write a clean test for the stop-chip change (item 1 above) without first establishing the pattern from scratch.

What is missing:
- A composable test for `RideHomeScreen`: assert the Stop chip is visible only when `serviceState.isSensorsEnabled = true`, is hidden when false, and that clicking it fires `onStop`.
- A general pattern for screen-level composable tests so future UI changes can be verified cheaply. The dependency (`androidx.compose.ui:ui-test-junit4`) is already in the BOM — it just needs to be added to `androidTestImplementation` in `build.gradle.kts`.

### RaceRecordingServiceTest misses the satellite-reset assertion

`testSensorsToggleAndCollection` verifies `isSensorsEnabled` goes false after stopping, but does not assert that `satellites` and `currentAccuracyM` are cleared. The bug fixed in commit `a3bade2` would not have been caught by the existing test. Add assertions:

```kotlin
assertEquals(0, RaceRecordingService.state.value.satellites.usedInFix)
assertEquals(0, RaceRecordingService.state.value.satellites.visible)
assertNull(RaceRecordingService.state.value.currentAccuracyM)
```

The test also uses bare `Thread.sleep` for synchronization, which is fragile. Consider replacing with a polling helper (e.g. `awaitCondition { RaceRecordingService.state.value.isSensorsEnabled }`) or using `turbine` for flow assertions.

### ExampleInstrumentedTest is still the boilerplate stub

`ExampleInstrumentedTest.kt` was never removed or repurposed. Delete it to keep the test surface honest.
