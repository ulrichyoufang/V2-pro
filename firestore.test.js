const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_ulrich";
const ADMIN_EMAIL = "ulrichyoufang388@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("leaderboard").get());
  await assertFails(unauthDb.collection("institutions").get());
  await assertFails(unauthDb.collection("official_schedules").get());
  await assertFails(unauthDb.collection("announcements").get());
  await assertFails(unauthDb.collection("support_tickets").get());
  await assertFails(unauthDb.collection("app_config").doc("settings").get());
  await assertFails(unauthDb.collection("admin_secrets").doc("ai_config").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Owner can create and update user profile and personal tasks, while preventing suspended escalation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Kamga",
      email: "alice@example.com",
      phoneNumber: "+237 699001122",
      institution: "Institut Universitaire de la Pointe",
      specialty: "Mécatronique",
      level: "Cycle Ingénieur",
      xpPoints: 120,
      streakDays: 3,
      totalStudyMinutes: 75,
      suspended: false,
      updatedAtEpochMs: Date.now(),
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      suspended: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task-1").set({
      userId: ALICE_UID,
      title: "TP Asservissement PID",
      subject: "Automatique",
      category: "TP",
      dueDateEpochMs: Date.now() + 86400000,
      priority: "HAUTE",
      completed: false,
      deleted: false,
      updatedAtEpochMs: Date.now(),
      syncVersion: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task-1").update({
      completed: true,
      syncVersion: 2,
      updatedAtEpochMs: Date.now(),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation blocks Bob from reading Alice private profile or tasks", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const now = new Date(Date.now() - 5000);
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Kamga",
      email: "alice@example.com",
      specialty: "Électronique",
      level: "Licence Professionnelle",
      xpPoints: 50,
      streakDays: 1,
      totalStudyMinutes: 25,
      suspended: false,
      updatedAtEpochMs: Date.now(),
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(bobDb.collection("users").get());
});

test("Super-Admin controls institutions, official_schedules, announcements, and app_config; spoofed & students are denied writes", async () => {
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_1", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const studentDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  const now = new Date(Date.now() - 1000);

  const institutionPayload = {
    name: "Institut Universitaire de la Pointe",
    city: "Douala",
    active: true,
    updatedAtEpochMs: Date.now(),
    createdAt: now,
    updatedAt: now,
  };
  await assertFails(spoofedAdminDb.collection("institutions").doc("iup_1").set(institutionPayload));
  await assertFails(studentDb.collection("institutions").doc("iup_1").set(institutionPayload));
  await assertSucceeds(realAdminDb.collection("institutions").doc("iup_1").set(institutionPayload));
  await assertSucceeds(studentDb.collection("institutions").get());

  const schedulePayload = {
    title: "Examen Automatique",
    subject: "Automatique Industrielle",
    category: "EXAMEN",
    dueDateEpochMs: Date.now() + 172800000,
    priority: "HAUTE",
    targetInstitution: "Institut Universitaire de la Pointe",
    targetLevel: "Cycle Ingénieur",
    targetSpecialty: "Mécatronique",
    roomLocation: "Amphi B2",
    instructor: "Pr. Mbarga",
    authorEmail: ADMIN_EMAIL,
    updatedAtEpochMs: Date.now(),
    createdAt: now,
    updatedAt: now,
  };
  await assertFails(studentDb.collection("official_schedules").doc("sched_1").set(schedulePayload));
  await assertSucceeds(realAdminDb.collection("official_schedules").doc("sched_1").set(schedulePayload));
  await assertSucceeds(studentDb.collection("official_schedules").get());

  const announcement = {
    title: "Session d'examens BTS",
    body: "Les emplois du temps sont disponibles.",
    priority: "URGENT",
    targetInstitution: "ALL",
    targetLevel: "ALL",
    targetSpecialty: "ALL",
    authorEmail: ADMIN_EMAIL,
    pinned: true,
    archived: false,
    createdAt: now,
    updatedAt: now,
  };
  await assertFails(spoofedAdminDb.collection("announcements").doc("ann-1").set(announcement));
  await assertFails(studentDb.collection("announcements").doc("ann-1").set(announcement));
  await assertSucceeds(realAdminDb.collection("announcements").doc("ann-1").set(announcement));
  await assertSucceeds(studentDb.collection("announcements").get());

  const configPayload = {
    maintenanceMode: false,
    maintenanceMessage: "Système opérationnel",
    registrationEnabled: true,
    aiEnabled: true,
    aiProvider: "FIREBASE_AI_LOGIC",
    aiModelName: "gemini-3.8-flash",
    aiSystemPrompt: "Tu es l'assistant académique officiel de YFUA.",
    aiTemperature: 0.7,
    aiDailyLimit: 30,
    customApiBaseUrl: "",
    customApiModel: "",
    hasCustomApiKey: false,
    updatedAtEpochMs: Date.now(),
    createdAt: now,
    updatedAt: now,
  };
  await assertFails(studentDb.collection("app_config").doc("settings").set(configPayload));
  await assertSucceeds(realAdminDb.collection("app_config").doc("settings").set(configPayload));
  await assertSucceeds(studentDb.collection("app_config").doc("settings").get());

  const adminSecretsPayload = {
    customAiApiKey: "sk-test-secret-123",
    platformWebhookUrl: "https://api.example.edu/webhook",
    platformApiSecret: "whsec-abc",
    extraApiHeadersJson: "{\"X-Campus\":\"IUP\"}",
    updatedAtEpochMs: Date.now(),
    createdAt: now,
    updatedAt: now,
  };
  await assertFails(spoofedAdminDb.collection("admin_secrets").doc("ai_config").set(adminSecretsPayload));
  await assertFails(studentDb.collection("admin_secrets").doc("ai_config").set(adminSecretsPayload));
  await assertSucceeds(realAdminDb.collection("admin_secrets").doc("ai_config").set(adminSecretsPayload));
  await assertFails(studentDb.collection("admin_secrets").doc("ai_config").get());
  await assertFails(spoofedAdminDb.collection("admin_secrets").doc("ai_config").get());
  await assertSucceeds(realAdminDb.collection("admin_secrets").doc("ai_config").get());
});

test("Support tickets enforce student ownership, prevent reply spoofing, and allow admin reply", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const now = new Date(Date.now() - 2000);

  const ticket = {
    userId: ALICE_UID,
    studentName: "Alice Kamga",
    studentEmail: "alice@example.com",
    institution: "Institut Universitaire de la Pointe",
    subject: "Problème d'emploi du temps",
    message: "Le cours de Thermodynamique n'apparaît pas.",
    status: "OUVERT",
    adminReply: "",
    createdAtEpochMs: Date.now(),
    updatedAtEpochMs: Date.now(),
    createdAt: now,
    updatedAt: now,
  };

  await assertSucceeds(aliceDb.collection("support_tickets").doc("ticket-1").set(ticket));
  await assertFails(aliceDb.collection("support_tickets").doc("ticket-1").update({
    adminReply: "Fake admin reply",
    status: "REPONDU",
    updatedAtEpochMs: Date.now(),
    updatedAt: new Date(Date.now() - 500),
  }));
  await assertFails(bobDb.collection("support_tickets").doc("ticket-1").get());
  await assertFails(bobDb.collection("support_tickets").get());
  await assertSucceeds(aliceDb.collection("support_tickets").where("userId", "==", ALICE_UID).get());

  await assertSucceeds(realAdminDb.collection("support_tickets").get());
  await assertSucceeds(realAdminDb.collection("support_tickets").doc("ticket-1").update({
    adminReply: "Corrigé à l'instant, actualisez votre planning.",
    status: "REPONDU",
    updatedAtEpochMs: Date.now(),
    updatedAt: new Date(Date.now() - 500),
  }));
});

test("Peer nudges enforce sender identity and filtered queries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("peer_nudges").doc("nudge-1").set({
      senderUid: ALICE_UID,
      senderName: "Alice",
      recipientUid: BOB_UID,
      specialty: "Informatique Industrielle",
      type: "ENCOURAGEMENT",
      message: "Courage pour les révisions !",
      read: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(bobDb.collection("peer_nudges").get());
  await assertSucceeds(
    bobDb.collection("peer_nudges").where("recipientUid", "==", BOB_UID).get()
  );
});
