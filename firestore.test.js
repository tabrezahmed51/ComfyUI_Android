const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: "demo-no-project",
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085
    }
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

test("unauthenticated users cannot read or write user profiles", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthedDb.collection("users").doc("alice").get());
  await assertFails(unauthedDb.collection("users").doc("alice").set({
    userId: "alice",
    email: "alice@test.com",
    displayName: "Alice"
  }));
});

test("authenticated user can create and read their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  await assertSucceeds(aliceDb.collection("users").doc("alice").set({
    userId: "alice",
    email: "alice@test.com",
    displayName: "Alice"
  }));
  await assertSucceeds(aliceDb.collection("users").doc("alice").get());
});

test("authenticated user can create and read their own workflow", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  await assertSucceeds(aliceDb.collection("users").doc("alice").collection("workflows").doc("wf1").set({
    userId: "alice",
    name: "Standard SD",
    description: "Default text to image",
    nodesJson: "[]",
    linksJson: "[]"
  }));
  await assertSucceeds(aliceDb.collection("users").doc("alice").collection("workflows").doc("wf1").get());
});

test("authenticated user cannot access another user's profile or workflows", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  const bobDb = testEnv.authenticatedContext("bob").firestore();

  await aliceDb.collection("users").doc("alice").set({
    userId: "alice",
    email: "alice@test.com",
    displayName: "Alice"
  });

  await assertFails(bobDb.collection("users").doc("alice").get());
  await assertFails(bobDb.collection("users").doc("alice").collection("workflows").doc("wf1").get());
  await assertFails(bobDb.collection("users").doc("alice").collection("workflows").doc("wf1").set({
    userId: "alice",
    name: "Compromised Workflow",
    description: "Invalid",
    nodesJson: "[]",
    linksJson: "[]"
  }));
});

test("authenticated user can save gallery item with parameters", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  await assertSucceeds(aliceDb.collection("users").doc("alice").collection("gallery").doc("job1").set({
    userId: "alice",
    promptText: "a photorealistic cyberpunk street",
    negativePromptText: "blurry",
    seed: 4820194827,
    steps: 20,
    cfg: 7.0,
    samplerName: "euler",
    modelName: "v1-5-pruned-emaonly.safetensors",
    width: 512,
    height: 512,
    durationSeconds: 3.4,
    imageUrl: "/images/job1.jpg"
  }));
  await assertSucceeds(aliceDb.collection("users").doc("alice").collection("gallery").doc("job1").get());
});
