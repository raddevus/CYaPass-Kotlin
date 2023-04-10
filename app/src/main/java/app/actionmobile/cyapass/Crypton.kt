package com.newlibre.aescrypt

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
import javax.crypto.Mac


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

    fun processData(ivIn: String = "", isEncrypt: Boolean = true): String{

        val keygen = KeyGenerator.getInstance("AES")
        keygen.init(256)
        val key : SecretKey = keygen.generateKey()

        // when you convert any string to a Sha256 it will always be 32 bytes (256 bits)
        // which is exactly the size we need our AES key to be.
        // In this case we pass the user's original cleartext password and convert it to a SHA256
        val rawSha256OfPassword = ConvertStringToSha256(password)
        Log.d("Crypton", rawSha256OfPassword.size.toString())
        val spec = SecretKeySpec(rawSha256OfPassword, "AES")
        // previously we used first 16 bytes of the rawSha256 Password to generate IV
        // now we need to use a generated IV.
        // val iv : ByteArray = rawSha256OfPassword.slice(0..15).toByteArray()
        val iv : ByteArray  = if (isEncrypt){
            generateRandomIv()
        } else{
            hexStringToByteArray(ivIn)
        }
        Log.d("Crypton", iv.size.toString())
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ByteArray.toBase64(): String =
        String(Base64.getEncoder().encode(this))

    private fun ConvertStringToSha256(plainText : String) : ByteArray{
        val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
        val hash: ByteArray = digest.digest(plainText.toByteArray(StandardCharsets.UTF_8))
        return hash;
    }
    fun hexStringToByteArray(hexString: String): ByteArray {

        val len: Int = hexString.length
        val data = ByteArray(len / 2)

        var i = 0
        while (i < len) {
            var stringByte : String = hexString[i].toString() + hexString[i+1].toString()
            data[i / 2] = Integer.parseInt(stringByte,16).toByte()
            i += 2
        }

        Log.d("Crypton", BytesToHex(data))
        return data
    }

    fun BytesToHex(sha256HashKey : ByteArray) : String {
        var hex: String = ""
        for (i in sha256HashKey) {
            // Note: The capital X in the format string causes
            // the hex value to contain uppercase hex values (A-F)
            hex += String.format("%02X", i)
        }
        return hex;
    }

    fun generateRandomIv() : ByteArray{
        var sr = SecureRandom()
        var iv = ByteArray(16)
        sr.nextBytes(iv)
        Log.d("Crypton", BytesToHex(iv))
        return iv
    }

    companion object{
        fun generateHmac(secret: String, message: String) : String{

            // Discovered that in my JavaScript code I used the SHA256 pwd hash
            // directly as the 64 byte key for the Hmac secret.
            // That's why here I'm converting the 64 byte SHA256 pwd directly to bytes,
            // instead of converting each two chars in the hash to one byte.
            var secretBytes = secret.toByteArray(Charsets.UTF_8);
            Log.d("Crypton", "in generateHmac")
            Log.d("Crypton", secretBytes.size.toString())

            val keySpec = SecretKeySpec(
                secretBytes,
                "HmacSHA256"
            )

            val mac: Mac = Mac.getInstance("HmacSHA256")
            mac.init(keySpec)
            val rawHmac: ByteArray = mac.doFinal(message.toByteArray())
            Log.d("Crypton", rawHmac.size.toString())

            return BytesToHex(rawHmac)
        }

        fun BytesToHex(sha256HashKey : ByteArray) : String {
            var hex: String = ""
            for (i in sha256HashKey) {
                // Note: The capital X in the format string causes
                // the hex value to contain uppercase hex values (A-F)
                hex += String.format("%02x", i)
            }
            return hex;
        }
    }

}