"use strict";

const {defineSecret} = require("firebase-functions/params");
const {
  onDocumentCreated,
  onDocumentDeleted,
  onDocumentUpdated,
} = require("firebase-functions/v2/firestore");
const {onSchedule} = require("firebase-functions/v2/scheduler");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();
const FieldValue = admin.firestore.FieldValue;

const CLOUDINARY_CLOUD_NAME = defineSecret("CLOUDINARY_CLOUD_NAME");
const CLOUDINARY_API_KEY = defineSecret("CLOUDINARY_API_KEY");
const CLOUDINARY_API_SECRET = defineSecret("CLOUDINARY_API_SECRET");

const INVALID_TOKEN_CODES = new Set([
  "messaging/invalid-registration-token",
  "messaging/registration-token-not-registered",
]);

exports.sendPhotoNotification = onDocumentCreated(
  "photos/{photoId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
      return;
    }

    const photo = snapshot.data() || {};
    const receiverIds = uniqueStrings(photo.receiverIds || photo.recipients);
    if (receiverIds.length === 0) {
      logger.info("Photo has no notification receivers", {
        photoId: event.params.photoId,
      });
      return;
    }

    const senderId = stringValue(photo.senderId);
    const senderName = await resolveSenderName(senderId, photo.senderName);
    const caption = stringValue(photo.caption);
    const body = caption || "Sent you a Pocket 📸";
    const mediaType = stringValue(photo.type) || "image";
    const imageUrl = resolveNotificationImage(photo, mediaType);
    const targets = await loadNotificationTargets(receiverIds);

    if (targets.length === 0) {
      logger.info("No valid FCM tokens found for photo", {
        photoId: event.params.photoId,
        receiverCount: receiverIds.length,
      });
      return;
    }

    const messages = targets.map((target) =>
      buildPhotoMessage({
        token: target.token,
        photoId: event.params.photoId,
        senderId,
        senderName,
        body,
        mediaType,
        imageUrl,
      }),
    );
    const results = await Promise.allSettled(
      messages.map((message) => messaging.send(message)),
    );

    let sentCount = 0;
    let failedCount = 0;
    const cleanupTasks = [];

    results.forEach((result, index) => {
      if (result.status === "fulfilled") {
        sentCount += 1;
        return;
      }

      failedCount += 1;
      const target = targets[index];
      const error = result.reason;
      logger.warn("Unable to send photo notification", {
        photoId: event.params.photoId,
        receiverId: target.uid,
        code: error && error.code,
        message: error && error.message,
      });
      if (error && INVALID_TOKEN_CODES.has(error.code)) {
        cleanupTasks.push(removeInvalidToken(target.uid, target.token));
      }
    });

    await Promise.allSettled(cleanupTasks);
    logger.info("Photo notification delivery finished", {
      photoId: event.params.photoId,
      sentCount,
      failedCount,
      invalidTokensRemoved: cleanupTasks.length,
    });
  },
);

exports.markLegacyFcmTriggerProcessed = onDocumentCreated(
  "fcmTriggers/{triggerId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot || snapshot.get("processed") === true) {
      return;
    }

    await snapshot.ref.set({
      processed: true,
      processedAt: FieldValue.serverTimestamp(),
      processedBy: "sendPhotoNotification",
    }, {merge: true});
  },
);

exports.sendChatMessageNotification = onDocumentCreated(
  "chats/{chatId}/messages/{messageId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
      return;
    }

    const message = snapshot.data() || {};
    const senderId = stringValue(message.senderId);
    if (!senderId) {
      logger.info("Chat message has no sender", {
        chatId: event.params.chatId,
        messageId: event.params.messageId,
      });
      return;
    }

    const chatSnapshot = await db.collection("chats")
      .doc(event.params.chatId)
      .get();
    const chat = chatSnapshot.exists ? chatSnapshot.data() || {} : {};
    const participants = resolveChatParticipants(
      chat,
      message,
      event.params.chatId,
    );
    const receiverIds = participants.filter((uid) => uid !== senderId);
    if (receiverIds.length === 0) {
      return;
    }

    const senderName = await resolveSenderName(senderId, message.senderName);
    const messageType = stringValue(message.type) || "text";
    const body = chatMessageBody(message, messageType);
    const targets = await loadNotificationTargets(receiverIds);
    const messages = targets.map((target) => ({
      token: target.token,
      data: stringifyData({
        type: "chat_message",
        messageType,
        chatId: event.params.chatId,
        messageId: event.params.messageId,
        senderId,
        senderUid: senderId,
        friendUid: senderId,
        senderName,
        title: senderName,
        body,
      }),
      android: {
        priority: "high",
      },
    }));

    await deliverNotifications(messages, targets, {
      notificationType: "chat_message",
      chatId: event.params.chatId,
      messageId: event.params.messageId,
    });
  },
);

exports.sendReactionNotification = onDocumentUpdated(
  "photos/{photoId}",
  async (event) => {
    const beforeSnapshot = event.data && event.data.before;
    const afterSnapshot = event.data && event.data.after;
    if (!beforeSnapshot || !afterSnapshot) {
      return;
    }

    const before = beforeSnapshot.data() || {};
    const after = afterSnapshot.data() || {};
    const senderId = stringValue(after.senderId);
    if (!senderId) {
      return;
    }

    const beforeReactions = reactionMap(before.reactions);
    const afterReactions = reactionMap(after.reactions);
    const changedReactions = Object.entries(afterReactions)
      .filter(([reactorId, emoji]) =>
        reactorId !== senderId &&
        emoji &&
        beforeReactions[reactorId] !== emoji,
      );
    if (changedReactions.length === 0) {
      return;
    }

    const targets = await loadNotificationTargets([senderId]);
    if (targets.length === 0) {
      return;
    }

    const imageUrl = resolveNotificationImage(
      after,
      stringValue(after.type) || "image",
    );
    for (const [reactorId, emoji] of changedReactions) {
      const reactorName = await resolveSenderName(reactorId, "");
      const body = `${emoji} reacted to your Pocket`;
      const messages = targets.map((target) => ({
        token: target.token,
        data: stringifyData({
            type: "reaction",
            photoId: event.params.photoId,
            reactorId,
            senderId: reactorId,
            senderUid: reactorId,
            senderName: reactorName,
            title: reactorName,
            body,
            imageUrl,
        }),
        android: {
          priority: "high",
        },
      }));

      await deliverNotifications(messages, targets, {
        notificationType: "reaction",
        photoId: event.params.photoId,
        reactorId,
      });
    }
  },
);

exports.resetExpiredStreaks = onSchedule(
  {
    schedule: "5 17 * * *",
    timeZone: "UTC",
  },
  async () => {
    const snapshot = await db.collection("streaks").get();
    const now = Date.now();
    const oneDayMs = 24 * 60 * 60 * 1000;
    const twoDaysMs = 48 * 60 * 60 * 1000;
    let batch = db.batch();
    let batchSize = 0;
    let resetCount = 0;

    for (const document of snapshot.docs) {
      const streak = document.data() || {};
      const lastUpdated = streak.lastUpdated;
      if (!lastUpdated || typeof lastUpdated.toMillis !== "function") {
        continue;
      }

      const ageMs = now - lastUpdated.toMillis();
      const userAPosted = streak.userAPostedToday === true;
      const userBPosted = streak.userBPostedToday === true;
      const onlyOnePosted = userAPosted !== userBPosted;
      const bothPosted = userAPosted && userBPosted;
      const shouldReset =
        (onlyOnePosted && ageMs > oneDayMs) ||
        (!bothPosted && ageMs > twoDaysMs);

      if (!shouldReset) {
        continue;
      }

      batch.update(document.ref, {
        streakCount: 0,
        userAPostedToday: false,
        userBPostedToday: false,
        lastUpdated: FieldValue.serverTimestamp(),
      });
      batchSize += 1;
      resetCount += 1;

      if (batchSize === 450) {
        await batch.commit();
        batch = db.batch();
        batchSize = 0;
      }
    }

    if (batchSize > 0) {
      await batch.commit();
    }

    logger.info("Scheduled streak reset finished", {
      scannedCount: snapshot.size,
      resetCount,
    });
  },
);

exports.deleteCloudinaryAsset = onDocumentDeleted(
  {
    document: "photos/{photoId}",
    secrets: [
      CLOUDINARY_CLOUD_NAME,
      CLOUDINARY_API_KEY,
      CLOUDINARY_API_SECRET,
    ],
  },
  async (event) => {
    const snapshot = event.data;
    const photo = snapshot ? snapshot.data() : null;
    if (!photo) {
      return;
    }

    const publicId =
      stringValue(photo.publicId) ||
      stringValue(photo.cloudinaryPublicId);
    if (!publicId) {
      logger.info("Deleted photo has no Cloudinary public id", {
        photoId: event.params.photoId,
      });
      return;
    }

    const resourceType = stringValue(photo.type) === "video" ?
      "video" :
      "image";

    try {
      const result = await deleteCloudinaryResource(publicId, resourceType);
      logger.info("Cloudinary asset deletion finished", {
        photoId: event.params.photoId,
        publicId,
        resourceType,
        result,
      });
    } catch (error) {
      logger.error("Cloudinary asset deletion failed", {
        photoId: event.params.photoId,
        publicId,
        resourceType,
        message: error.message,
      });
    }
  },
);

async function resolveSenderName(senderId, storedName) {
  const senderName = stringValue(storedName);
  if (senderName) {
    return senderName;
  }
  if (!senderId) {
    return "Pocket";
  }

  const senderSnapshot = await db.collection("users").doc(senderId).get();
  if (!senderSnapshot.exists) {
    return "Pocket";
  }
  const sender = senderSnapshot.data() || {};
  return stringValue(sender.displayName) ||
    stringValue(sender.username) ||
    "Pocket";
}

async function loadNotificationTargets(receiverIds) {
  const references = receiverIds.map((uid) =>
    db.collection("users").doc(uid),
  );
  const snapshots = await db.getAll(...references);
  const targets = [];

  snapshots.forEach((snapshot) => {
    if (!snapshot.exists) {
      return;
    }

    const user = snapshot.data() || {};
    if (user.notificationsEnabled === false) {
      return;
    }

    userTokens(user).forEach((token) => {
      targets.push({
        uid: snapshot.id,
        token,
      });
    });
  });

  return targets;
}

function userTokens(user) {
  const tokens = [];
  const primaryToken = stringValue(user.fcmToken);
  if (primaryToken) {
    tokens.push(primaryToken);
  }
  if (Array.isArray(user.fcmTokens)) {
    tokens.push(...uniqueStrings(user.fcmTokens));
  }
  return [...new Set(tokens)];
}

function buildPhotoMessage({
  token,
  photoId,
  senderId,
  senderName,
  body,
  mediaType,
  imageUrl,
}) {
  const notification = {
    title: senderName,
    body,
  };
  const androidNotification = {
    channelId: "pocket_messages",
  };
  if (imageUrl) {
    notification.imageUrl = imageUrl;
    androidNotification.imageUrl = imageUrl;
  }

  return {
    token,
    notification,
    data: stringifyData({
      type: "photo_received",
      mediaType,
      photoId,
      senderId,
      senderUid: senderId,
      senderName,
      body,
      imageUrl,
    }),
    android: {
      priority: "high",
      notification: androidNotification,
    },
  };
}

function resolveChatParticipants(chat, message, chatId) {
  const participants = uniqueStrings(chat.participants);
  if (participants.length > 0) {
    return participants;
  }

  const senderId = stringValue(message.senderId);
  const receiverId = stringValue(message.receiverId);
  if (receiverId) {
    return uniqueStrings([senderId, receiverId]);
  }

  const legacyIds = stringValue(chatId).split("_").filter(Boolean);
  return uniqueStrings([senderId, ...legacyIds]);
}

function chatMessageBody(message, messageType) {
  const content =
    stringValue(message.content) ||
    stringValue(message.text) ||
    stringValue(message.emoji);
  switch (messageType) {
    case "pocket":
    case "photo":
      return "Sent you a Pocket \u{1F4F8}";
    case "video":
      return "Sent you a video Pocket \u{1F3A5}";
    case "reaction":
    case "emoji":
      return content;
    default:
      return truncate(content || "New message", 80);
  }
}

function reactionMap(value) {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    return {};
  }
  return Object.entries(value).reduce((result, [uid, emoji]) => {
    const normalizedUid = stringValue(uid);
    const normalizedEmoji = stringValue(emoji);
    if (normalizedUid && normalizedEmoji) {
      result[normalizedUid] = normalizedEmoji;
    }
    return result;
  }, {});
}

async function deliverNotifications(messages, targets, context) {
  if (messages.length === 0) {
    return;
  }

  const results = await Promise.allSettled(
    messages.map((message) => messaging.send(message)),
  );
  const cleanupTasks = [];
  let sentCount = 0;
  let failedCount = 0;

  results.forEach((result, index) => {
    if (result.status === "fulfilled") {
      sentCount += 1;
      return;
    }

    failedCount += 1;
    const target = targets[index];
    const error = result.reason;
    logger.warn("Unable to send notification", {
      ...context,
      receiverId: target && target.uid,
      code: error && error.code,
      message: error && error.message,
    });
    if (target && error && INVALID_TOKEN_CODES.has(error.code)) {
      cleanupTasks.push(removeInvalidToken(target.uid, target.token));
    }
  });

  await Promise.allSettled(cleanupTasks);
  logger.info("Notification delivery finished", {
    ...context,
    sentCount,
    failedCount,
    invalidTokensRemoved: cleanupTasks.length,
  });
}

function resolveNotificationImage(photo, mediaType) {
  const thumbnailUrl = stringValue(photo.thumbnailUrl);
  if (thumbnailUrl) {
    return thumbnailUrl;
  }
  if (mediaType === "video") {
    return stringValue(photo.imageUrl) ||
      stringValue(photo.videoThumbnailUrl);
  }
  return stringValue(photo.imageUrl);
}

async function removeInvalidToken(uid, invalidToken) {
  const userRef = db.collection("users").doc(uid);
  await db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(userRef);
    if (!snapshot.exists) {
      return;
    }

    const user = snapshot.data() || {};
    const updates = {};
    if (stringValue(user.fcmToken) === invalidToken) {
      updates.fcmToken = FieldValue.delete();
    }
    if (Array.isArray(user.fcmTokens) &&
        user.fcmTokens.includes(invalidToken)) {
      updates.fcmTokens = FieldValue.arrayRemove(invalidToken);
    }
    if (Object.keys(updates).length > 0) {
      transaction.update(userRef, updates);
    }
  });
}

async function deleteCloudinaryResource(publicId, resourceType) {
  const cloudName = CLOUDINARY_CLOUD_NAME.value();
  const apiKey = CLOUDINARY_API_KEY.value();
  const apiSecret = CLOUDINARY_API_SECRET.value();
  const endpoint =
    `https://api.cloudinary.com/v1_1/${encodeURIComponent(cloudName)}` +
    `/resources/${resourceType}/upload`;
  const body = new URLSearchParams();
  body.append("public_ids[]", publicId);
  body.append("invalidate", "true");

  const authorization = Buffer.from(`${apiKey}:${apiSecret}`)
    .toString("base64");
  const response = await fetch(endpoint, {
    method: "DELETE",
    headers: {
      "Authorization": `Basic ${authorization}`,
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: body.toString(),
  });
  const responseText = await response.text();

  if (!response.ok) {
    throw new Error(
      `Cloudinary returned ${response.status}: ${responseText}`,
    );
  }

  try {
    return JSON.parse(responseText);
  } catch (error) {
    return responseText;
  }
}

function stringifyData(data) {
  return Object.entries(data).reduce((result, [key, value]) => {
    result[key] = value == null ? "" : String(value);
    return result;
  }, {});
}

function uniqueStrings(values) {
  if (!Array.isArray(values)) {
    return [];
  }
  return [...new Set(values.map(stringValue).filter(Boolean))];
}

function truncate(value, maxLength) {
  return value.length <= maxLength ? value : value.substring(0, maxLength);
}

function stringValue(value) {
  return typeof value === "string" ? value.trim() : "";
}
