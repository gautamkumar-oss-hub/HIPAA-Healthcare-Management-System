#!/bin/sh
set -eu

: "${AUTH_DB_PASSWORD:?AUTH_DB_PASSWORD is required}"
: "${PATIENT_DB_PASSWORD:?PATIENT_DB_PASSWORD is required}"
: "${APPOINTMENT_DB_PASSWORD:?APPOINTMENT_DB_PASSWORD is required}"
: "${NOTIFICATION_DB_PASSWORD:?NOTIFICATION_DB_PASSWORD is required}"
: "${AUDIT_DB_PASSWORD:?AUDIT_DB_PASSWORD is required}"

psql --set=ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  --set=auth_password="$AUTH_DB_PASSWORD" \
  --set=patient_password="$PATIENT_DB_PASSWORD" \
  --set=appointment_password="$APPOINTMENT_DB_PASSWORD" \
  --set=notification_password="$NOTIFICATION_DB_PASSWORD" \
  --set=audit_password="$AUDIT_DB_PASSWORD" \
  --file=/scripts/create-service-databases.sql
