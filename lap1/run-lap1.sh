#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk}"
JAVA_BIN="$JAVA_HOME/bin/java"
JAVAC_BIN="$JAVA_HOME/bin/javac"
JAVAFX_LIB="/usr/share/java/java-openjfx"
JAVAFX_NATIVE="/usr/lib/java-openjfx"
JDBC_VERSION="9.5.0"
JDBC_JAR="$ROOT_DIR/lib/mysql-connector-j-$JDBC_VERSION.jar"

usage() {
    echo "Cach dung: $0 {server|client|both|demo}"
    echo "  server  Chi mo giao dien server"
    echo "  client  Chi mo mot giao dien client"
    echo "  both    Mo ca server va mot client"
    echo "  demo    Mo server va 6 client da dien san ten/mat khau"
}

MODE="${1:-both}"
if [[ "$MODE" != "server" && "$MODE" != "client" && "$MODE" != "both" && "$MODE" != "demo" ]]; then
    usage
    exit 2
fi

if [[ ! -x "$JAVA_BIN" || ! -x "$JAVAC_BIN" ]]; then
    echo "Khong tim thay JDK tai $JAVA_HOME" >&2
    exit 1
fi

if [[ ! -r "$JAVAFX_LIB/javafx.controls.jar" ]]; then
    echo "Khong tim thay JavaFX tai $JAVAFX_LIB" >&2
    exit 1
fi

if ! systemctl is-active --quiet mariadb; then
    echo "Canh bao: MariaDB chua chay." >&2
    echo "Hay chay 'sudo systemctl start mariadb' truoc khi khoi dong server." >&2
fi

mkdir -p "$ROOT_DIR/lib" "$ROOT_DIR/out"
find "$ROOT_DIR/out" -type f -name '*.class' -delete
if [[ ! -s "$JDBC_JAR" ]]; then
    echo "Dang tai MySQL Connector/J $JDBC_VERSION..."
    curl --fail --location --show-error \
        --output "$JDBC_JAR" \
        "https://repo.maven.apache.org/maven2/com/mysql/mysql-connector-j/$JDBC_VERSION/mysql-connector-j-$JDBC_VERSION.jar"
fi

mapfile -d '' SOURCE_FILES < <(find "$ROOT_DIR/src" -type f -name '*.java' -print0)
if (( ${#SOURCE_FILES[@]} == 0 )); then
    echo "Khong tim thay source Java trong $ROOT_DIR/src" >&2
    exit 1
fi

echo "Dang bien dich..."
"$JAVAC_BIN" \
    --release 17 \
    -encoding UTF-8 \
    --module-path "$JAVAFX_LIB" \
    --add-modules javafx.controls \
    -cp "$JDBC_JAR" \
    -d "$ROOT_DIR/out" \
    "${SOURCE_FILES[@]}"

JAVA_OPTIONS=(
    --enable-native-access=javafx.graphics
    "-Djava.library.path=$JAVAFX_NATIVE"
    --module-path "$JAVAFX_LIB"
    --add-modules javafx.controls
    -cp "$ROOT_DIR/out:$JDBC_JAR"
)

start_server() {
    echo "Dang mo giao dien server..."
    (cd "$ROOT_DIR" && "$JAVA_BIN" "${JAVA_OPTIONS[@]}" lab1.server.ServerFxApp)
}

start_client() {
    echo "Dang mo giao dien client..."
    (cd "$ROOT_DIR" && "$JAVA_BIN" "${JAVA_OPTIONS[@]}" lab1.client.ChatClient "$@")
}

case "$MODE" in
    server)
        start_server
        ;;
    client)
        start_client
        ;;
    both)
        start_server &
        SERVER_PID=$!
        start_client &
        CLIENT_PID=$!
        trap 'kill "$SERVER_PID" "$CLIENT_PID" 2>/dev/null || true' INT TERM EXIT
        wait "$SERVER_PID" "$CLIENT_PID"
        trap - INT TERM EXIT
        ;;
    demo)
        start_server &
        PIDS=("$!")
        sleep 1
        for USERNAME in An Binh Chi Dung Ha Lan; do
            start_client localhost 5000 "$USERNAME" "lab1-$USERNAME" &
            PIDS+=("$!")
        done
        echo "Lan dau: bam Dang ky tren moi client. Lan sau: bam Dang nhap."
        trap 'kill "${PIDS[@]}" 2>/dev/null || true' INT TERM EXIT
        wait "${PIDS[@]}"
        trap - INT TERM EXIT
        ;;
esac
