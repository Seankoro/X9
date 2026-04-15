# Group 19 Assignment 2 Report

## Overview - Table of contents:

1. **[Design Choices](#design-choices)**
2. **[User Interfaces](#user-interfaces)**
3. **[Extensions](#extensions)**
4. **[Testing and Evaluation](#testing-and-evaluation)**
5. **[Problems](#problems)**

---

## Design Choices

## User Interfaces

## Extensions

1. Geoproximity enabled notifications (Assignment 2 extra feature)
    - Similar to existing traffic navigation apps (Google Maps, Waze etc.), when users are within 
    the proximity of an existing traffic report, they will recieve notifications on their device about 
    said reports. They will then be prompted to confirm if the report is still relevant and the status 
    will be updated to the rest of the X9 community in real-time.
    - **Rationale**: Dual purpose of alerting users of only the reports that are immediately relevant
    to them (geolocation wise) and helping to remove stale reports from the application. 
    - TODO: add screenshots of this feature when implemented
    - **Tech stack**: Explored GeoFencing API to create Geofences for each of the reports and used a
    monitoring object to keep track of these Geofences and alert the users when necessary.
2. Speech-enabled report creation (Assignment 2 extra feature)
    - With a tap of a single button, the user will be able to create a report through speech.
    - **Rationale**: When users are commuting with the app, having to manually provide text input to
    create reports will distract them from the actual road conditions. Having a one-tap work flow to
    create reports allows them to focus on real-life traffic conditions while alerting X9 community
    about road conditions. 
    - TODO: add screenshots of this feature when implemented
    - **Tech stack**: Explored Android SpeechRecognizer library which allows us to parse user's audio
    input into text tokens. These tokens are then processed and triggers their respective workflows 
    in the service layer of the application.
    > _Note_: This feature is just a demo-feature and does not have full NLP capabilities and behavior is
    > largely restricted by simple keyword-based routing for code execution.

## Testing and Evaluation

Before each iteration of the application, we will look through the requirements stated in the weekly
exercises and work plans. We will come up with a simple internal document which describes the expected
behavior of the application after this iteration. We understand that the best development practice is
to write unit-tests using `Junit`. But in the interest of time and considering the scale of the project
we decided that conducting smoke tests after each iteration will suffice.

The application is also developed in such a way that there is sufficient logging at key code execution
points, which aids our debugging process when faced with problems during the smoke tests. Since there
is no solid testing pipelines in place, we try to limit the blast radius of potential issues by 
adopting trunk-based development. Starting each iteration in their respective branches and only 
merging back to `main` when we are satisfied with the behavior. Given more time, we will want to 
explore incorporating a `JUnit` testing pipeline to ensure the correctness of the code and eliminate
any human errors in the testing process.

> Example of internal document used for smoke tests:
>
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

## Problems

1. Problematic place search.
    - Initially when we were implementing Google Maps functionality into X9, we wanted to provide 
    users with the functionality to perform place search, like how they would usually search for 
    places on Google Maps. This allowed users to create reports at locations that they are not 
    physically at. However, the free API that we were using from `Geocoding API` was buggy and we 
    could not replicate the behavior of Google Maps properly.
    - After much thought, we decided to implement the report creation workflow such that newly 
    created reports will be geolocated automatically with the user's current location. This is inline
    with how Google Maps naviagation does it.
