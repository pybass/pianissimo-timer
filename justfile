set default-list := true

# adb refuses to pick a device when a phone is plugged in too, so every emulator command names it.
emulator_serial := "emulator-5554"
apk := "app/build/outputs/apk/debug/app-debug.apk"
namespace := "io.github.pybass.pianissimotimer"
# The recipes work with the debug build, which has its own id (`applicationIdSuffix` in app/build.gradle.kts).
app_id := namespace + ".debug"
avd := "pianissimo-timer"
emulator_image := "system-images;android-37.0;google_apis;arm64-v8a"
avd_config := home_directory() / ".android/avd" / avd + ".avd/config.ini"
# The shell expands $ANDROID_HOME when a recipe runs.
apksigner := "$ANDROID_HOME/build-tools/37.0.0/apksigner"
release_apk := "app/build/pianissimo-timer.apk"
# The release version: `versionName` in app/build.gradle.kts.
version := `sed -n 's/.*versionName = "\(.*\)"/\1/p' app/build.gradle.kts`

# avdmanager has no flags for these settings, so the recipe rewrites them in the AVD's config.ini:
# - hw.keyboard=yes: without it the emulator's Home and Back buttons and the Mac keyboard do nothing.
# - The host GPU, 8 GB of RAM and 8 cores: with the defaults (no GPU, 2 GB, 4 cores) the emulator lags.
# - The screen is 384x832 dp, as on the user's phone; 480 is a standard emulator density.
# Create the `pianissimo-timer` emulator, replacing an existing one with all its data.
emulator-create:
    sdkmanager "{{ emulator_image }}"
    avdmanager create avd --force -n {{ avd }} -k "{{ emulator_image }}" -d medium_phone
    sed -i '' \
        -e 's/^hw.keyboard=.*/hw.keyboard=yes/' \
        -e 's/^hw.gpu.enabled=.*/hw.gpu.enabled=yes/' \
        -e 's/^hw.gpu.mode=.*/hw.gpu.mode=host/' \
        -e 's/^hw.ramSize=.*/hw.ramSize=8G/' \
        -e 's/^hw.cpu.ncore=.*/hw.cpu.ncore=8/' \
        -e 's/^hw.lcd.width=.*/hw.lcd.width=1152/' \
        -e 's/^hw.lcd.height=.*/hw.lcd.height=2496/' \
        -e 's/^hw.lcd.density=.*/hw.lcd.density=480/' \
        "{{ avd_config }}"

# Boot the `pianissimo-timer` emulator; leave it running in its own terminal.
emulator:
    emulator -avd {{ avd }}

# Run before a commit: ktlint style, then Android Lint and a full compile. Any warning fails it.
check:
    ktlint "**/*.kt" "**/*.kts"
    ./gradlew lint assembleDebug

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

# `-d` means "the only USB device": no phone serial in the repo, and adb fails when two phones are plugged in.
# Run adb against the emulator, or against the phone on USB when target is `phone`.
[private]
adb target *args:
    adb {{ if target == "phone" { "-d" } else { "-s " + emulator_serial } }} {{ args }}

# Replaces an installed app; channels, permissions and data stay. Use `app-remove` first to reset them.
# The activity is named in full: its class stays in the namespace, and `/.MainActivity` would look under the debug id.
# Build, install into the emulator, or into the phone with `phone`, and open the app.
[arg("target", pattern="emulator|phone")]
app-run target="emulator": build
    just adb {{ target }} install {{ apk }}
    just adb {{ target }} shell am start -n {{ app_id }}/{{ namespace }}.MainActivity

# Follow the app's log lines tagged `Bell` (emulator, or the phone with `phone`); leave it running in its own terminal.
[arg("target", pattern="emulator|phone")]
log target="emulator":
    just adb {{ target }} logcat -s Bell:I

# A notification channel keeps its first settings forever, so this is how a channel change lands.
# Remove the app with everything it stored: channels, permissions, data (emulator, or the phone with `phone`).
[arg("target", pattern="emulator|phone")]
app-remove target="emulator":
    just adb {{ target }} uninstall {{ app_id }}
