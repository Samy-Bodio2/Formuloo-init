/**
 * Truffle config — DocumentRegistry (Formuloo OS / GesDoc)
 *
 * Réseau cible : Ethereum Sepolia via Infura (choix validé — pas de
 * Ganache en dev pour ce module). `truffle develop`/`truffle test`
 * utilisent toujours le réseau local "development" par défaut, qui ne
 * nécessite aucun secret.
 */

require("dotenv").config();
const HDWalletProvider = require("@truffle/hdwallet-provider");

const { INFURA_PROJECT_ID, SEPOLIA_PRIVATE_KEY } = process.env;

module.exports = {
  networks: {
    development: {
      host: "127.0.0.1",
      port: 8545,
      network_id: "*",
    },
    sepolia: {
      provider: () => {
        if (!INFURA_PROJECT_ID || !SEPOLIA_PRIVATE_KEY) {
          throw new Error(
            "INFURA_PROJECT_ID et SEPOLIA_PRIVATE_KEY doivent être définis dans .env " +
            "avant de déployer sur Sepolia (voir services/gesdoc/.env.example)."
          );
        }
        // @truffle/hdwallet-provider n'accepte une clé privée unique que sous
        // forme de chaîne hex de 64 caractères SANS préfixe "0x" (sinon il la
        // rejette avec "must be a mnemonic phrase..."). Le .env garde le
        // préfixe 0x, conventionnel pour web3.py utilisé côté Django.
        return new HDWalletProvider(
          SEPOLIA_PRIVATE_KEY.replace(/^0x/, ""),
          `https://sepolia.infura.io/v3/${INFURA_PROJECT_ID}`
        );
      },
      network_id: 11155111,
      gas: 3_000_000,
      confirmations: 2,
      timeoutBlocks: 200,
      skipDryRun: true,
    },
  },

  compilers: {
    solc: {
      version: "0.8.20",
    },
  },
};
