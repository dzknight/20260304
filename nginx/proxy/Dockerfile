FROM nginx:1.27-alpine

RUN apk add --no-cache certbot curl

RUN rm -f /etc/nginx/conf.d/default.conf

COPY proxy/default-http.conf.template /etc/nginx/templates/default-http.conf.template
COPY proxy/default-https.conf.template /etc/nginx/templates/default-https.conf.template
COPY proxy/entrypoint.sh /docker-entrypoint-proxy.sh

RUN chmod +x /docker-entrypoint-proxy.sh

ENTRYPOINT ["/docker-entrypoint-proxy.sh"]
