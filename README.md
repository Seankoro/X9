# X9

> **A community driven traffic reporting Android application**
> Developed for ITU's Mobile App Development Coursework project

---

## Overview

X9 is an Android application which allows users to report traffic conditions in real-time to the community.
The application is developed fully in Kotlin and integrates with Firebase services such as:

- Firebase Auth
- Firebase Realtime Database
- Firebase Storage

### Key Features

1. **User account management**: Users will be able to create and manage their own X9 user accounts with authentication provided by `Firebase Authentication`
2. **Traffic report viewing**: All users of X9, regardless of authentication state, will be able to view all traffic reports that are published by the community. Allowing users to be aware of local traffic conditions.
3. **Traffic report management**: Authenticated and authorized users of X9 will be able to create, update and delete traffic reports for other community users. The traffic reports is enriched with location and image data.

### Extra features (in-development)

1. **Geoproximity enabled notifications**: Similar to existing traffic navigation apps (Google Maps, Waze etc.), when users are within the proximity of an existing traffic report, they will recieve notifications on their device about said reports They will then be prompted to confirm if the report is still relevant and the status will be updated to the rest of the X9 community in real-time.
2. **Speech-enabled report creation**: With a tap of a single button, the user will be able to create a report through speech.
> _Note: Due to resource constraints, this feature will not have NLP or AI capabilities. A simple keyword-based routing in the service layer of the application will determine the applications behavior based on some keywords that the users mentioned in their speech.
> Reports created using this feature will not have any description or image due to this constraint._

## Support

**Status**: In-development
**Developers**: Mobile App Development, B.Sc. (Spring 2026): Group 19 - Loo Zhi Yi, Sean Elisha Koh Tze Li
**Last updated**: April 2026