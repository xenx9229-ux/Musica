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

test("Unauthenticated user cannot read or write any Musica collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("public_profiles").where("discoverable", "==", true).get());
  await assertFails(unauthDb.collection("stories").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("rooms").where("publicRoom", "==", true).get());
});

test("Private user PII isolation: owner succeeds, cross-user fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      username: "alice_vibes",
      displayName: "Alice",
      followingArtistIds: ["artist_1"],
      streakDays: 3,
      lastActiveDate: "2026-11-17",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      username: "hacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Public profile & filtered discoverable query succeeds; unfiltered query fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_vibes",
      displayName: "Alice",
      bio: "Liquid glass lover",
      avatarIndex: 2,
      borderStyle: "neon_cyan",
      frameStyle: "vinyl_ring",
      streakDays: 10,
      followersCount: 1,
      discoverable: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    bobDb.collection("public_profiles").where("discoverable", "==", true).get()
  );
  await assertFails(bobDb.collection("public_profiles").get());
});

test("Chat & editable message bubbles enforce sender ownership and participant scoping", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);
  const chatId = "chat_alice_bob";

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: ["alice_vibes", "bob_beats"],
      lastMessage: "Hey check this track!",
      mutualStreak: 4,
      lastStreakDate: "2026-11-17",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").set({
      chatId,
      senderId: ALICE_UID,
      senderName: "alice_vibes",
      participantIds: [ALICE_UID, BOB_UID],
      text: "Listen to this!",
      bubbleStyle: "frost_cyan",
      edited: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice can edit her own bubble text & style
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").update({
      text: "Listen to this remix!",
      bubbleStyle: "neon_rose",
      edited: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot edit Alice's bubble
  await assertFails(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").update({
      text: "Tampered",
      bubbleStyle: "neon_rose",
      edited: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Filtered chat list succeeds
  await assertSucceeds(
    bobDb.collection("chats").where("participantIds", "array-contains", BOB_UID).get()
  );
});

test("Discord-style room join, playback sync, and invite validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);
  const roomId = "room_midnight_lounge";

  await assertSucceeds(
    aliceDb.collection("rooms").doc(roomId).set({
      hostId: ALICE_UID,
      hostName: "alice_vibes",
      roomName: "Midnight Liquid Lounge",
      inviteCode: "XEN99",
      publicRoom: true,
      memberUids: [ALICE_UID],
      playing: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob joins room
  await assertSucceeds(
    bobDb.collection("rooms").doc(roomId).update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob posts room chat message
  await assertSucceeds(
    bobDb.collection("rooms").doc(roomId).collection("messages").doc("rmsg_1").set({
      roomId,
      senderId: BOB_UID,
      senderName: "bob_beats",
      text: "Vibes!",
      publicRoom: true,
      createdAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp poisoning
  await assertFails(
    aliceDb.collection("stories").doc("story_bad").set({
      authorId: ALICE_UID,
      authorUsername: "alice_vibes",
      mediaType: "STREAK_PAD",
      caption: "Future timestamp",
      visibility: "public",
      createdAt: new Date(Date.now() + 120000),
    })
  );
});
