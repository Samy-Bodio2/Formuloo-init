const DocumentRegistry = artifacts.require("DocumentRegistry");

const HASH_A = web3.utils.keccak256("facture-INV-2025-0217");
const HASH_B = web3.utils.keccak256("facture-INV-2025-0218");

contract("DocumentRegistry", (accounts) => {
  let registry;

  beforeEach(async () => {
    registry = await DocumentRegistry.new();
  });

  it("enregistre un nouveau hash et émet DocumentRegistered", async () => {
    const tx = await registry.registerDocument(HASH_A, { from: accounts[0] });

    const isRegistered = await registry.isRegistered(HASH_A);
    assert.equal(isRegistered, true);

    const event = tx.logs.find((log) => log.event === "DocumentRegistered");
    assert.exists(event, "l'événement DocumentRegistered doit être émis");
    assert.equal(event.args.documentHash, HASH_A);
    assert.equal(event.args.anchoredBy, accounts[0]);
  });

  it("refuse d'enregistrer deux fois le même hash", async () => {
    await registry.registerDocument(HASH_A, { from: accounts[0] });
    try {
      await registry.registerDocument(HASH_A, { from: accounts[0] });
      assert.fail("la transaction aurait dû échouer");
    } catch (error) {
      assert.include(error.message, "Document deja enregistre");
    }
  });

  it("retourne isRegistered=false pour un hash jamais ancré", async () => {
    const isRegistered = await registry.isRegistered(HASH_B);
    assert.equal(isRegistered, false);
  });

  it("retourne les métadonnées d'ancrage via getRecord", async () => {
    await registry.registerDocument(HASH_A, { from: accounts[1] });
    const record = await registry.getRecord(HASH_A);
    assert.notEqual(record.blockNumber.toString(), "0");
    assert.notEqual(record.timestamp.toString(), "0");
    assert.equal(record.anchoredBy, accounts[1]);
  });
});
