#!/bin/sh
set -eu

: "${DOMAIN_NAME:=_}"
: "${ENABLE_HTTPS:=false}"
: "${LETSENCRYPT_ENABLED:=false}"
: "${LETSENCRYPT_EMAIL:=admin@example.com}"
: "${LETSENCRYPT_STAGING:=false}"
: "${LETSENCRYPT_RENEW_INTERVAL_HOURS:=12}"
: "${LETSENCRYPT_LOG_FORMAT:=json}"
: "${SSL_CERT_PATH:=/etc/nginx/certs/fullchain.pem}"
: "${SSL_KEY_PATH:=/etc/nginx/certs/privkey.pem}"
: "${ACME_WEBROOT:=/var/www/certbot}"

is_true() {
  case "$1" in
    [Tt][Rr][Uu][Ee]|1|[Yy][Ee][Ss]|[Yy]|[Oo][Nn]|[Tt]) return 0 ;;
    *) return 1 ;;
  esac
}

escape_json() {
  printf '%s' "$1" \
    | sed 's/\\/\\\\/g' \
    | sed 's/"/\\"/g' \
    | sed 's/\r/ /g' \
    | sed 's/\n/\\n/g'
}

log() {
  level="$1"
  event="$2"
  msg="$3"
  ts="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
  staging="$(is_true "${LETSENCRYPT_STAGING}" && echo "true" || echo "false")"
  interval="${LETSENCRYPT_RENEW_INTERVAL_HOURS}"
  if [ "${LETSENCRYPT_LOG_FORMAT}" = "json" ]; then
    printf '{"ts":"%s","level":"%s","service":"community-proxy","event":"%s","domain":"%s","staging":%s,"renew_interval_hours":%s,"msg":"%s"}\n' \
      "${ts}" "$(escape_json "${level}")" "$(escape_json "${event}")" "$(escape_json "${DOMAIN_NAME}")" "${staging}" "${interval}" "$(escape_json "${msg}")"
  else
    printf 'ts=%s level=%s service=community-proxy event=%s domain=%s staging=%s renew_interval_hours=%s msg=%s\n' \
      "${ts}" "${level}" "${event}" "${DOMAIN_NAME}" "${staging}" "${interval}" "${msg}"
  fi
}

run_certbot_cmd() {
  # $1=label, $2...=certbot args
  label="$1"
  shift

  log "info" "${label}_start" "command starts"
  if output="$(certbot "$@" 2>&1)"; then
    log "info" "${label}_ok" "command completed"
  else
    rc=$?
    log "error" "${label}_failed" "command failed rc=${rc}"
    printf '%s\n' "$output" | head -n 20 | sed 's/^/CERTBOT_LOG: /'
    return "${rc}"
  fi
}

reload_nginx() {
  nginx -s reload > /dev/null 2>&1 || true
}

if is_true "${ENABLE_HTTPS}"; then
  log "info" "proxy_mode" "HTTPS enabled"
  TEMPLATE_FILE="/etc/nginx/templates/default-https.conf.template"
  mkdir -p "${ACME_WEBROOT}"

  if is_true "${LETSENCRYPT_ENABLED}"; then
    SSL_CERT_PATH="/etc/letsencrypt/live/${DOMAIN_NAME}/fullchain.pem"
    SSL_KEY_PATH="/etc/letsencrypt/live/${DOMAIN_NAME}/privkey.pem"
    if [ "${DOMAIN_NAME}" = "_" ] || [ -z "${DOMAIN_NAME}" ]; then
      log "error" "config_invalid" "LETSENCRYPT_ENABLED=true requires DOMAIN_NAME"
      exit 1
    fi

    LE_ARGS=""
    if is_true "${LETSENCRYPT_STAGING}"; then
      LE_ARGS="--staging"
    fi

    if [ ! -f "${SSL_CERT_PATH}" ] || [ ! -f "${SSL_KEY_PATH}" ]; then
      run_certbot_cmd "certbot_issue" certonly \
        --non-interactive \
        --agree-tos \
        --no-eff-email \
        --email "${LETSENCRYPT_EMAIL}" \
        --webroot -w "${ACME_WEBROOT}" \
        -d "${DOMAIN_NAME}" ${LE_ARGS}
    else
      log "info" "certbot_issue_skipped" "existing certificate detected"
    fi

    if [ ! -f "${SSL_CERT_PATH}" ] || [ ! -f "${SSL_KEY_PATH}" ]; then
      log "error" "cert_path_missing" "certbot issue/prepare failed"
      exit 1
    fi
    log "info" "cert_ready" "certificate found for reverse-proxy"

    if echo "${LETSENCRYPT_RENEW_INTERVAL_HOURS}" | grep -Eq '^[0-9]+$'; then
      if [ "${LETSENCRYPT_RENEW_INTERVAL_HOURS}" -gt 0 ]; then
        sleep_seconds=$((LETSENCRYPT_RENEW_INTERVAL_HOURS * 3600))
        (
          while true; do
            log "info" "certbot_renew_cycle" "start"
            if certbot renew --non-interactive ${LE_ARGS} --webroot -w "${ACME_WEBROOT}" --deploy-hook "nginx -s reload" --quiet; then
              log "info" "certbot_renew_ok" "renew completed"
            else
              log "error" "certbot_renew_failed" "renew attempt failed"
            fi
            log "info" "certbot_renew_wait" "next_after_seconds=${sleep_seconds}"
            sleep "${sleep_seconds}"
          done
        ) &
      else
        log "warn" "renew_schedule_disabled" "LETSENCRYPT_RENEW_INTERVAL_HOURS is 0"
      fi
    else
      log "error" "renew_schedule_invalid" "LETSENCRYPT_RENEW_INTERVAL_HOURS must be numeric"
      exit 1
    fi
  else
    if [ ! -f "${SSL_CERT_PATH}" ] || [ ! -f "${SSL_KEY_PATH}" ]; then
      log "error" "cert_path_missing" "ENABLE_HTTPS=true but certificate files are not found"
      exit 1
    fi
    log "info" "cert_manual" "using pre-mounted certificates"
  fi
else
  log "info" "proxy_mode" "HTTP only mode"
  TEMPLATE_FILE="/etc/nginx/templates/default-http.conf.template"
fi

cp "${TEMPLATE_FILE}" /etc/nginx/conf.d/default.conf
sed -i "s|@@DOMAIN_NAME@@|${DOMAIN_NAME}|g" /etc/nginx/conf.d/default.conf
sed -i "s|@@SSL_CERT_PATH@@|${SSL_CERT_PATH}|g" /etc/nginx/conf.d/default.conf
sed -i "s|@@SSL_KEY_PATH@@|${SSL_KEY_PATH}|g" /etc/nginx/conf.d/default.conf

log "info" "nginx_config" "applied template to /etc/nginx/conf.d/default.conf"
log "info" "nginx_run" "starting nginx foreground"

nginx -g "daemon off;"
