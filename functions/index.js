/**
 * Import function triggers from their respective submodules:
 */
const { onRequest } = require("firebase-functions/v2/https");
const { onValueDeleted } = require("firebase-functions/v2/database");
const { initializeApp } = require("firebase-admin/app");
const { getDatabase } = require("firebase-admin/database");
const { getAuth } = require("firebase-admin/auth");
const logger = require("firebase-functions/logger");

// Initialize Firebase Admin SDK
initializeApp();

// Realtime Database trigger for cashier deletion
exports.deleteCashierAuth = onValueDeleted(
  {
    ref: "/cashiers/{cashierId}",
    region: "your-region",
  },
  async (event) => {
    const uid = event.params.cashierId;
    const auth = getAuth();

    try {
      // Delete user from Authentication
      await auth.deleteUser(uid);
      logger.log(`Successfully deleted user: ${uid}`);

      // Optional: Clean up other related data
      const db = getDatabase();
      await db.ref(`/otherUserData/${uid}`).remove();

      return { success: true };
    } catch (error) {
      logger.error("Error deleting user:", error);
      throw new Error("User deletion failed");
    }
  },
);

// Example HTTP function (keep if needed)
exports.helloWorld = onRequest({ region: "your-region" }, (req, res) => {
  logger.info("Hello logs!", { structuredData: true });
  res.send("Hello from Firebase v2!");
});