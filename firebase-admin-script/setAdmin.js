const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json'); // Path to your service account key

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const uid = '80sWMErPrMR4LhexRei8CiAhluR2'; // Replace with the UID from Step 3

admin.auth().setCustomUserClaims(uid, { admin: true })
  .then(() => {
    console.log('Admin claim set successfully for UID:', uid);
    process.exit(0);
  })
  .catch(error => {
    console.error('Error setting admin claim:', error);
    process.exit(1);
  });