const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const ADMIN_UID = "admin_uid_789";

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

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("specialistRequests").where("userId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("wellnessResources").get());
});

test("Owner can create and partially update UserProfile; cross-user & ghost fields rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      fullName: "Alice Green",
      ageRange: "25-34",
      preferredLanguage: "English",
      dietaryPreference: "Vegetarian",
      wellnessGoal: "Improve sleep and hydration",
      activityLevel: "Moderately Active",
      preferredRoutine: "Morning walk at 7 AM",
      wellnessPreferences: "Plant-based meals",
      waterGoalMl: 2500,
      sleepGoalHours: 8.0,
      exerciseGoalMinutes: 30,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update preserving createdAt and userId
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      waterGoalMl: 3000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Shadow update with ghost field fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot read or update Alice's profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ fullName: "Hacked" }));
});

test("Owner can create, update, and list WellnessRecords and ChatSessions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("wellnessRecords").doc("rec_2026_11_23").set({
      userId: ALICE_UID,
      dateKey: "2026-11-23",
      waterMl: 1800,
      sleepHours: 7.5,
      activityType: "Brisk Walk",
      exerciseMinutes: 35,
      habitsCompleted: ["Morning Hydration", "No Caffeine After 2PM"],
      notes: "Felt energized",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("wellnessRecords").get()
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("wellnessRecords").get()
  );

  // Chat session & message
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chatSessions").doc("sess_1").set({
      userId: ALICE_UID,
      title: "Balanced Vegetarian Meal Plan",
      category: "Nutrition and Diet",
      lastMessagePreview: "Here is a balanced plan...",
      messageCount: 2,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chatSessions").doc("sess_1").collection("messages").doc("msg_1").set({
      userId: ALICE_UID,
      sessionId: "sess_1",
      role: "user",
      content: "Suggest a high-protein vegetarian breakfast.",
      category: "Nutrition and Diet",
      createdAt: now,
    })
  );
});

test("HealthReport enforces 8MB limit and owner-only isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("healthReports").doc("rep_1").set({
      userId: ALICE_UID,
      fileName: "annual_lab_panel.pdf",
      fileSizeBytes: 524288,
      pageCount: 2,
      extractedText: "Hemoglobin: 13.8 g/dL, Vitamin D: 28 ng/mL",
      aiSummary: "Educational overview of your lab panel terms.",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Over 8 MB must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("healthReports").doc("rep_too_big").set({
      userId: ALICE_UID,
      fileName: "huge.pdf",
      fileSizeBytes: 9000000,
      pageCount: 10,
      extractedText: "text",
      aiSummary: "summary",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("healthReports").doc("rep_1").get());
});

test("SpecialistRequests enforce owner query, pending creation, and admin-only status updates", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("admins").doc(ADMIN_UID).set({ active: true });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "admin@example.com", email_verified: true }).firestore();
  const spoofAdminDb = testEnv.authenticatedContext(BOB_UID, { email: "manishaaaaa.govind@gmail.com", email_verified: false }).firestore();
  const now = new Date(Date.now() - 2000);

  // Alice can create pending request
  await assertSucceeds(
    aliceDb.collection("specialistRequests").doc("req_1").set({
      userId: ALICE_UID,
      userEmail: "alice@example.com",
      userName: "Alice Green",
      category: "Sleep Management",
      concern: "Looking for guidance on improving sleep schedule consistency.",
      status: "pending",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice cannot bypass status to resolved
  await assertFails(
    aliceDb.collection("specialistRequests").doc("req_2").set({
      userId: ALICE_UID,
      userEmail: "alice@example.com",
      userName: "Alice Green",
      category: "Sleep Management",
      concern: "Self-resolving",
      status: "resolved",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered query succeeds, unfiltered query fails for Alice
  await assertSucceeds(aliceDb.collection("specialistRequests").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("specialistRequests").get());

  // Unverified email spoof fails admin list/update
  await assertFails(spoofAdminDb.collection("specialistRequests").get());

  // Real Admin can list all and update status + adminNotes
  await assertSucceeds(adminDb.collection("specialistRequests").get());
  await assertSucceeds(
    adminDb.collection("specialistRequests").doc("req_1").update({
      status: "in_review",
      adminNotes: "Assigned to sleep wellness educator.",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
