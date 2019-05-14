#!/bin/sh

umask 0000 # avoid file deletion problems on host due to owner being root

# Append existing environment variables to config, thereby enabling settings overrides.
# TODO Add SOLTIX_ prefix for soltix-related settings variables, then filter?
set >>./settings.cfg.sh

exec "$@"
