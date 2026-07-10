// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/// @title DocumentRegistry
/// @notice Ancre l'empreinte SHA-256 de pièces comptables Formuloo OS
///         (module GesDoc — thèse OCR + Blockchain). Un hash déjà
///         enregistré ne peut pas être écrasé : l'immuabilité de la
///         preuve d'existence/intégrité est garantie par le contrat.
contract DocumentRegistry {
    struct Record {
        uint256 blockNumber;
        uint256 timestamp;
        address anchoredBy;
    }

    mapping(bytes32 => Record) private records;

    event DocumentRegistered(
        bytes32 indexed documentHash,
        address indexed anchoredBy,
        uint256 blockNumber,
        uint256 timestamp
    );

    /// @notice Enregistre le hash SHA-256 d'un document. Échoue si déjà enregistré.
    function registerDocument(bytes32 documentHash) external {
        require(records[documentHash].timestamp == 0, "Document deja enregistre");

        records[documentHash] = Record({
            blockNumber: block.number,
            timestamp: block.timestamp,
            anchoredBy: msg.sender
        });

        emit DocumentRegistered(documentHash, msg.sender, block.number, block.timestamp);
    }

    /// @notice Retourne les métadonnées d'ancrage d'un hash (0 si jamais enregistré).
    function getRecord(bytes32 documentHash)
        external
        view
        returns (uint256 blockNumber, uint256 timestamp, address anchoredBy)
    {
        Record memory record = records[documentHash];
        return (record.blockNumber, record.timestamp, record.anchoredBy);
    }

    /// @notice Vérifie si un hash a déjà été ancré.
    function isRegistered(bytes32 documentHash) external view returns (bool) {
        return records[documentHash].timestamp != 0;
    }
}
