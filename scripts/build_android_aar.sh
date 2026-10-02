#!/bin/bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
NATIVE_MODULE="${PROJECT_ROOT}/android/native"
GRADLEW="${PROJECT_ROOT}/example/android/gradlew"
OUTPUT_DIR="${NATIVE_MODULE}/build/outputs/aar"
TARGET_LIBS_DIR="${PROJECT_ROOT}/android/libs"
TARGET_AAR="${TARGET_LIBS_DIR}/cielo_payment_tech-release.aar"
TARGET_JAR="${TARGET_LIBS_DIR}/cielo_payment_tech-native.jar"

# Detectar FLUTTER_SDK automaticamente, se necessário
if [[ -z "${FLUTTER_SDK:-}" ]]; then
  if command -v flutter >/dev/null 2>&1; then
    FLUTTER_BIN="$(command -v flutter)"
    FLUTTER_SDK="$(cd "$(dirname "${FLUTTER_BIN}")/.." && pwd)"
    export FLUTTER_SDK
    echo "[INFO] FLUTTER_SDK detectado automaticamente em ${FLUTTER_SDK}."
  else
    echo "[ERRO] Não foi possível localizar o Flutter no PATH nem a variável FLUTTER_SDK. Configure um dos dois." >&2
    exit 1
  fi
else
  export FLUTTER_SDK
  echo "[INFO] FLUTTER_SDK definido manualmente em ${FLUTTER_SDK}."
fi

# Detectar JAVA_HOME automaticamente, se necessário
if [[ -z "${JAVA_HOME:-}" || ! -d "${JAVA_HOME}" ]]; then
  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    if JAVA_DETECTED=$(/usr/libexec/java_home -v 17 2>/dev/null); then
      JAVA_HOME="${JAVA_DETECTED}"
    elif JAVA_DETECTED=$(/usr/libexec/java_home 2>/dev/null); then
      JAVA_HOME="${JAVA_DETECTED}"
    else
      echo "[ERRO] Não foi possível detectar automaticamente o JAVA_HOME. Instale o OpenJDK (ex: brew install openjdk@17)." >&2
      exit 1
    fi
    export JAVA_HOME
    echo "[INFO] JAVA_HOME detectado automaticamente em ${JAVA_HOME}."
  else
    echo "[ERRO] JAVA_HOME não está configurado e /usr/libexec/java_home não está disponível." >&2
    exit 1
  fi
else
  export JAVA_HOME
  echo "[INFO] JAVA_HOME definido manualmente em ${JAVA_HOME}."
fi

if [[ ! -x "${GRADLEW}" ]]; then
  echo "[ERRO] Wrapper Gradle não encontrado em ${GRADLEW}. Certifique-se de que o projeto de exemplo foi gerado corretamente." >&2
  exit 1
fi

LOCAL_PROPERTIES="${NATIVE_MODULE}/local.properties"
if [[ ! -f "${LOCAL_PROPERTIES}" ]]; then
  if [[ -f "${PROJECT_ROOT}/android/local.properties" ]]; then
    cp "${PROJECT_ROOT}/android/local.properties" "${LOCAL_PROPERTIES}"
    echo "[INFO] local.properties copiado do módulo android raiz."
  elif [[ -n "${ANDROID_SDK_ROOT:-}" ]]; then
    printf "sdk.dir=%s\n" "${ANDROID_SDK_ROOT}" > "${LOCAL_PROPERTIES}"
    echo "[INFO] local.properties gerado a partir de ANDROID_SDK_ROOT."
  elif [[ -n "${ANDROID_HOME:-}" ]]; then
    printf "sdk.dir=%s\n" "${ANDROID_HOME}" > "${LOCAL_PROPERTIES}"
    echo "[INFO] local.properties gerado a partir de ANDROID_HOME."
  else
    echo "[ERRO] Não foi possível determinar o sdk.dir. Configure ANDROID_SDK_ROOT ou forneça android/local.properties." >&2
    exit 1
  fi
fi

FLUTTER_BIN_PATH="${FLUTTER_SDK}/bin/flutter"
if [[ -x "${FLUTTER_BIN_PATH}" ]]; then
  echo "[INFO] Garantindo artefatos Android do Flutter (precache)..."
  "${FLUTTER_BIN_PATH}" --disable-analytics precache --android >/dev/null 2>&1 || true
fi

echo "[INFO] Limpando e compilando o módulo nativo..."
FLUTTER_SDK="${FLUTTER_SDK}" JAVA_HOME="${JAVA_HOME}" "${GRADLEW}" -p "${NATIVE_MODULE}" --no-daemon clean assembleRelease

if [[ ! -d "${OUTPUT_DIR}" ]]; then
  echo "[ERRO] Diretório de saída ${OUTPUT_DIR} não encontrado." >&2
  exit 1
fi

AAR_FILE=$(find "${OUTPUT_DIR}" -maxdepth 1 -name '*-release.aar' -print -quit)
if [[ -z "${AAR_FILE}" || ! -f "${AAR_FILE}" ]]; then
  echo "[ERRO] AAR release não encontrado em ${OUTPUT_DIR}. Verifique os logs do Gradle." >&2
  exit 1
fi

mkdir -p "${TARGET_LIBS_DIR}"
cp "${AAR_FILE}" "${TARGET_AAR}"

# Extrai classes.jar e salva como artefato nativo principal
tmp_dir=$(mktemp -d)
unzip -p "${AAR_FILE}" classes.jar > "${TARGET_JAR}"
rm -rf "${tmp_dir}"

echo "[INFO] AAR copiado para ${TARGET_AAR}."
echo "[INFO] Jar nativo copiado para ${TARGET_JAR}."
echo "[INFO] Finalizado. O plugin Android agora depende do artefato pré-compilado."

