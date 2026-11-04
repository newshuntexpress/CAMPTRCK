const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_user_1";
const ADMIN_EMAIL = "newshuntexpress@gmail.com";
const USER_UID = "viewer_user_2";
const USER_EMAIL = "viewer@awazel.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- SECURITY BOUNDS TESTS ---

test("Unauthenticated user: cannot read rooms", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("rooms").get());
});

test("Unauthenticated user: cannot read employees", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("employees").get());
});

test("Unauthenticated user: cannot read users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
});

test("Standard View-Only user: can read rooms and employees", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("rooms").doc("A-1").set({
      roomNumber: "A-1",
      block: "Block A",
      capacity: 2,
    });
    await context.firestore().collection("employees").doc("emp_1").set({
      name: "Ahmed",
      iqamaNumber: "1234567890",
      cardNumber: "CRD-01",
      status: "ACTIVE",
    });
  });

  const viewerDb = testEnv.authenticatedContext(USER_UID, { email: USER_EMAIL }).firestore();
  await assertSucceeds(viewerDb.collection("rooms").doc("A-1").get());
  await assertSucceeds(viewerDb.collection("employees").doc("emp_1").get());
});

test("Standard View-Only user: CANNOT create or delete rooms", async () => {
  const viewerDb = testEnv.authenticatedContext(USER_UID, { email: USER_EMAIL }).firestore();
  await assertFails(viewerDb.collection("rooms").doc("B-1").set({
    roomNumber: "B-1",
    block: "Block B",
    capacity: 2,
  }));
});

test("Standard View-Only user: CANNOT list all users", async () => {
  const viewerDb = testEnv.authenticatedContext(USER_UID, { email: USER_EMAIL }).firestore();
  await assertFails(viewerDb.collection("users").get());
});

test("Admin user: can create, update, and delete rooms", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  
  // Create room
  await assertSucceeds(adminDb.collection("rooms").doc("D-1").set({
    roomNumber: "D-1",
    block: "Block D",
    capacity: 2,
    note: "Newly added block D room",
  }));

  // Update room
  await assertSucceeds(adminDb.collection("rooms").doc("D-1").update({
    note: "Updated note",
  }));

  // Delete room
  await assertSucceeds(adminDb.collection("rooms").doc("D-1").delete());
});

test("Admin user: can manage users in user panel", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();

  // Create new user profile with VIEW_ONLY or ADMIN
  await assertSucceeds(adminDb.collection("users").doc("new_staff_1").set({
    userId: "new_staff_1",
    email: "staff@awazel.com",
    displayName: "Staff Member",
    role: "VIEW_ONLY",
  }));

  // List all users
  await assertSucceeds(adminDb.collection("users").get());

  // Edit user role to ADMIN
  await assertSucceeds(adminDb.collection("users").doc("new_staff_1").update({
    role: "ADMIN",
  }));

  // Delete user
  await assertSucceeds(adminDb.collection("users").doc("new_staff_1").delete());
});
