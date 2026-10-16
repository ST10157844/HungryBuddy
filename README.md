HB(HUngryBuddy)


Kotlin Android recipe discovery app — XML Views edition  
Module: OPSC6312 Open Source Code · PoE Part 2 · Group 5


Overview
HungryBuddy helps users discover meals, search recipes, and follow ingredient lists and cooking instructions. This updated Part 2 implementation uses XML layouts, Activities, ViewBinding and RecyclerView—no Jetpack Compose. It connects to TheMealDB through Retrofit and uses Firebase Authentication for accounts.

Repository: https://github.com/ST10157844/HungryBuddy  
XML migration branch: https://github.com/ST10157844/HungryBuddy/tree/xml-migration


Implemented features
Authentication: register, log in, email/password validation, remembered Firebase session and confirmed logout.
Meal browsing: Home shows TheMealDB meal results in a RecyclerView with food photographs, titles and category/cuisine metadata.
Search: search by meal name; view loading, empty and error states; retain search query/results during normal tab switching.
Recipe details: view a meal photograph, category, cuisine, ingredients with measurements and cooking instructions.
Cooking videos: open the linked YouTube recipe video when available.
Settings: see signed-in email, current English language display and sign out; dark-mode switch is deliberately disabled pending implementation.
Navigation: XML bottom tabs for Home, Search and Settings.



Selected user-defined features (Part 2)
Recipe search and discovery — text search and browseable meal results with visual feedback.
Detailed cooking guides — dynamic ingredients, measurements, instructions and cuisine information.
Cooking video links — optional YouTube video access from recipe details.


Technology stack
Layer		Technology
Language	Kotlin
UI		XML Views, ViewBinding, Activities, RecyclerView
State		ViewModel, StateFlow, Kotlin coroutines
API		TheMealDB, Retrofit, Gson
Images		Glide
Authentication	Firebase Authentication
Testing		JUnit, Android instrumentation tests
Version control	Git, GitHub


Requirements and setup
Install Android Studio with an Android SDK compatible with the project configuration (compileSdk 37, targetSdk 36, minSdk 26) and the Gradle/JDK setup required by the included wrapper.
Clone the project and check out the XML branch:
```bash
   git clone https://github.com/ST10157844/HungryBuddy.git
   cd HungryBuddy
   git checkout xml-migration
   ```
Open the project folder in Android Studio and let Gradle sync.
Configure Firebase for your own Android application with package name `com.example.hungrybuddy`; enable Email/Password in Firebase Authentication and place your own `google-services.json` under `app/`. Do not commit credential or signing secrets. The project already uses the Google Services Gradle plugin.
Start an emulator (tested on Pixel 6 Android 17) or an Android device and Run `app`.
Ensure the emulator/device has internet access for live TheMealDB searches and Firebase Authentication.
> Firebase registration requires access to a correctly configured Firebase project. `google-services.json` is app configuration; do not share secrets or service-account credentials.



Build and test
On Windows, from the project root:
```powershell
.\\gradlew.bat :app:assembleDebug
.\\gradlew.bat :app:testDebugUnitTest
.\\gradlew.bat :app:connectedDebugAndroidTest
```




Reported migration-verification results :

Check	Result

Debug build		Successful
Local unit tests	13 passed, 0 failed, 0 skipped
Instrumentation tests	1 passed on Pixel 6 AVD


Note: Passing tests do not mean 100% code coverage. The original Compose files and dependencies were removed from the checked XML migration, and the XML implementation was tested through manual emulator walkthroughs.


Main app structure
```text
app/src/main/
├── java/com/example/hungrybuddy/
│   ├── data/remote/           # TheMealDB Retrofit API
│   ├── data/repository/       # RecipeRepository
│   ├── model/                 # Recipe model
│   ├── ui/activities/         # Login, Register, Home, Search, Details, Settings
│   ├── ui/adapters/           # RecipeAdapter / RecyclerView
│   ├── ui/navigation/         # XML bottom-navigation helper
│   ├── utils/                 # ValidationUtils
│   └── viewmodel/             # RecipeViewModel
└── res/layout/               # XML screen and recipe item layouts
```


Using HungryBuddy
Register an account or log in.
Browse meals on Home or use the Search tab to search by name.
Tap any recipe to see ingredients, measurements and instructions.
Open its cooking video when a valid YouTube link is available.
Use Settings to view the account email or securely log out.




Known limitations and Part 3 roadmap
This is a Part 2 prototype, not the full feature set from the original project proposal. The following are not verified as completed: user-created/published recipes, favorites, comments, Room offline sync, Google sign-in, notifications and profile editing.





Part 3 priorities:
Language settings: implement real, persistent language selection for English, isiZulu and Setswana across supported UI strings.
Dark mode: implement a genuine DayNight theme, adaptive XML colors and persistent user preference.




Oladayo Williams Oladele · ST10157844 · Group 5  
OPSC6312 — Open Source Code, PoE Part 2
Meal names, photographs and recipe content are provided by TheMealDB. Firebase, Android, Retrofit and Glide are third-party technologies maintained by their respective contributors.