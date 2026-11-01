#!/usr/bin/env bash
# Creates the ONE signing key for ToDoList releases and prints what to put into GitHub Secrets.
#
#   ./tools/generate-keystore.sh [output.jks] [alias]
#
# Why one key: Android installs an update only if it is signed with the same key as the installed
# app. Lose the key (or let CI invent a new one per build) and nobody can update without uninstalling.
# Back the .jks file up somewhere safe. It is git-ignored; never commit it.
#
# Non-interactive use: KEYSTORE_PASSWORD='...' ./tools/generate-keystore.sh
set -euo pipefail

OUT="${1:-todolist-release.jks}"
ALIAS="${2:-todolist}"

command -v keytool >/dev/null || { echo "keytool not found. Install a JDK (17+)." >&2; exit 1; }
[ ! -e "$OUT" ] || { echo "$OUT already exists. Refusing to overwrite a signing key." >&2; exit 1; }

PASSWORD="${KEYSTORE_PASSWORD:-}"
if [ -z "$PASSWORD" ]; then
  read -rsp "Choose a key password (min 6 characters): " PASSWORD; echo
  read -rsp "Repeat it: " AGAIN; echo
  [ "$PASSWORD" = "$AGAIN" ] || { echo "Passwords differ." >&2; exit 1; }
fi
[ "${#PASSWORD}" -ge 6 ] || { echo "Password must have at least 6 characters." >&2; exit 1; }

# One password for store and key: the default PKCS12 format requires them to be equal.
keytool -genkeypair -keystore "$OUT" -alias "$ALIAS" -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass "$PASSWORD" -keypass "$PASSWORD" -dname "CN=ToDoList, O=ToDoList, C=UA" >/dev/null

echo
echo "Created $OUT (alias: $ALIAS). BACK IT UP, and never commit it."
echo
echo "Add these four secrets in GitHub: repository -> Settings -> Secrets and variables -> Actions:"
echo "  KEYSTORE_BASE64    = (the base64 text of the .jks file)"
echo "  KEYSTORE_PASSWORD  = the password you chose"
echo "  KEY_ALIAS          = $ALIAS"
echo "  KEY_PASSWORD       = the same password"
echo
if command -v gh >/dev/null && gh auth status >/dev/null 2>&1; then
  echo "The GitHub CLI is logged in, so you can run:"
  echo "  base64 -w0 \"$OUT\" | gh secret set KEYSTORE_BASE64"
  echo "  gh secret set KEYSTORE_PASSWORD   # paste the password"
  echo "  gh secret set KEY_ALIAS --body \"$ALIAS\""
  echo "  gh secret set KEY_PASSWORD        # paste the password"
else
  echo "To get the base64 text:  base64 -w0 \"$OUT\"   (on macOS: base64 -i \"$OUT\")"
fi
