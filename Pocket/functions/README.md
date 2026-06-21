# Pocket Firebase Functions

This directory contains the Node.js 20 Cloud Functions used by Pocket:

- `sendPhotoNotification`: sends FCM notifications when a photo is created.
- `sendChatMessageNotification`: sends adaptive chat notifications when a
  message is created.
- `sendReactionNotification`: notifies a Pocket sender when a reaction is
  added or changed.
- `markLegacyFcmTriggerProcessed`: closes legacy client-created trigger records
  without sending a duplicate notification.
- `resetExpiredStreaks`: resets expired streaks daily at 00:05 ICT.
- `deleteCloudinaryAsset`: deletes Cloudinary media after its Firestore photo
  document is deleted.

## Configure secrets

Run these commands from the `Pocket` directory:

```powershell
firebase functions:secrets:set CLOUDINARY_CLOUD_NAME
firebase functions:secrets:set CLOUDINARY_API_KEY
firebase functions:secrets:set CLOUDINARY_API_SECRET
```

The values are stored in Google Secret Manager and are only attached to
`deleteCloudinaryAsset`.

## Validate

```powershell
cd functions
npm install
npm run lint
```

## Deploy

```powershell
firebase use mobile-program
firebase deploy --only functions
```

Firebase Admin SDK operations bypass client Firestore security rules, so no
additional client rule grants are required for these functions.
