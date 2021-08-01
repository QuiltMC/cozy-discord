package org.quiltmc.community.database.serializers

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class SnowflakeSerializer : KSerializer<Snowflake> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Kord.Snowflake", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Snowflake = Snowflake(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: Snowflake) {
        encoder.encodeString(value.asString)
    }
}
