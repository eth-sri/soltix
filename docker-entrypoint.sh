#!/bin/sh

umask 0000 # avoid file deletion problems on host due to owner being root

# Append existing environment variables to config, thereby enabling settings overrides.
# TODO Add SOLTIX_ prefix for soltix-related settings variables, then filter?
set | grep -v PATH >>./settings.cfg.sh

echo --------- docker-entrypoint.sh with settings.cfg.sh: -----------
cat settings.cfg.sh
echo ----------------------------------------------------------------

exec "$@"
