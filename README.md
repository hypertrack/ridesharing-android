# Uber-for-X driver & customer apps using HyperTrack SDK

🛑**WARNING: THIS OPEN SOURCE REPO ISN'T A WORKING REPO. WE ARE ACTIVELY WORKING TO UPDATE THIS REPO. WE EXPECTED TO GET IT BACK IN A WORKING STATE BY NOV 2018.**🛑

Uber’s business model has given rise to a large number of Uber-for-X services. Among other things, X equals moving, parking, courier, groceries, flowers, alcohol, dog walks, massages, dry cleaning, vets, medicines, car washes, roadside assistance and marijuana. Through these on-demand platforms, supply and demand are aggregated online for services to be fulfilled offline.

This open source repo/s uses HyperTrack SDK for developing real world Uber-like consumer & driver apps.

 - **Uber-for-X Consumer app** can be used by customer to :
      - Login/signup customer using Firebase phone-number authentication
      - Show available cars near customer's current location
      - Allow customer to select pickup and dropoff location
      - Show estimated fare and route for selected pickup and dropoff location
      - Book a ride from desired pickup and dropoff location
      - Track driver to customer's pickup location
      - Track the ongoing ride to dropoff location
      - Let customers share live trip with friends and family
      - Show trip summary with distance travelled

<p align="center">
 <a href="https://www.youtube.com/watch?v=1qMFP5w32GY">
  <img src="http://res.cloudinary.com/hypertrack/image/upload/v1525329669/customer.png" width="300"/>
 </a>
</p>


- **Uber-for-X Driver app** can be used by driver to :
     - Login/signup driver using Firebase phone-number authentication
     - Find new rides
     - Accept a ride
     - Track and navigate till customer's pickup location, and mark the pickup as complete
     - Track and navigate from customer's pickup to dropoff location, and mark the dropoff as complete
     - Show trip summary with distance travelled

<p align="center">
 <a href="https://www.youtube.com/watch?v=3R9GDQitt40">
  <img src="http://res.cloudinary.com/hypertrack/image/upload/v1525329669/driver.png" width="300"/>
 </a>
</p>


## How to Begin

### 1. Get your keys
 - [Signup](https://dashboard.hypertrack.com/signup?utm_source=github&utm_campaign=uber_for_x_android) to get your [HyperTrack API keys](https://dashboard.hypertrack.com/settings)
 - Get the [Google Maps API key](https://developers.google.com/maps/documentation/android-api/signup)

### 2. Set up consumer & driver app
 - [Clone](https://github.com/hypertrack/uberx_android.git) the consumer & driver apps
 - Add the publishable key in DriverApp.java in Driver project and in ConsumerApp.java in Consumer project
 - Add your Google Maps key to google-maps-api.xml in each of your individual repos. Make sure you replace API keys in all environments (debug/release)

### 3. Set up FCM
The HyperTrack SDK requires FCM for a battery efficient real-time tracking experience
 - Setup your account on [Firebase console](https://console.firebase.google.com/) and get your FCM keys
 - After setting up your account on the [Firebase console](https://console.firebase.google.com), add the [google-services.json](https://support.google.com/firebase/answer/7015592) file to your app folder
 - Add your FCM keys to [HyperTrack dashboard](https://dashboard.hypertrack.com/settings)

 - Enable [Phone Number sign-in](https://firebase.google.com/docs/auth/android/phone-auth) for your Firebase project

 *Note:*
  - Both Consumer & Driver apps are using [Firebase's default UI for login](ttps://firebase.google.com/docs/auth/android/firebaseui)
  - Firebase login callbacks are used to trigger the upcoming flow.
  - In case you have custom login mechanism please make necessary changes and trigger the next flow.

### 4. Set up Firebase Realtime Database
 - Setup Firebase Realtime Database via Android studio. Go to: Android Studio --> Tools --> Firebase --> Realtime Database
 - Note that Firebase Realtime Database is _not required_ to use HyperTrack SDK. You may have your own server that is connected to your apps

### 5. Mock Tracking
- To overview the tracking experience provided by HyperTrack SDK, feel free to use "createMockAction" instead of "createAction" in Driver app (StartRideActivity.java).

- No changes are required to be made in Consumer app with respect to createMockAction/ mock tracking.

- Enabling/ disabling of mock tracking is handled from where actions are created.

- In these samples apps, Driver app creates action, which are tracked by Driver & Consumer apps.

## Release to PlayStore
To release the apps on the Play Store, you will have to change the apps' package names.

1. Change the package name in AndroidManifest.xml file.

2. Refactor the name of your package with right click → Refactor → Rename in the tree view, then Android Studio will display a window, select "Rename package" option.

3. Change the application id in the build.gradle file. Once done, clean and rebuild the project.
   - Add `release key store file` in app level folder.
   - Create a `keystore.properties` file in root or project level folder with key-values pair.
    ```properties
        storeFile=<File path of keystore file>
        storePassword=<Key Store Password>
        keyAlias=<Key Alias>
        keyPassword=<Key Password>
   ```
4. Check for pojos/ models mentioned in proguard-rules.pro

## Documentation
For detailed documentation of the APIs, customizations and what all you can build using HyperTrack, please visit the official [docs](https://www.hypertrack.com/docs).

## Contribute
Feel free to clone, use, and contribute back via [pull requests](https://help.github.com/articles/about-pull-requests/). We'd love to see your pull requests - send them in! Please use the [issues tracker](https://github.com/hypertrack/uberx-android/issues) to raise bug reports and feature requests.

We are excited to see what live location feature you build in your app using this project. Do ping us at help@hypertrack.com once you build one, and we would love to feature your app on our blog!

## Support
Join our [Slack community](http://slack.hypertrack.com) for instant responses, or interact with our growing [community](https://community.hypertrack.com). You can also email us at help@hypertrack.com.
