package com.newlibre.aescrypt

import java.lang.Exception
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class Crypton(val password: String, val targetBytes: ByteArray) {

    // ############
    // ############ Usage notes
    // to use this class :
    // 1. Create an instance
    // 2. call EncryptData with ByteArray of the 1. _raw_ bytes you want to encrypt.
    //     and 2. cleartext password you want to use to encrypt data
    // 2a. The password is used to create a Sha256 hash -- the hash is 32 bytes and is
    //     used as the encryption key for the AES algo
    //     The first 16 bytes of that hash are used for the iv (init vector) of AES algo
    // 3.  The raw data bytes will be encrypted and those encrypted bytes are
    //     Base64 encoded (for ease of passing around & saving)

    // ############
    // Decrypting is just as simple
    // 1. Create an instance (set the original password & target bytes (plainText or encrypted data)
    // 2. call processData with true (or blank) for encrypt & false for decrypt
    // 2a. Again the password will be hashed (Sha256) and the 32 bytes are used as AES key
    //     The first 16 bytes are used as the AES IV
    // 3.  Most likely (if you're using this program to encrypt) you had your encrypted bytes
    //    saved as Base64 encoded data (a string in a file somewhere).
    //    If your data is encoded then decode it properly first and only send the raw bytes
    //    into the DecryptData() function with the correct password & everything will work fine.

    fun processData(isEncrypt: Boolean = true): String{

        val keygen = KeyGenerator.getInstance("AES")
        keygen.init(256)
        val key : SecretKey = keygen.generateKey()

        // when you convert any string to a Sha256 it will always be 32 bytes (256 bits)
        // which is exactly the size we need our AES key to be.
        val rawSha256OfPassword = ConvertStringToSha256(password);

        val spec = SecretKeySpec(rawSha256OfPassword, "AES")
        val iv : ByteArray = rawSha256OfPassword.slice(0..15).toByteArray()

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

        //Create IvParameterSpec
        val ivSpec = IvParameterSpec(iv)

        if (isEncrypt){
             //Perform Encryption
            //Initialize Cipher for ENCRYPT_MODE
            cipher.init(Cipher.ENCRYPT_MODE, spec, ivSpec)
            return cipher.doFinal(targetBytes).toBase64()
        }
        else {
            try {
                  //Initialize Cipher for DECRYPT_MODE
                cipher.init(Cipher.DECRYPT_MODE, spec, ivSpec)

                //Perform Decryption
                val decryptedText = cipher.doFinal(targetBytes)

                return String(decryptedText)
            } catch (ex: Exception) {
                return "Decryption failed : ${ex.message}"
            }
        }
        return "fail"
    }

    private fun ByteArray.toBase64(): String =
        String(Base64.getEncoder().encode(this))

    private fun ConvertStringToSha256(plainText : String) : ByteArray{
        val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
        val hash: ByteArray = digest.digest(plainText.toByteArray(StandardCharsets.UTF_8))
        return hash;
    }
}