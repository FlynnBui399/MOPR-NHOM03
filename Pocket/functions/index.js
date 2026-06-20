"use strict";

const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

exports.sendFcmTriggerNotification = onDocumentCreated(
  "fcmTriggers/{triggerId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
      return;
    }

    const trigger = snapshot.data();
    if (!trigger || trigger.processed === true) {
      return;
    }

    const triggerRef = snapshot.ref;
    const type = stringValue(trigger.type);
    const recipientId = stringValue(trigger.recipientId);
    if (!recipientId) {
      await markSkipped(triggerRef, "missing_recipient");
      return;
    }

    const recipientSnapshot = await db.collection("users").doc(recipientId).get();
    const recipient = recipientSnapshot.exists ? recipientSnapshot.data() : null;
    if (!recipient) {
      await markSkipped(triggerRef, "recipient_not_found");
      return;
    }

    if (recipient.notificationsEnabled === false) {
      await markSkipped(triggerRef, "notifications_disabled");
      return;
    }

    const token = stringValue(recipient.fcmToken);
    if (!token) {
      await markSkipped(triggerRef, "missing_token");
      return;
    }

    const message = buildMessage(token, type, trigger);
    if (!message) {
      await markSkipped(triggerRef, "unsupported_type");
      return;
    }

    try {
      const messageId = await messaging.send(message);
      await triggerRef.update({
        processed: true,
        processedAt: admin.firestore.FieldValue.serverTimestamp(),
        messageId,
      });
    } catch (error) {
      logger.error("Unable to send FCM notification", {
        triggerId: event.params.triggerId,
        type,
        recipientId,
        error: error.message,
      });
      await triggerRef.update({
        processed: false,
        error: error.message,
        lastAttemptAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    }
  },
);

function buildMessage(token, type, trigger) {
  if (type === "message") {
    const senderName = stringValue(trigger.senderName) || "Pocket";
    const body = stringValue(trigger.body) || "New message";
    return dataMessage(token, {
      type,
      title: senderName,
      body,
      senderUid: stringValue(trigger.senderId),
      senderName,
      friendUid: stringValue(trigger.senderId),
    });
  }

  if (type === "photo_received") {
    const senderName = stringValue(trigger.senderName) || "Pocket";
    const caption = stringValue(trigger.caption);
    return dataMessage(token, {
      type,
      title: senderName,
      body: caption || `${senderName} sent you a new photo`,
      senderUid: stringValue(trigger.senderId),
      senderName,
      photoId: stringValue(trigger.photoId),
      imageUrl: stringValue(trigger.imageUrl),
      caption,
    });
  }

  return null;
}

function dataMessage(token, data) {
  return {
    token,
    data: stringifyData(data),
    android: {
      priority: "high",
    },
  };
}

function stringifyData(data) {
  return Object.entries(data).reduce((result, [key, value]) => {
    result[key] = value == null ? "" : String(value);
    return result;
  }, {});
}

function stringValue(value) {
  return typeof value === "string" ? value.trim() : "";
}

async function markSkipped(triggerRef, reason) {
  await triggerRef.update({
    processed: true,
    skippedReason: reason,
    processedAt: admin.firestore.FieldValue.serverTimestamp(),
  });
}
