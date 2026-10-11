#!/usr/bin/env bash
# 로컬(또는 GitHub Actions)에서 빌드해 복순 서버로 올리고 DLog 를 재시작한다.
#
# 사용법: ./deploy/deploy.sh <서버IP> <SSH 개인키 경로>
#   접속 계정은 기본 root 이고, 다른 계정이면 SSH_USER=ubuntu ./deploy/deploy.sh ... 처럼 지정한다.
#   서버 설정(setup.sh)은 매번 실행되며 여러 번 실행해도 안전하다.
#
# 선택 환경변수(있으면 서버 /etc/dlog.env 에 반영, 비어 있으면 기존 값 유지):
#   ADMIN_PASSWORD_HASH  관리자 비밀번호 BCrypt 해시 (최초 배포 시 필수)
#   ADMIN_USERNAME, UPBIT_ACCESS_KEY, UPBIT_SECRET_KEY, TOSS_CLIENT_ID, TOSS_CLIENT_SECRET
#
# 필요한 것: JDK 17 이상, npm, ssh/scp (Git Bash 에 포함)
set -euo pipefail

HOST="${1:?서버 IP를 지정하세요}"
KEY="${2:?SSH 키 경로를 지정하세요}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REMOTE="${SSH_USER:-root}@${HOST}"
SSH_OPTS=(-i "$KEY" -o StrictHostKeyChecking=accept-new)
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

echo "== 백엔드 빌드"
(cd "$ROOT/backend" && ./gradlew bootJar --console=plain)
JAR="$(ls "$ROOT"/backend/build/libs/*.jar | head -1)"

echo "== 프론트엔드 빌드"
(cd "$ROOT/frontend" && npm install --no-audit --no-fund && npm run build)
tar -C "$ROOT/frontend/dist" -czf "$WORK/dlog-web.tgz" .

echo "== 업로드"
ssh "${SSH_OPTS[@]}" "$REMOTE" "rm -rf /tmp/dlog-deploy && mkdir -p /tmp/dlog-deploy"
scp "${SSH_OPTS[@]}" "$JAR" "${REMOTE}:/tmp/dlog-deploy/dlog.jar"
scp "${SSH_OPTS[@]}" "$WORK/dlog-web.tgz" "$ROOT"/deploy/setup.sh "$ROOT"/deploy/dlog.service "$ROOT"/deploy/nginx-app.conf "${REMOTE}:/tmp/dlog-deploy/"

echo "== 서버 설정"
PUBLIC_PORT="${DLOG_PUBLIC_PORT:-8443}"
SETUP_OUT="$(ssh "${SSH_OPTS[@]}" "$REMOTE" "sudo DLOG_PUBLIC_PORT=${PUBLIC_PORT} bash /tmp/dlog-deploy/setup.sh")"
echo "$SETUP_OUT"

# 비밀값은 명령줄이 아니라 표준입력으로 넘겨 로그·프로세스 목록에 남지 않게 한다.
# BCrypt 해시의 '$' 가 해석되지 않도록 작은따옴표로 감싸 저장한다(systemd EnvironmentFile 규칙).
for name in ADMIN_PASSWORD_HASH ADMIN_USERNAME UPBIT_ACCESS_KEY UPBIT_SECRET_KEY TOSS_CLIENT_ID TOSS_CLIENT_SECRET; do
  value="${!name:-}"
  [ -n "$value" ] || continue
  if [[ "$value" == *"'"* || "$value" == *$'\n'* ]]; then
    echo "오류: $name 값에 작은따옴표나 줄바꿈은 사용할 수 없습니다." >&2
    exit 1
  fi
  echo "== 서버 설정($name) 반영"
  printf '%s' "$value" | ssh "${SSH_OPTS[@]}" "$REMOTE" "
    set -e
    value=\$(cat)
    sudo sed -i '/^$name=/d' /etc/dlog.env
    printf \"$name='%s'\\n\" \"\$value\" | sudo tee -a /etc/dlog.env >/dev/null
  "
done

echo "== 반영 및 재시작"
ssh "${SSH_OPTS[@]}" "$REMOTE" "
  set -e
  if ! sudo grep -q '^ADMIN_PASSWORD_HASH=' /etc/dlog.env; then
    echo '오류: 관리자 비밀번호 해시(ADMIN_PASSWORD_HASH)가 서버에 없습니다. GitHub Secret 또는 환경변수로 지정하세요.' >&2
    exit 1
  fi
  sudo install -o dlog -g dlog -m 644 /tmp/dlog-deploy/dlog.jar /opt/dlog/dlog.jar
  sudo find /var/www/dlog -mindepth 1 -delete
  sudo tar -xzf /tmp/dlog-deploy/dlog-web.tgz -C /var/www/dlog
  sudo nginx -t
  sudo systemctl reload nginx
  sudo systemctl restart dlog

  # 기동 확인 (최대 120초). 1GB 서버라 JVM 기동이 느릴 수 있다.
  for i in \$(seq 1 60); do
    if curl -fs -o /dev/null http://127.0.0.1:8081/api/posts; then
      echo 'DLog 기동 확인'
      rm -rf /tmp/dlog-deploy
      exit 0
    fi
    sleep 2
  done
  # 공개 저장소의 Actions 로그에 설정값이 노출되지 않도록 서버 로그는 출력하지 않는다.
  echo '오류: DLog 가 120초 안에 기동되지 않았습니다. 서버에서 확인: journalctl -u dlog -n 100' >&2
  exit 1
"

SCHEME="$(printf '%s\n' "$SETUP_OUT" | sed -n 's/^DLOG_SCHEME=//p')"
DOMAIN="$(printf '%s\n' "$SETUP_OUT" | sed -n 's/^DLOG_DOMAIN=//p')"
echo "완료: ${SCHEME:-http}://${DOMAIN:-$HOST}:${PUBLIC_PORT}"
