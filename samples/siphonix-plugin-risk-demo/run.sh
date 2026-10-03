#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
DEMO="$ROOT/samples/siphonix-plugin-risk-demo"

mvn -q -pl siphonix-runtime,siphonix-plugins/dependency-conflict-demo-plugin,siphonix-plugins/security-demo-plugin -am package

rm -rf "$DEMO/plugins" "$DEMO/fixture"
mkdir -p "$DEMO/plugins" "$DEMO/fixture/containers" "$DEMO/fixture/database" "$DEMO/fixture/secrets"
cp "$ROOT/siphonix-runtime/target/io.github.brice10.siphonix-jar-with-dependencies.jar" "$DEMO/siphonix-app.jar"
cp "$ROOT/siphonix-plugins/dependency-conflict-demo-plugin/target/dependency-conflict-demo-plugin-1.0.0.jar" "$DEMO/plugins/"
cp "$ROOT/siphonix-plugins/security-demo-plugin/target/security-demo-plugin-1.0.0.jar" "$DEMO/plugins/"
printf 'disposable=true\n' > "$DEMO/fixture/.siphonix-security-demo"
printf 'RUNNING\n' > "$DEMO/fixture/containers/target.status"
printf 'id=42,email=fake@example.test\n' > "$DEMO/fixture/database/records.txt"
printf 'DEMO_ONLY_FAKE_SECRET\n' > "$DEMO/fixture/secrets/demo.secret"

cleanup() {
  docker compose -f "$DEMO/docker-compose.yml" down --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker compose -f "$DEMO/docker-compose.yml" up --build --abort-on-container-exit --exit-code-from siphonix

printf '\nFixture after the simulation:\n'
printf 'target.status: '
sed -n '1p' "$DEMO/fixture/containers/target.status"
if [[ ! -e "$DEMO/fixture/database/records.txt" ]]; then
  printf 'database/records.txt: DELETED\n'
fi
printf 'security-demo.log:\n'
sed -n '1,20p' "$DEMO/fixture/security-demo.log"
