#!/bin/sh
set -eu

BACKUP_DIR=/backups
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
INTERVAL_SECONDS=$((24 * 60 * 60))

echo "Backup service started. Dumping every 24h, retaining ${RETENTION_DAYS} days, writing to ${BACKUP_DIR}."

while true; do
	timestamp=$(date -u +%Y%m%dT%H%M%SZ)
	dest="${BACKUP_DIR}/${PGDATABASE}-${timestamp}.sql.gz"

	if pg_dump --no-owner --no-acl | gzip > "${dest}.tmp"; then
		mv "${dest}.tmp" "${dest}"
		echo "Backup written: ${dest}"
	else
		echo "Backup FAILED at ${timestamp}" >&2
		rm -f "${dest}.tmp"
	fi

	find "${BACKUP_DIR}" -name "${PGDATABASE}-*.sql.gz" -mtime "+${RETENTION_DAYS}" -delete

	sleep "${INTERVAL_SECONDS}"
done
