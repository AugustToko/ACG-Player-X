#!/bin/sh

# Decrypt the file
gpg --quiet --batch --yes --decrypt --passphrase="$LARGE_SECRET_PASSPHRASE" \
--output $GITHUB_WORKSPACE/android/key.properties $GITHUB_WORKSPACE/.github/secrets/keystore.properties.gpg

echo "Decrypt key.properties done"

gpg --quiet --batch --yes --decrypt --passphrase="$LARGE_SECRET_PASSPHRASE" \
--output $GITHUB_WORKSPACE/.github/secrets/key.jks $GITHUB_WORKSPACE/.github/secrets/key.jks.gpg

echo "Decrypt key.jks done"