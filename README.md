# ESMANAGER

An alternative for ESLauncher2, aimed at developers and advanced users.

## Currently supported features:

- Download the latest continuous
- Download any release, or the latest release
- Build any branch, PR or release from source
  - Supports forked repositories
- Pull from multiple sources and merge them automatically
  - Look out for merge conflicts
- Automatic updates for all instances, plugins and the launcher
- **Audio controls**
- Plugin management
- Full user theming and localization
  - You can also add your own custom launcher music
  - Comes with a light and dark theme
- A lot of settings and customization

## ESL2 features missing from ESManager

- Proper macOS support
  - I can't test on Mac, so I cannot ensure anything will work. Most things should be ok. I'll try my best to fix any bugs that come up.
- The convenient instance creation panel on the main page
  - There are simply too many options for me to fit them all in there. A simplified version might be added later. You get an instance creation wizard instead.
- Downloading build artifacts from pull requests
  - MCO has his own server building PRs remotely, and I don't want to use that (you can still use it via direct downloads, though). Instead, this application builds pull requests from source.

## Requirements

- ### Hardware requirements
  - I'd recommend having at least 512 MB of RAM available. This is a half-bullshit number based on how much it uses on my machine, but due to dynamic caching it's probably fine if you have less. I wouldn't recommend limiting it, though.
  - Since many updates are ran in parallel, it is recommended to have an at least quad-core CPU, especially if you use many plugins or instances. This is not necessary for the app to run, but it can easily use upwards of 20 threads after startup, so having more cpu cannot hurt.
  - Hardware acceleration is supported by JavaFX via Direct3D and GLES, so make sure your GPU supports some of these. Others might also be supported, it's difficult to find an up-to-date list. If you see outlandish CPU usage *even with the media player paused*, the application is likely using software-based rendering with much lower performance. (The audio player is known to have high cpu usage for software decoding.)
  
- ### Software requirements
  - For most users, nothing. The application comes bundled with a pure Java-based Git implementation, so even Git doesn't have to be installed.
  - If you want to build the game from source (using a pull request as source, for example), you need to have Git and all development libraries installed as [described in the game repository](https://github.com/endless-sky/endless-sky/blob/master/readme-developer.md). ESManager can build the game from source using CMake (default) and SCons, so follow the instructions for whichever one you configured in the settings.

## Installation
- Download the latest release from GitHub.

## Tutorials and settings

### Custom theming
  You can customize the theme of the launcher using css. It comes with two built-in themes: dark and light. You can choose between these in `Settings -> Launcher -> Theme`.
  
  You can also add your own themes. For this, navigate to the [data directory](#data-directory), and create a new subdirectory called `themes`. Any css file in this directory will be available as a launcher theme in the settings menu. You might have to restart the application for the new theme to be detected, but applying themes is instantaneous.
  
  *Please note that the css files don't have the same entries that you might be used to from HTML. Follow the [JavaFX CSS Guide](https://openjfx.io/javadoc/19/javafx.graphics/javafx/scene/doc-files/cssref.html) or have a look at the [default themes](src/main/resources/tibetiroka/esmanager/ui/themes) for more details.*
  
  Feel free to look at the [fxml files](src/main/resources/tibetiroka/esmanager/ui) to see which style classes apply to certain components. 
  
### Localization
  Pretty much all text you see in the launcher is localized. The application ships with a single English locale, but you can define your own locales as well. You can choose between locales in `Settings -> Launcher -> Language`.
  
  To add a new language, navigate to the [data directory](#data-directory), and create a new subdirectory called `locales`. You can put your own locale files in this directory. Locale files are always named `ui_language.properties`, like `ui_hu.properties` or `ui_en_US.properties`. These locales will become available in the launcher after it is restarted.
  
  Locales are applied instantly to all components except log messages. Old log messages will continue to use their own locale, but new log messages are created with the new locale.
  
  Please have a look at the [default locale file](src/main/resources/tibetiroka/esmanager/config/locales/ui_en.properties) to see what localization entries are used, and how messages are formatted. Please note that internally [MessageFormat](https://docs.oracle.com/en/java/javase/19/docs/api/java.base/java/text/MessageFormat.html) is used to format these messages, which uses `'` as an escape character.
  
  You don't have to specify every entry in a locale, missing entries will be resolved from the default locale.

### Important options
- `Settings -> Game -> Preserve manually installed plugins`
  You can specify whether plugins downloaded from the launcher should override existing plugins. Please note that once a plugin is overridden, it is unrecoverable. This setting defaults to preserve existing plugins.
- `Settings -> Update`
  If you want to play with the game as soon as the launcher opens, disable all updates. You can still update the instances and plugins manually.
  Unless something actually needs updating, having automatic updates turned on shouldn't cause more than a few seconds of delay. Some upgrades can be performed even while in-game.

## Location of ESManager files

### Data directory
  The data directory contains all instances and plugins, and also all user-defined themes and audio files.
- Linux: `$XDG_DATA_HOME/ESManager`, or its appropriate fallback
- Windows: `$user.home/AppData/Local/ESManager/data`
- macOS: `$user.home/Library/Application Support/ESManager/data`

### Config directory
  This is where configuration files are located for ESManager. Please don't edit these files unless you know what you are doing.
- Linux: `$XDG_CONFIG_HOME/ESManager/`, or its appropriate fallback
- Windows: `$user.home/AppData/Local/ESManager/config`
- macOS: `$user.home/Library/Application Support/ESManager/config`

### Log files
  Log files are stored in a single directory. Old log files are compressed and kept indefinitely, while the latest log is a plain text file called `latest.log`.
- Linux: `$XDG_STATE_HOME/ESManager/logs`, or its appropriate fallback
- Windows: `$user.home/AppData/Local/ESManager/logs`
- macOS: `$user.home/Library/Application Support/ESManager/logs`

## Licensing
- Any code I wrote is licensed under GNU General Public License v3.0, the same license that is used in Endless Sky (GPL-3.0-or-later).
- Some auto-generated project files are licensed under the Apache License.
- Dependencies and the bundled Java Development Kit use various open-source licenses.
- The launcher music is taken from [sounds-of-endless-sky](https://github.com/samrocketman/sounds-of-endless-sky), licensed under CC-BY-4.0. I modified the files to contain ID3v2 metadata with the artists' name and the songs' title, and these are displayed on the main screen.
- The plugin banner images are not stored in this application, and have unknown licenses (probably the same as the plugins themselves). Plugins might use a variety of licenses.
- For the license used for any specific file, refer to the copyright header of the file in question.
