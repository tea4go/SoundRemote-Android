package io.github.soundremote.network

import io.github.soundremote.util.Net
import io.github.soundremote.util.Net.putUByte
import io.github.soundremote.util.Net.putUShort
import io.github.soundremote.util.PacketRequestIdType
import java.nio.ByteBuffer

data class ConnectData(
    @Net.Compression val compression: Int,
    val requestId: PacketRequestIdType,
    val password: String = "",
) : PacketData {
    override fun write(dest: ByteBuffer) {
        require(dest.remaining() >= SIZE)
        dest.putUByte(Net.PROTOCOL_VERSION)
        dest.putUShort(requestId)
        dest.putUByte(compression.toUByte())
        // 32 字节密码字段，null 填充
        val pwdBytes = password.toByteArray(Charsets.UTF_8)
        val fixed = ByteArray(PASSWORD_SIZE)
        System.arraycopy(pwdBytes, 0, fixed, 0, minOf(pwdBytes.size, PASSWORD_SIZE - 1))
        dest.put(fixed)
    }

    companion object {
        const val PASSWORD_SIZE = 32
        /**
         * unsigned 8bit    Protocol version
         *
         * unsigned 16bit   Request id
         *
         * unsigned 8bit    Compression
         *
         * 32 bytes         Password (UTF-8, null-padded)
         */
        const val SIZE = 4 + PASSWORD_SIZE
    }
}
