package fr.xania.legendsTwilight.utils

import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags

private val mm = MiniMessage.builder()
    .tags(
        TagResolver.builder()
            .resolvers(
                StandardTags.decorations(),
                StandardTags.color(),
                StandardTags.hoverEvent(),
                StandardTags.clickEvent(),
                StandardTags.keybind(),
                StandardTags.translatable(),
                StandardTags.translatableFallback(),
                StandardTags.insertion(),
                StandardTags.font(),
                StandardTags.gradient(),
                StandardTags.rainbow(),
                StandardTags.transition(),
                StandardTags.reset(),
                StandardTags.newline(),
                StandardTags.selector(),
                StandardTags.score(),
                StandardTags.nbt(),
            )
            .build()
    )
    .build()


fun String.asMini(): String = MiniMessage.miniMessage().serialize(mm.deserialize(this))