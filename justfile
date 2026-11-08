set default-list := true

# Emulators are named by Android version: the AVD `android-16` runs Android 16, and so on. Each version always
# boots on its own port, so its adb serial is fixed and two emulators can run at once. The private recipes
# `image` and `port` map a version to its system image and port; a new version needs a branch in each and its
# number in every `pattern`.
apk := "app/build/outputs/apk/debug/app-debug.apk"
namespace := "io.github.pybass.pianissimotimer"
# The recipes work with the debug build, which has its own id (`applicationIdSuffix` in app/build.gradle.kts).
app_id := namespace + ".debug"
# The shell expands $ANDROID_HOME when a recipe runs.
apksigner := "$ANDROID_HOME/build-tools/37.0.0/apksigner"
release_apk := "app/build/pianissimo-timer.apk"
# The release version: `versionName` in app/build.gradle.kts.
version := `sed -n 's/.*versionName = "\(.*\)"/\1/p' app/build.gradle.kts`

# The SDK package of Android <version>'s system image. Google names Android 17's directory `android-37.0`.
[private]
image version:
    @echo "system-images;{{ if version == "16" { "android-36" } else { "android-37.0" } }};google_apis;arm64-v8a"

# The emulator port of Android <version>; adb calls the emulator `emulator-<port>`.
[private]
port version:
    @echo {{ if version == "16" { "5554" } else { "5556" } }}

# avdmanager has no flags for these settings, so the recipe rewrites them in the AVD's config.ini:
# - hw.keyboard=yes: without it the emulator's Home and Back buttons and the Mac keyboard do nothing.
# - The host GPU, 4 GB of RAM and 8 cores: with the defaults (no GPU, 2 GB, 4 cores) the emulator lags.
# - The screen is 384x832 dp, as on the user's phone; 480 is a standard emulator density.
# Install Android <version>'s image and create the `android-<version>` emulator, replacing an existing one with all its data.
[arg("version", pattern="16|17")]
emulator-create version:
    sdkmanager "$(just image {{ version }})"
    avdmanager create avd --force -n android-{{ version }} -k "$(just image {{ version }})" -d medium_phone
    sed -i '' \
        -e 's/^hw.keyboard=.*/hw.keyboard=yes/' \
        -e 's/^hw.gpu.enabled=.*/hw.gpu.enabled=yes/' \
        -e 's/^hw.gpu.mode=.*/hw.gpu.mode=host/' \
        -e 's/^hw.ramSize=.*/hw.ramSize=4G/' \
        -e 's/^hw.cpu.ncore=.*/hw.cpu.ncore=8/' \
        -e 's/^hw.lcd.width=.*/hw.lcd.width=1152/' \
        -e 's/^hw.lcd.height=.*/hw.lcd.height=2496/' \
        -e 's/^hw.lcd.density=.*/hw.lcd.density=480/' \
        "{{ home_directory() }}/.android/avd/android-{{ version }}.avd/config.ini"

# Boot the `android-<version>` emulator on its own port; leave it running in its own terminal.
[arg("version", pattern="16|17")]
emulator version:
    emulator -avd android-{{ version }} -port $(just port {{ version }})

# Run before a commit: ktlint style, then Android Lint, the unit tests and a full compile. Any warning fails it.
check:
    ktlint "**/*.kt" "**/*.kts"
    ./gradlew lint testDebugUnitTest assembleDebug

# Run the unit tests; they run on this computer, no emulator needed.
test:
    ./gradlew testDebugUnitTest

# Apply ktlint's autofixes.
fix:
    ktlint --format "**/*.kt" "**/*.kts"

# Build the debug APK into app/build/outputs/apk/debug/.
build:
    ./gradlew assembleDebug

# Gradle builds the APK unsigned and apksigner asks for the password itself. Signing in Gradle would put the
# password into Gradle's configuration cache on disk.
# Build the release APK and sign it into app/build/pianissimo-timer.apk; asks for the key's password.
release-build:
    ./gradlew assembleRelease
    "{{ apksigner }}" sign --ks "{{ home_directory() }}/.android/pianissimo-timer-release.p12" --ks-key-alias release \
        --out {{ release_apk }} app/build/outputs/apk/release/app-release-unsigned.apk
    "{{ apksigner }}" verify --print-certs {{ release_apk }}

# Verify the repo is release-ready: clean tree, HEAD at origin/main tip, version not yet tagged.
[private]
release-preflight:
    #!/usr/bin/env bash
    set -euo pipefail
    if [[ -n "$(git status --porcelain)" ]]; then
        echo "error: working tree has uncommitted changes; commit them before releasing." >&2
        exit 1
    fi
    git fetch -q origin main
    # Exact tip, not just an ancestor: catches unpushed, stale, and feature-branch HEADs alike.
    if [[ "$(git rev-parse HEAD)" != "$(git rev-parse refs/remotes/origin/main)" ]]; then
        echo "error: HEAD is not at the tip of origin/main; switch to main and pull/push first." >&2
        exit 1
    fi
    # Origin is checked too: a remote-only tag would otherwise fail the push after the APK is signed.
    if git rev-parse -q --verify "refs/tags/v{{ version }}" >/dev/null || git ls-remote --exit-code --tags origin "refs/tags/v{{ version }}" >/dev/null 2>&1; then
        echo "error: tag v{{ version }} already exists; raise versionName and versionCode in app/build.gradle.kts." >&2
        exit 1
    fi

# The release is a draft: nobody sees it until Publish is pressed on GitHub, where its description is written.
# Run the preflight and check, build and sign the APK, tag v<version>, push the tag, and open a draft GitHub Release.
release: release-preflight check release-build
    git tag -a "v{{ version }}" -m "v{{ version }}"
    git push origin "v{{ version }}"
    gh release create "v{{ version }}" {{ release_apk }} --draft --verify-tag --title "v{{ version }}" --notes ""

# Zed's Kotlin server caches R.jar and misses new resources even after a restart; deleting the cache fixes it.
# Delete the Kotlin server's cache for all projects; close Zed first.
lsp-reset:
    rm -rf "{{ home_directory() }}/Library/Caches/JetBrains/IntelliJServer/workspaces"

# A target is an Android version (its emulator) or `phone`, the phone on USB.
# `-d` means "the only USB device": no phone serial in the repo, and adb fails when two phones are plugged in.
# adb refuses to pick a device by itself when several are connected, so every emulator command names its serial.
[private]
adb target *args:
    adb {{ if target == "phone" { "-d" } else { "-s emulator-$(just port " + target + ")" } }} {{ args }}

# Replaces an installed app; channels, permissions and data stay. Use `app-remove` first to reset them.
# The activity is named in full: its class stays in the namespace, and `/.MainActivity` would look under the debug id.
# Build, install into the target (an Android version's emulator, or `phone`) and open the app.
[arg("target", pattern="16|17|phone")]
app-run target="16": build
    just adb {{ target }} install {{ apk }}
    just adb {{ target }} shell am start -n {{ app_id }}/{{ namespace }}.MainActivity

# Follow the app's log lines tagged `Bell` on the target; leave it running in its own terminal.
[arg("target", pattern="16|17|phone")]
log target="16":
    just adb {{ target }} logcat -s Bell:I

# A notification channel keeps its first settings forever, so this is how a channel change lands.
# Remove the app from the target with everything it stored: channels, permissions, data.
[arg("target", pattern="16|17|phone")]
app-remove target="16":
    just adb {{ target }} uninstall {{ app_id }}
