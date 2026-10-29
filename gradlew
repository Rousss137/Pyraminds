#!/bin/sh

#
# Copyright © 2015 the original authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
#

app_path=$0
while
    APP_HOME=${app_path%"${app_path##*/}"}
    [ -h "$app_path" ]
do
    ls=$( ls -ld "$app_path" )
    link=${ls#*' -> '}
    case $link in
      /*)   app_path=$link ;;
      *)    app_path=$APP_HOME$link ;;
    esac
done

APP_BASE_NAME=${0##*/}
APP_HOME=$( cd -P "${APP_HOME:-./}" > /dev/null && printf '%s\n' "$PWD" ) || exit

# 1. Ensure debug.keystore exists (needed in GitHub Actions where keystore files are not exported)
if [ ! -f "$APP_HOME/debug.keystore" ]; then
    if [ -f "$APP_HOME/debug.keystore.base64" ]; then
        tr -d '\r\n ' < "$APP_HOME/debug.keystore.base64" | base64 -d > "$APP_HOME/debug.keystore" 2>/dev/null || \
        base64 --decode "$APP_HOME/debug.keystore.base64" > "$APP_HOME/debug.keystore" 2>/dev/null || true
    else
        DEBUG_KEYSTORE_B64="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"
        printf '%s' "$DEBUG_KEYSTORE_B64" | base64 -d > "$APP_HOME/debug.keystore" 2>/dev/null || \
        printf '%s' "$DEBUG_KEYSTORE_B64" | base64 --decode > "$APP_HOME/debug.keystore" 2>/dev/null || true
    fi
    if [ ! -s "$APP_HOME/debug.keystore" ] && command -v keytool >/dev/null 2>&1; then
        keytool -genkeypair -v -keystore "$APP_HOME/debug.keystore" -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1 || true
    fi
fi

# 2. In GitHub Actions, remove legacy setup-gradle@v3 init scripts that are incompatible with Gradle 9 (Groovy 4)
if [ "$GITHUB_ACTIONS" = "true" ]; then
    rm -f "${GRADLE_USER_HOME:-$HOME/.gradle}"/init.d/*gradle-actions* 2>/dev/null || true
    rm -f "$HOME"/.gradle/init.d/*gradle-actions* 2>/dev/null || true
    if [ -n "$RUNNER_TEMP" ]; then
        rm -rf "$RUNNER_TEMP/.gradle-actions" 2>/dev/null || true
    fi
    if [ -n "$JAVA_HOME_21_X64" ] && [ -x "$JAVA_HOME_21_X64/bin/java" ]; then
        JAVA_HOME="$JAVA_HOME_21_X64"
        export JAVA_HOME
    fi
fi

# 3. Ensure Android SDK licenses are accepted in CI so AGP can auto-install compileSdk / build-tools
for sdk_dir in "$ANDROID_HOME" "$ANDROID_SDK_ROOT" "/usr/local/lib/android/sdk"; do
    if [ -n "$sdk_dir" ] && [ -d "$sdk_dir" ]; then
        mkdir -p "$sdk_dir/licenses" 2>/dev/null || true
        printf "\n8933bad161af4178b1185d1a37fbf41ea5269c55\nd56f5187479451eabf01fb78af6dfcb131a6481e\n24333f8a63b6825ea9c5514f83c2829b004d1fee\n" > "$sdk_dir/licenses/android-sdk-license" 2>/dev/null || true
        printf "\n84831b9409646a918e30573bab4c9c91346d8abd\n504667f4c0de7af1a06de9f4b1727b84351f2910\n" > "$sdk_dir/licenses/android-sdk-preview-license" 2>/dev/null || true
    fi
done

# 4. Bootstrap Gradle 9.3.1 directly without requiring a binary gradle-wrapper.jar in git
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -s "$WRAPPER_JAR" ]; then
    if [ -x "/opt/gradle/gradle-9.3.1/bin/gradle" ]; then
        exec "/opt/gradle/gradle-9.3.1/bin/gradle" "$@"
    fi
    GRADLE_VERSION="9.3.1"
    GRADLE_DIST_DIR="$HOME/.gradle-dist/gradle-${GRADLE_VERSION}-bin"
    GRADLE_BIN="$GRADLE_DIST_DIR/gradle-${GRADLE_VERSION}/bin/gradle"
    if [ ! -x "$GRADLE_BIN" ]; then
        mkdir -p "$GRADLE_DIST_DIR"
        ZIP_FILE="$GRADLE_DIST_DIR/gradle-${GRADLE_VERSION}-bin.zip"
        DIST_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
        echo "Downloading Gradle ${GRADLE_VERSION} from ${DIST_URL}..."
        if command -v curl >/dev/null 2>&1; then
            curl -fsSL "$DIST_URL" -o "$ZIP_FILE"
        elif command -v wget >/dev/null 2>&1; then
            wget -qO "$ZIP_FILE" "$DIST_URL"
        else
            echo "ERROR: Neither curl nor wget is available to download Gradle ${GRADLE_VERSION}." >&2
            exit 1
        fi
        unzip -q -o "$ZIP_FILE" -d "$GRADLE_DIST_DIR"
        rm -f "$ZIP_FILE"
        chmod +x "$GRADLE_BIN"
    fi
    exec "$GRADLE_BIN" "$@"
fi

exec java -Xmx64m -Xms64m "-Dorg.gradle.appname=$APP_BASE_NAME" -jar "$WRAPPER_JAR" "$@"
