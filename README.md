# WeatherApp Mosby MVI RxJava

[![Build CI](https://github.com/hoc081098/WeatherApp_MVI_sample/actions/workflows/build.yml/badge.svg)](https://github.com/hoc081098/WeatherApp_MVI_sample/actions/workflows/build.yml)
[![spotless](https://github.com/hoc081098/WeatherApp_MVI_sample/actions/workflows/spotless.yml/badge.svg)](https://github.com/hoc081098/WeatherApp_MVI_sample/actions/workflows/spotless.yml)

## Sreenshots
|  |  |   |
| :---:                              | :---:                             | :---:                              |
| ![](screenshots/Screenshot_1.png)  | ![](screenshots/Screenshot_2.png) | ![](screenshots/Screenshot_3.png)  |
| ![](screenshots/Screenshot_4.png)  | ![](screenshots/Screenshot_5.png) | ![](screenshots/Screenshot_6.png)  |
| ![](screenshots/Screenshot_7.png)  | ![](screenshots/Screenshot_8.png) | ![](screenshots/Screenshot_9.png)  |
| ![](screenshots/Screenshot_10.png)  | ![](screenshots/Screenshot_11.png) | ![](screenshots/Screenshot_12.png)  |
| ![](screenshots/Screenshot_13.png)  | ![](screenshots/Screenshot_14.png) | ![](screenshots/Screenshot_16.png)  |
| ![](screenshots/Screenshot_17.png)  | ![](screenshots/Screenshot_18.png) | ![](screenshots/Screenshot_19.png)  |
| ![](screenshots/Screenshot_20.png)  | ![](screenshots/Screenshot_21.png) | ![](screenshots/Screenshot_22.png)  |
| ![](screenshots/Screenshot_23.png)  | ![](screenshots/Screenshot_24.png) | ![](screenshots/Screenshot_25.png)  |

## Features

- Architecture MVI with [Mosby MVI library](https://github.com/sockeqwe/mosby)
    - Data (for models, database, API and preferences).
    - Presentation (for UI logic, with Mosby Mvi Presenter).
- Dependency injection with [Koin](https://insert-koin.io/).
- Full reactive programming with [RxKotlin](https://github.com/ReactiveX/RxKotlin), [RxAndroid](https://github.com/ReactiveX/RxAndroid), [RxBinding](https://github.com/JakeWharton/RxBinding).
- Networking with [Retrofit](https://square.github.io/retrofit/), [Moshi](https://github.com/square/moshi), [RxJava2 Adapter](https://github.com/square/retrofit/tree/master/retrofit-adapters/rxjava2).
- Local data with [Room Persistence](https://developer.android.com/topic/libraries/architecture/room).
- Auto update weather per 15 minutes with [Jetpack WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager).

## Trying:

  [Debug apk](https://github.com/hoc081098/WeatherApp_MVI_sample/blob/try_mvi/app/build/outputs/apk/debug/app-debug.apk)

## Todo
- Add testing
- Use [Kotlin coroutine](https://github.com/Kotlin/kotlinx.coroutines) and suspend function instead of Rx Single, Rx Completable
- Fix some problems
