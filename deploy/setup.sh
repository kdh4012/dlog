#!/usr/bin/env bash
# DLog 서버 설정. deploy.sh 가 배포 때마다 서버에서 sudo 로 실행한다(여러 번 실행해도 안전).
# 복순 앱이 이미 올라가 있는 서버를 전제로 하며, 복순 설정 파일은 건드리지 않는다.
#
# 구성: 외부 https://<복순 도메인>:8443 → nginx → 127.0.0.1:8081 (DLog 백엔드)
set -euo pipefail

DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
PUBLIC_PORT="${DLOG_PUBLIC_PORT:-8443}"
BOKSOON_HTTPS_CONF=/etc/nginx/conf.d/boksoon-https.conf

# 복순 setup.sh 로 이미 설치되어 있으면 건너뛴다.
if ! command -v java >/dev/null || ! command -v nginx >/dev/null || ! command -v psql >/dev/null; then
  apt-get update
  DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-17-jre-headless nginx postgresql
fi

if command -v ufw >/dev/null 2>&1; then
  ufw allow "${PUBLIC_PORT}/tcp" >/dev/null
fi

id -u dlog >/dev/null 2>&1 || useradd --system --home /opt/dlog --shell /usr/sbin/nologin dlog
mkdir -p /opt/dlog /var/www/dlog
chown dlog:dlog /opt/dlog

# DB 계정과 서버 비밀값은 최초 1회만 만든다. 이미 있으면 기존 값을 유지한다.
if [ ! -f /etc/dlog.env ]; then
  DB_PASSWORD="$(openssl rand -hex 16)"
  JWT_SECRET="$(openssl rand -hex 32)"
  sudo -u postgres psql -qc "CREATE ROLE dlog LOGIN PASSWORD '${DB_PASSWORD}'"
  sudo -u postgres createdb -O dlog dlog
  cat > /etc/dlog.env <<ENV
SPRING_PROFILES_ACTIVE=prod
SERVER_ADDRESS=127.0.0.1
PORT=8081
DB_URL=jdbc:postgresql://localhost:5432/dlog
DB_USERNAME=dlog
DB_PASSWORD=${DB_PASSWORD}
JWT_SECRET=${JWT_SECRET}
ADMIN_USERNAME=admin
ENV
  chmod 600 /etc/dlog.env
fi

install -m 644 "${DEPLOY_DIR}/dlog.service" /etc/systemd/system/dlog.service
systemctl daemon-reload
systemctl enable dlog >/dev/null

# 복순 HTTPS 설정에서 인증서 경로를 찾아 같은 인증서를 8443 에서 재사용한다.
# 인증서는 도메인 기준이라 포트가 달라도 그대로 쓸 수 있다.
CERT=""
KEY=""
if [ -f "$BOKSOON_HTTPS_CONF" ]; then
  CERT="$(awk '$1=="ssl_certificate"{gsub(";","",$2); print $2; exit}' "$BOKSOON_HTTPS_CONF")"
  KEY="$(awk '$1=="ssl_certificate_key"{gsub(";","",$2); print $2; exit}' "$BOKSOON_HTTPS_CONF")"
fi

mkdir -p /etc/nginx/snippets
install -m 644 "${DEPLOY_DIR}/nginx-app.conf" /etc/nginx/snippets/dlog-app.conf

if [ -n "$CERT" ] && [ -f "$CERT" ] && [ -n "$KEY" ] && [ -f "$KEY" ]; then
  cat > /etc/nginx/sites-available/dlog <<NGINX
# DLog: https://<도메인>:${PUBLIC_PORT} (setup.sh 가 생성, 직접 수정하지 말 것)
server {
    # Ubuntu 24.04 기본 nginx(1.24)는 'http2 on;' 지시어가 없어 listen 에 붙인다(복순 설정과 동일)
    listen ${PUBLIC_PORT} ssl http2;
    listen [::]:${PUBLIC_PORT} ssl http2;
    server_name _;

    ssl_certificate ${CERT};
    ssl_certificate_key ${KEY};
    ssl_protocols TLSv1.2 TLSv1.3;

    # 같은 포트로 http:// 로 들어오면(nginx 오류 497) https 로 보낸다.
    error_page 497 =301 https://\$host:\$server_port\$request_uri;

    include snippets/dlog-app.conf;
}
NGINX
  echo "DLOG_SCHEME=https"
else
  # 인증서가 없으면 HTTP 로만 연다. 로그인 비밀번호가 암호화되지 않으므로 복순 HTTPS 설정 후 다시 배포할 것.
  cat > /etc/nginx/sites-available/dlog <<NGINX
# DLog: http://<서버IP>:${PUBLIC_PORT} (HTTPS 인증서 없음, setup.sh 가 생성)
server {
    listen ${PUBLIC_PORT};
    listen [::]:${PUBLIC_PORT};
    server_name _;

    include snippets/dlog-app.conf;
}
NGINX
  echo "DLOG_SCHEME=http"
  echo "경고: HTTPS 인증서를 찾지 못해 HTTP 로 엽니다. (복순 deploy/enable-https.sh 실행 후 다시 배포하면 HTTPS 로 바뀜)" >&2
fi
ln -sf /etc/nginx/sites-available/dlog /etc/nginx/sites-enabled/dlog

if [ -n "$CERT" ]; then
  # 접속 주소 출력용: 인증서 경로(/etc/letsencrypt/live/<도메인>/...)에서 도메인을 꺼낸다.
  echo "DLOG_DOMAIN=$(basename "$(dirname "$CERT")")"
fi
