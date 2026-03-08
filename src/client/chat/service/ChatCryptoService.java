package client.chat.service;

/**
 * E2E encryption interface for DMs.
 * Design: ECDH key exchange + AES-256-GCM.
 * Implementation deferred to backend phase.
 */
public interface ChatCryptoService {
    String generateKeyPair();
    void deriveSharedSecret(String peerId, String peerPublicKeyBase64);
    String encrypt(String peerId, String plaintext);
    String decrypt(String peerId, String ciphertextBase64);
    boolean hasSharedSecret(String peerId);
}
