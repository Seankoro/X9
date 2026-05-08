# Group 19 Assignment 2 Report
Members: Loo Zhi Yi, Sean Elisha Koh Tze Li

## Overview - Table of contents:

1. **[Design Choices](#design-choices)**
2. **[User Interfaces](#user-interfaces)**
3. **[Extensions](#extensions)**
4. **[Testing and Evaluation](#testing-and-evaluation)**
5. **[Problems](#problems)**

---

## Design Choices

### Architecture choices

The app follows **MVVM (Model-View-ViewModel)** with the **Repository pattern** that is accessed 
through the different classes through an Application singleton `X9Application`.

Each layer of the application namely, Model, Repository, ViewModel and UI, follows the single-responsibility
principle.

- **Model**: Data classes, `TrafficReport`, `User` and serialization logic with Firebase Realtime database
- **Repository**: Data access to external services such as: Firebase Realtime DB, Firebase Storage,
Firebase Auth, Geofencing, Geocoding etc.
- **ViewModel**: Holds the business logic and the reactive state

### Data classes

The two main data classes in the application are `TrafficReport` and `User`.

![Data Classes](img/A2/data-classes.png)

> _Note: Class diagram generated using Mermaid syntax and exported to .PNG files using online mermaid
> editor_

---

## User Interfaces

There are 7 main screens designed in X9, namely `DashboardScreen`, `LoginScreen`, `MapScreen`, 
`ProfileScreen`, `ReportDetailScreen`, `ReportFormScreen`, `ReportListScreen`. We can discuss the 
more prominent screens in X9 and briefly the design considerations behind each of these screens.

### `DashboardScreen`

![DashboardScreen](img/A2/dashboard.png)

- At the top of the dashboard, there is a `2x2` stat card that provides quick filter options to
reports. E.g., there is the filter for `Active Reports` and `Resolved Today` reports. There is also
filters to filter between your own created reports and critical reports. These quick filter options
allow users to quickly find reports that are of interest to them.
- There are 2 `Quick Actions cards` namely `New Report` and `View Map` which allows users to quickly
access the report creation workflow and view the maps quickly.
- The `Recent Reports` section provides a list of summarized reports. Users can read recent reports 
at a quick glance. A `View All` button also allows the users to access the list of all reports in 
detail.
- The `Bottom Nav Bar` is always at the bottom of X9. It provides clickable navigation buttons for 
users regardless of where they are at in X9. The buttons are also named in such a way that their 
functionality is self-explanatory to prevent any confusion among users.

### Map Screen

![Map Screen](img/A2/map-screen.png)

- This is the Map screen where all reports are being shown as Red pins (< 4 in a cluster) or a red 
circle labeled with the count of reports in that cluster.
- Adopted a similar UI to an already familiar Google Maps using `Google Maps API` to prevent confusions.

### Report Details screen

![Report Details Screen](img/A2/report-details.png)

- This page shows all details of a report to users. This includes fields like location, report type,
severity, description, pictures (if any).
- In this page, there is also authorization guard rails in place such that only the creators of that
report can delete and edit them.

### Report creation screen

![Reoport creation screen](img/A2/report-form.png)

- This page is where users create new reports. They must fill in the Report type, Description, choose
a severity level and upload images if they want to.
- Geolocation data is prepopulated automatically based on user's current location, it cannot be changed
otherwise.
- User's location is also represented by a small Google Maps composable and reverse Geocoded addresses.
This provides users with both visual and textual ways to check if their current location is correct.

---

## Extensions

1. Geoproximity enabled notifications **_(Assignment 2 extra feature)_**

    - Similar to existing traffic navigation apps (Google Maps, Waze etc.), when users are within 
    the proximity of certain traffic reports, users will receive a notification about them.
    - We will implement Geofences of 500m around all reports with severity `HIGH` or `CRITICAL` and
    will notify users about them with a cooldown of 1 hour that is stored using `SharedPreferences`. 
    This prevents the users from being spammed with notifications everytime X9 syncs with Firebase.
    - **Rationale**: Alerting users of the reports that concerns then while commuting, allowing them
    to take relevant actions to protect themselves.

    ![Severity sorted notifications](img/A2/ordered-notifications.png)

    - **Tech stack**: Explored GeoFencing API to create Geofences for each of the reports and used a
    monitoring object to keep track of these Geofences and alert the users when necessary.

2. Speech-enabled report creation **_(Assignment 2 extra feature)_**

    - With a tap of a single button, the user will be able to create a report through speech.
    - **Rationale**: When users are commuting with the app, having to manually provide text input to
    create reports will distract them from the actual road conditions. Having a one-tap work flow to
    create reports allows them to focus on real-life traffic conditions while alerting X9 community
    about road conditions. 
   
    ![Speech enabled report creation](img/A2/speech-enabled-report-creation.png)
   
    - **Tech stack**: Explored Android SpeechRecognizer library which allows us to parse user's audio
    input into text tokens. These tokens are then processed and triggers their respective workflows 
    in the service layer of the application.
    > _Note: This feature is just a demo-feature and does not have full NLP capabilities and behavior is
    > largely restricted by simple keyword-based routing for code execution._

3. Report clustering on Application's Map screen

    - Each report is represented as a pin on the Maps screen which results in overlapping pins and a
    bad UX for users.
    - **Tech Stack**: Used `maps-compose-utils` library which provides a `clustering` composable.
    Reports are passed as cluster items. The library handles distance-based grouping automatically. 
    We also configured a threshold of 4 reports to trigger clustering into a red bubble count.
    - To allow users to navigate between each of the reports in the cluster, we designed a small 
    scrollable bar with the brief report details at the bottom of the maps. This allows users to 
    navigate between reports and view full details if they wanted to.
    
    ![Report Clustering Example](img/A2/report-clustering.png)

    > _Image showing report clustering on the map screen with scrollable report details_

4. Offline persistence for Firebase realtime database

    - As covered in the lecture, we implemented offline persistence in X9 using 
    `Firebase.database.setPersistenceEnabled(true)` in `X9Application.kt`. 
    - Including this before any uses of Firebase realtime database application code allows for local
    reports to be cached offline on the user's device and then synced to Firebase realtime database 
    when the users have internet connection again.

5. NavRoutes sealed navigation object

    - Highlighted in Assignment 1 feedback, we were manually configuring navigation in X9. During 
    re-submission, we refactored our manual navigation approach to Compose Navigation using
    `NavHost` + `NavController`.
    - In `MainActivity.kt` we defined the routes using raw string literals as shown in the code below:

    ```kotlin
    // Snippet from MainActivity.kt describing NavHost + NavController navigation
    
    // rest of file ...
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
            LoginScreen(
                isLoading = isLoading,
                onSignInWithEmail = { e, p -> authViewModel.signInWithEmail(e, p) },
                onRegisterWithEmail = { name, e, p -> authViewModel.registerWithEmail(name, e, p) },
                onSignInWithGoogle = { context -> authViewModel.signInWithGoogleCredential(context) },
                onContinueAsGuest = { navController.popBackStack() },
                paddingValues = paddingValues
            )
        }
        // rest of file ...
   }
    ```

    - While developing, we ran into a problem where there was a typo in the raw string literal
    defining the route. This resulted in a silent run-time error which took us a really long time to
    debug. We realized that having raw string literals in this navigation approach is not maintainable
    and error-prone. For the scale of X9, it might still be acceptable but in actual production-level
    applications, errors will be made easily. 
    - Hence, we created a sealed `NavRoutes` object to centralize the defining of routes to a single
    sealed object that can be accessed in other parts of the application. This prevents mistakes 
    like what we made from happening and makes the codebase more maintainable as changes to the routes
    only need to be made in one central location.

    ```kotlin
   // NavRoutes.kt
    
    object NavRoutes {
        const val LOGIN = "login"
        const val HOME = "home"
        const val REPORTS = "reports"
        const val REPORTS_ROUTE = "reports?myReportsOnly={myReportsOnly}&criticalOnly={criticalOnly}"
        const val ADD = "add"
        const val DETAIL_ROUTE = "detail/{reportId}"
        const val EDIT_ROUTE = "edit/{reportId}"
        const val MAP = "map"
        const val PROFILE = "profile"
    
        fun detail(reportId: String) = "detail/$reportId"
        fun edit(reportId: String) = "edit/$reportId"
        fun reports(myReportsOnly: Boolean = false, criticalOnly: Boolean = false): String =
            if (!myReportsOnly && !criticalOnly) REPORTS
            else "$REPORTS?myReportsOnly=$myReportsOnly&criticalOnly=$criticalOnly"
    }
    ```
   
    > _`NavRoutes.kt` file showing where the routes are being defined_

    ```kotlin
    // MainActivity.kt
    
    // rest of file ...
    LaunchedEffect(currentUser) {
        if (currentUser != null && currentRoute == NavRoutes.LOGIN) {
            navController.navigate(NavRoutes.HOME) {
                popUpTo(NavRoutes.LOGIN) { inclusive = true }
            }
        }
    }
    // rest of file ...
    ```

    > _`MainActivity.kt` where it wires all the routing using routes defined in `NavRoutes.kt`_
  
---
   
## Testing and Evaluation

Before each iteration of the application, we will look through the requirements stated in the weekly
exercises and work plans. We will come up with a simple internal document which describes the expected
behavior of the application after this iteration. We understand that the best development practice is
to write unit-tests using `Junit`. But in the interest of time and considering the scale of the project
we decided that conducting smoke tests after each iteration will suffice.

Both physical Android devices and Android studio's built-in Android emulator are being used during
testing. Usually, physical devices are used for convenience and to get a "real feel" of how X9 will 
feel on real phones. The emulator is only used for specific features (geo-proximity enabled notifications)
to spoof the GPS location to test if the notifications are triggered correctly.

The application is also developed in such a way that there is sufficient logging at key code execution
points, which aids our debugging process when faced with problems during the smoke tests. Since there
is no solid testing pipelines in place, we try to limit the blast radius of potential issues by 
adopting trunk-based development. Starting each iteration in their respective branches and only 
merging back to `main` when we are satisfied with the behavior. Given more time, we will want to 
explore incorporating a `JUnit` testing pipeline to ensure the correctness of the code and eliminate
any human errors in the testing process.

> Example of internal document used for smoke tests:
> ```text
> X9-Version-6 requirements:
> 1.) Successful sign-up work flow.
> - Offloaded to firebase auth, just need to test if new users can create account successfully
> - Edge cases: duplicate emails, usernames etc. 
> 2.) Successful log-in, log-out workflow
> - Users should be authenticate and terminate their session gracefully while using the application
> - Check logcat for correct detection of authentication state.
> 3.) Authentication and authorization checks
> - ALL users can read the traffic reports, no matter authenticated or not
> - For update and delete workflows, authorization workflow is such that only creators of the reports 
> themselves can update and delete the reports.
> ```

---

## Problems

1. Problematic place search.

    - Initially when we were implementing Google Maps functionality into X9, we wanted to provide 
    users with the functionality to perform place search, like how they would usually search for 
    places on Google Maps. This allowed users to create reports at locations that they are not 
    physically at. However, the free API that we were using from `Geocoding API` was buggy, and we 
    could not replicate the behavior of Google Maps properly.
    - After much thought, we decided to implement the report creation workflow such that newly 
    created reports will be geolocated automatically with the user's current location. This aligns
    with how Google Maps naviagation does it.

2. Geofence always fails to register on first launch

    - Permission requesting was done at the wrong places previously. `ACCESS_FINE_LOCATION` and 
    `ACCESS_COARSE_LOCATION` was only requested when users first access the Maps page of X9. For first
    time users, when the app is first launched, neither `ACCESS_FINE_LOCATION` nor `ACCESS_COARSE_LOCATION`
    has been granted yet so `ACCESS_BACKGROUND_LOCATION` does not work.
    - Furthermore, there is an inherent race condition that cannot be avoided in the registering of
    Geofences. Geofences is registered once Firebase Realtime database emits the reports and without
    these permissions working correctly, Geofences cannot be registered correctly.
   
    ![Geofence register error](img/A2/geofence-logcat-error.png)

    - To fix this, we implemented a guard + retry mechanism in `MainActivity.kt` that allows Geofences
    to fail silently when the permissions are not granted correctly. When they are granted by the users,
    the Geofences will then be triggered to be registered again. With this fix, Geofences will be 
    registered properly and new users will be able to get geo-proximity enabled notifications even if
    it is their first time using the app.

3. Lack of severity sorting for report notifications

    - The notifications are correctly showing for reports with severity `HIGH` and `CRITICAL` but
    they are not sorted in any order. `HIGH` severity reports can be shown before `CRITICAL` reports
    in the notification shade.
    - Even though this is purely a UI/UX error, we developed with the usability of X9 in mind. As a 
    user, I will be more concerned about `CRITICAL` reports compared to `HIGH` reports.
    - To fix this, we sorted the reports according to severity such that the most severe report is
    shown at the top of the notification shade.
    
    ![Severity sorted notifications](img/A2/ordered-notifications.png)

4. Firebase type mismatch during deserialization

    - Firebase Realtime DB stores numbers without a type tag. When reading back a `Double`, latitude/
    longitude that happens to be a whole number _(e.g. 55.0)_. Firebase returns it as a `long`, 
    casting it directly to `Double` throws an exception at runtime.
    - The `ENUM` defined also cannot be deserialized directly to Firebase Realtime DB.
    - To fix these issues, the `TrafficReportSerializer.kt` was implemented with functions like
    `DataSnapshot.toTrafficReport()` extension function convert all numeric fields via 
    `toString().toDoubleOrNull()` to handle both `Long` and `Double` values transparently. The `Int`
    and string fo the `ENUM` is also used in the serializer to safely convert between an `ENUM` and
    what is stored in the database.

5. Difficulty to fully automate report creation using speech

    - Initially, reports are validated such that descriptions of each report is non-nullable. We
    tried to use SpeechRecognizer to automate the full report creation process, including filling up
    the description field. However, we were unable to come up with a reliable way to effectively 
    process the text tokens parsed by `SpeechRecognizer` and 'make sense' of the text tokens.
    - We came to the conclusion that we need to pass the input to a NLP model to process the data
    and create a Report object with the relevant fields filled up accordingly. Lacking monetary 
    resources and access to an API key, we decided to give up on this automation.
    - We came to a workaround where descriptions can now be left blank and reports can be created
    using speech with simple keyword routing. A code snippet showing the simple keyword routing is
    shown below:
   
    ```kotlin
   // Snippet from the file SpeechParser.kt showing keyword mapping between speech tokens and Severity level
   
       // Helper to match parsed speech tokens to severity levels
       private val severityKeywords : List<Pair<String, Severity>> = listOf(
           "critical" to Severity.CRITICAL,
           "severe" to Severity.CRITICAL,
           "danger" to Severity.CRITICAL,
           "serious" to Severity.HIGH,
           "moderate" to Severity.MODERATE,
           "medium" to Severity.MODERATE,
           "minor" to Severity.MINOR,
           "high" to Severity.HIGH,
           "low" to Severity.LOW
       )
   ```

    - This simple object handles the mapping logic between the parsed speech tokens (words spoken by
    users) to report types and severity levels respectively. From the design of this object, we can
    see that the list is non-exhaustive. There are countless synonyms to words and any words that 
    fall outside the mapping will not make sense to the application.
    - For MVP demo purposes, we have decided that this behavior is acceptable and given resources and
    access to API tokens, we can make the controller level 'smarter' and truly automate the report
    creation process.
