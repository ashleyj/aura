#!/bin/sh

BASE=$(dirname $0)

mkdir -p "$BASE/binaries"
rsync -av --exclude '*-dbg.a' --include '*.a' --include '*/' --exclude '**' "$BASE/target/binaries/" "$BASE/binaries/"
