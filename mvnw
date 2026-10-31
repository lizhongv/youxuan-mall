#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper (minimal, only-script style) - bash version
# ----------------------------------------------------------------------------
set -e

MAVEN_VERSION="3.9.9"
WRAPPER_BASE="${HOME}/.m2/wrapper/dists/apache-maven-${MAVEN_VERSION}"
MAVEN_HOME="${WRAPPER_BASE}/apache-maven-${MAVEN_VERSION}"
MAVEN_URL="https://maven.aliyun.com/repository/central/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.zip"

if [ ! -x "${MAVEN_HOME}/bin/mvn" ]; then
    echo "[mvnw] Maven ${MAVEN_VERSION} not found, downloading to ${WRAPPER_BASE} ..."
    mkdir -p "${WRAPPER_BASE}"
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL "${MAVEN_URL}" -o "${WRAPPER_BASE}/maven.zip"
    elif command -v wget >/dev/null 2>&1; then
        wget -q "${MAVEN_URL}" -O "${WRAPPER_BASE}/maven.zip"
    else
        echo "[mvnw] curl or wget required" >&2
        exit 1
    fi
    echo "[mvnw] Extracting..."
    if command -v unzip >/dev/null 2>&1; then
        unzip -q "${WRAPPER_BASE}/maven.zip" -d "${WRAPPER_BASE}"
    else
        echo "[mvnw] unzip required" >&2
        exit 1
    fi
    rm -f "${WRAPPER_BASE}/maven.zip"
    chmod +x "${MAVEN_HOME}/bin/mvn"
fi

exec "${MAVEN_HOME}/bin/mvn" "$@"
