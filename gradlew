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

# 1. Decode debug.keystore from debug.keystore.base64 if missing
if [ ! -f "$APP_HOME/debug.keystore" ] && [ -f "$APP_HOME/debug.keystore.base64" ]; then
    tr -d '\r\n ' < "$APP_HOME/debug.keystore.base64" | base64 -d > "$APP_HOME/debug.keystore" 2>/dev/null || \
    base64 --decode "$APP_HOME/debug.keystore.base64" > "$APP_HOME/debug.keystore" 2>/dev/null || true
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
