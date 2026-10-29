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

# Resolve links: $0 may be a link
app_path=$0

# Need this for daisy-chained symlinks.
while
    APP_HOME=${app_path%"${app_path##*/}"}  # leaves a trailing /; empty if no leading path
    [ -h "$app_path" ]
do
    ls=$( ls -ld "$app_path" )
    link=${ls#*' -> '}
    case $link in             #(
      /*)   app_path=$link ;; #(
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

# 2. Ensure .env exists in CI environments if .env.example is present
if [ "$GITHUB_ACTIONS" = "true" ] && [ ! -f "$APP_HOME/.env" ] && [ -f "$APP_HOME/.env.example" ]; then
    cp "$APP_HOME/.env.example" "$APP_HOME/.env"
fi

# 3. Ensure Android SDK licenses are accepted in CI so AGP can auto-install compileSdk / build-tools
for sdk_dir in "$ANDROID_HOME" "$ANDROID_SDK_ROOT"; do
    if [ -n "$sdk_dir" ] && [ -d "$sdk_dir" ]; then
        mkdir -p "$sdk_dir/licenses" 2>/dev/null || true
        if [ ! -f "$sdk_dir/licenses/android-sdk-license" ]; then
            printf "\n8933bad161af4178b1185d1a37fbf41ea5269c55\nd56f5187479451eabf01fb78af6dfcb131a6481e\n24333f8a63b6825ea9c5514f83c2829b004d1fee\n" > "$sdk_dir/licenses/android-sdk-license" 2>/dev/null || true
        elif ! grep -q "24333f8a63b6825ea9c5514f83c2829b004d1fee" "$sdk_dir/licenses/android-sdk-license" 2>/dev/null; then
            printf "\n24333f8a63b6825ea9c5514f83c2829b004d1fee\n" >> "$sdk_dir/licenses/android-sdk-license" 2>/dev/null || true
        fi
        if [ ! -f "$sdk_dir/licenses/android-sdk-preview-license" ]; then
            printf "\n84831b9409646a918e30573bab4c9c91346d8abd\n" > "$sdk_dir/licenses/android-sdk-preview-license" 2>/dev/null || true
        fi
    fi
done

# 4. If gradle-wrapper.jar is absent (binary files are not exported as text to GitHub),
# bootstrap Gradle 9.3.1 directly without requiring a binary jar in git.
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -s "$WRAPPER_JAR" ]; then
    if [ -x "/opt/gradle/gradle-9.3.1/bin/gradle" ]; then
        exec "/opt/gradle/gradle-9.3.1/bin/gradle" "$@"
    fi
    GRADLE_VERSION="9.3.1"
    GRADLE_CACHE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-${GRADLE_VERSION}-bin"
    GRADLE_BIN="$GRADLE_CACHE_DIR/gradle-${GRADLE_VERSION}/bin/gradle"
    if [ ! -x "$GRADLE_BIN" ]; then
        mkdir -p "$GRADLE_CACHE_DIR"
        ZIP_FILE="$GRADLE_CACHE_DIR/gradle-${GRADLE_VERSION}-bin.zip"
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
        unzip -q -o "$ZIP_FILE" -d "$GRADLE_CACHE_DIR"
        rm -f "$ZIP_FILE"
        chmod +x "$GRADLE_BIN"
    fi
    exec "$GRADLE_BIN" "$@"
fi

# Standard Gradle Wrapper execution when gradle-wrapper.jar is present
MAX_FD=maximum

warn () {
    echo "$*"
} >&2

die () {
    echo
    echo "$*"
    echo
    exit 1
} >&2

cygwin=false
msys=false
darwin=false
nonstop=false
case "$( uname )" in                #(
  CYGWIN* )         cygwin=true  ;; #(
  Darwin* )         darwin=true  ;; #(
  MSYS* | MINGW* )  msys=true    ;; #(
  NONSTOP* )        nonstop=true ;;
esac

if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD=$JAVA_HOME/jre/sh/java
    else
        JAVACMD=$JAVA_HOME/bin/java
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
    fi
else
    JAVACMD=java
    if ! command -v java >/dev/null 2>&1
    then
        die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
    fi
fi

if ! "$cygwin" && ! "$darwin" && ! "$nonstop" ; then
    case $MAX_FD in #(
      max*)
        MAX_FD=$( ulimit -H -n ) ||
            warn "Could not query maximum file descriptor limit"
    esac
    case $MAX_FD in  #(
      '' | soft) :;; #(
      *)
        ulimit -n "$MAX_FD" ||
            warn "Could not set maximum file descriptor limit to $MAX_FD"
    esac
fi

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

set -- \
        "-Dorg.gradle.appname=$APP_BASE_NAME" \
        -jar "$WRAPPER_JAR" \
        "$@"

eval "set -- $(
        printf '%s\n' "$DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS" |
        xargs -n1 |
        sed ' s~[^-[:alnum:]+,./:=@_]~\\&~g; ' |
        tr '\n' ' '
    )" '"$@"'

exec "$JAVACMD" "$@"
