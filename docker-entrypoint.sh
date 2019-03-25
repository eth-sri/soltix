#!/bin/sh

umask 0000 # avoid file deletion problems on host due to owner being root
exec "$@"
