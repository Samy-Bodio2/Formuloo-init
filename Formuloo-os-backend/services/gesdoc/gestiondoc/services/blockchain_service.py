"""
Ancrage blockchain — Formuloo OS Service GesDoc

Ancre le hash SHA-256 d'un document sur le smart contract
DocumentRegistry (services/gesdoc/blockchain/contracts/DocumentRegistry.sol),
déployé sur Ethereum Sepolia via Infura.

web3.py n'est importé qu'à l'intérieur des fonctions (lazy import) afin
que le reste du service (API, migrations, tests avec mocks) fonctionne
sans que le paquet soit installé — utile en environnement de
développement où l'ancrage réel n'est pas exercé.
"""

from django.conf import settings

CONTRACT_ABI = [
    {
        "inputs": [{"internalType": "bytes32", "name": "documentHash", "type": "bytes32"}],
        "name": "registerDocument",
        "outputs": [],
        "stateMutability": "nonpayable",
        "type": "function",
    },
    {
        "inputs": [{"internalType": "bytes32", "name": "documentHash", "type": "bytes32"}],
        "name": "getRecord",
        "outputs": [
            {"internalType": "uint256", "name": "blockNumber", "type": "uint256"},
            {"internalType": "uint256", "name": "timestamp", "type": "uint256"},
            {"internalType": "address", "name": "anchoredBy", "type": "address"},
        ],
        "stateMutability": "view",
        "type": "function",
    },
    {
        "inputs": [{"internalType": "bytes32", "name": "documentHash", "type": "bytes32"}],
        "name": "isRegistered",
        "outputs": [{"internalType": "bool", "name": "", "type": "bool"}],
        "stateMutability": "view",
        "type": "function",
    },
    {
        "anonymous": False,
        "inputs": [
            {"indexed": True, "internalType": "bytes32", "name": "documentHash", "type": "bytes32"},
            {"indexed": True, "internalType": "address", "name": "anchoredBy", "type": "address"},
            {"indexed": False, "internalType": "uint256", "name": "blockNumber", "type": "uint256"},
            {"indexed": False, "internalType": "uint256", "name": "timestamp", "type": "uint256"},
        ],
        "name": "DocumentRegistered",
        "type": "event",
    },
]


class BlockchainConfigError(Exception):
    """Configuration manquante (INFURA_PROJECT_ID, clé privée, adresse du contrat)."""


class BlockchainTransactionError(Exception):
    """La transaction d'ancrage a échoué côté réseau Ethereum."""


def _get_web3():
    from web3 import Web3

    if not settings.INFURA_PROJECT_ID:
        raise BlockchainConfigError("INFURA_PROJECT_ID non configuré.")

    url = f"https://{settings.BLOCKCHAIN_NETWORK}.infura.io/v3/{settings.INFURA_PROJECT_ID}"
    return Web3(Web3.HTTPProvider(url))


def _get_contract(w3):
    if not settings.BLOCKCHAIN_CONTRACT_ADDRESS:
        raise BlockchainConfigError(
            "CONTRACT_ADDRESS non configuré — déployer DocumentRegistry.sol au préalable."
        )
    return w3.eth.contract(
        address=w3.to_checksum_address(settings.BLOCKCHAIN_CONTRACT_ADDRESS),
        abi=CONTRACT_ABI,
    )


def anchor_hash(hash_sha256: str) -> dict:
    """
    Envoie une transaction `registerDocument(hash)`, attend la confirmation
    et retourne {"tx_hash": ..., "block_number": ...}.
    """
    if not settings.BLOCKCHAIN_PRIVATE_KEY:
        raise BlockchainConfigError("SEPOLIA_PRIVATE_KEY non configuré.")

    w3 = _get_web3()
    contract = _get_contract(w3)
    account = w3.eth.account.from_key(settings.BLOCKCHAIN_PRIVATE_KEY)
    document_hash_bytes = bytes.fromhex(hash_sha256)

    try:
        tx = contract.functions.registerDocument(document_hash_bytes).build_transaction({
            "from": account.address,
            "nonce": w3.eth.get_transaction_count(account.address),
            "gas": 150_000,
            "gasPrice": w3.eth.gas_price,
        })
        signed = account.sign_transaction(tx)
        # web3.py 6.x expose .rawTransaction (camelCase), pas .raw_transaction —
        # confirmé en conditions réelles lors du déploiement de DocumentRegistry.
        tx_hash = w3.eth.send_raw_transaction(signed.rawTransaction)
        receipt = w3.eth.wait_for_transaction_receipt(tx_hash, timeout=180)
    except Exception as exc:  # noqa: BLE001 — remonté tel quel par la tâche Celery (retry)
        raise BlockchainTransactionError(str(exc)) from exc

    return {"tx_hash": tx_hash.hex(), "block_number": receipt.blockNumber}


def build_explorer_url(tx_hash: str) -> str:
    base = settings.ETHERSCAN_BASE_URL.rstrip("/")
    return f"{base}/tx/{tx_hash}"
